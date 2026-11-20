package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.*;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.Functionals;
import io.eiaun.shared.util.Generic;
import io.eiaun.shared.util.JSON;
import io.eiaun.shared.util.TwoD;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.fory.reflect.TypeRef;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.eiaun.shared.organisms.Organism.KIND_ORGANISMS;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;

@Slf4j
public class FileSnapshots extends FileSnapshotManipulator implements Snapshots {

    public static Snapshots of(Path recording) {
        return new FileSnapshots(recording);
    }

    // This is configurable from application.yaml, but we assume
    // it is never changed, since this default is so effective.
    // TODO: Add compression to representations.Config.
    public static final String COMPRESSION = ZSTANDARD;

    private static final TypeRef<List<Change<Long, Generic>>> ORGANISM_CHANGES_TYPE = new TypeRef<>() {};
    private static final TypeRef<List<Change<String, String>>> SUBSTANCE_CHANGES_TYPE = new TypeRef<>() {};

    private final Path recording;
    @Getter private final Physics physics;
    @Getter private final Config config;
    private final SubstanceFactory substanceFactory;
    private final FullDumpReader fullDumpReader;
    private final ChangeApplier changeApplier;

    private long nextSnapshotId = 0;  // always tracks one ahead of snapshot
    private Path snapshot = null;     // always tracks one behind nextSnapshotId
    @Getter private TwoD<Organism> organisms = null;
    private Map<Long, Organism> organismIndex = null;
    @Getter private TwoD<Substance> substances = null;

    private FileSnapshots(Path recording) {
        this(recording, substanceFactory -> new FullDumpReader(COMPRESSION, substanceFactory));
    }

    FileSnapshots(
            Path recording,
            Function<SubstanceFactory,FullDumpReader> fullDumpReaderBuilder
    ) {
        this.recording = recording;
        this.physics = loadRecordingMetadata(PHYSICS, Physics.class);
        this.config = loadRecordingMetadata(CONFIG, Config.class);
        this.substanceFactory = new SubstanceFactory(this.physics.getSubstanceSpecs().values());
        this.fullDumpReader = fullDumpReaderBuilder.apply(this.substanceFactory);
        this.organisms = this.fullDumpReader.maybeGetOrganisms(recording).orElseThrow(() -> new RuntimeException("Missing initial organisms full dump"));
        this.organismIndex = makeIndex(organisms, Organism::getId);
        this.substances = this.fullDumpReader.maybeGetSubstances(recording).orElseThrow(() -> new RuntimeException("Missing initial substances full dump"));
        this.changeApplier = new ChangeApplier(this.config.getGrid(), log::info);
    }

    private <T> T loadRecordingMetadata(String metadata, Class<T> clazz) {
        return Functionals.get(() -> JSON.fromJson(
                Files.readString(this.recording.resolve(metadata)),
                clazz));
    }

    @Override
    public long getRecordingTimestamp() {
        return getRecordingTimestamp(this.recording);
    }

    public static long getRecordingTimestamp(Path recording) {
        return getRecordingTimestamp(recording, YMDHMSS);
    }

    @Override
    public long getId() {
        if (haveSnapshot()) {
            return this.nextSnapshotId - 1;
        } else {
            throw new IllegalStateException("No current snapshot");
        }
    }

    private boolean haveSnapshot() {
        return this.snapshot != null && this.nextSnapshotId > 0;
    }

    @Override
    public long getCount() {
        try {
            Path largest = this.recording.resolve(SNAPSHOTS);
            for (int i = 0; i < SNAPSHOT_ID_SEGMENTS; i++) {
                try (Stream<Path> children = Files.list(largest)) {
                    largest = children.max(Comparator.naturalOrder()).get();
                }
            }
            return 1 + getSnapshotId(largest);
        } catch (Exception failure) {
            // no snapshots for this recording, IO failure getting snapshots, etc.
            return 0;
        }
    }

    @Override
    public List<Change<String, Substance>> getSubstanceChanges() throws IllegalStateException {
        if (haveSnapshot()) {
            return Functionals.get(() -> JSON.fromJson(
                            Files.readString(this.snapshot.resolve(SUBSTANCE_CHANGES)),
                            SUBSTANCE_CHANGES_TYPE))
                    .stream()
                    .map(change -> change.translate(this.substanceFactory::make))
                    .toList();
        } else {
            throw new IllegalStateException("No current snapshot");
        }
    }

    @Override
    public List<Change<Long, Organism>> getOrganismChanges() throws IllegalStateException {
        if (haveSnapshot()) {
            return Functionals.get(() -> JSON.fromJson(
                            Files.readString(this.snapshot.resolve(ORGANISM_CHANGES)),
                            ORGANISM_CHANGES_TYPE))
                    .stream()
                    .map(change -> change.translate(generic -> {
                        Organism organism = generic.decode(KIND_ORGANISMS);
                        organism.getGenome().setSubstanceFactory(this.substanceFactory);
                        return organism;
                    }))
                    .toList();
        } else {
            throw new IllegalStateException("No current snapshot");
        }
    }

    private static <Id, Thing> Map<Id, Thing> makeIndex(
            TwoD<Thing> things,
            Function<Thing, Id> identifier
    ) {
        Map<Id, Thing> index = new HashMap<>();
        for (int lat : things.firstIndices()) {
            for (int lon : things.secondIndices(lat)) {
                Thing thing = things.get(lat, lon);
                if (thing != null) { // guard against race conditions
                    index.put(identifier.apply(thing), thing);
                }
            }
        }
        return index;
    }

    // TODO: There MIGHT BE  bugs below:
    // Repro: open VIZ, click "+1".  should not need to load full dumps (they are already loaded)!
    // Click "+1" again.  applies #0's changes again (and rejects them)
    @Override
    public Result snap(
            long id,
            Runnable wipeCallback,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback
    ) {
        if (haveSnapshot() && this.nextSnapshotId - 1 == id) {
            log.info("Snapping to current snapshot");
            return Result.SUCCESS;
        }
        if (!Files.exists(makeSnapshotPath(this.recording, id))) {
            log.warn("Snapshot {} does not exist", id);
            return Result.NOT_FOUND;
        }
        // Suppose we are snapping to id = 105. We need to look backwards from 105 to find the
        // newest snapshot K with a full dump. For our example id = 105, that will be K = 99.
        // We therefore need to load 99's full dump, then apply the changes for 100, 101, 102,
        // 103, 104 and 105. But we need to handle some special cases:
        //  1. If id has a full dump, then we just load it and we're done.
        //  2. While looking backwards, if we encounter the current snapshot (ie, K = `nextSnapshotId - 1`)
        //     then we can start from our current organisms & substances and apply the changes from
        //     nextSnapshotId to id. To make this concrete, if id = 105 and nextSnapshotId = 103 (so the
        //     current snapshot is 102), then we just need to apply the changes for 103, 104 and 105. Note
        //     that this implicitly "trusts" that we have correctly tracked the state through snapshot 102.
        //     Note that as a special happy-path case, if we snap from the current snapshot to next snapshot,
        //     then we just need to apply the changes of that next snapshot.
        //  3. While looking backwards, if we encounter the first snapshot (ie K = -1), as with case 2, we need to
        //     apply the changes from snapshots 0, 1, 2, ..., id - but first we must load the full dump from the
        //     recording preamble, to reset everything back to the beginning of time.
        Path loadFullDump = null; // will be set if/when we find a full dump that should be loaded
        long K = id;
        final long currentSnapshotId = this.nextSnapshotId - 1;
        log.info("Scanning backwards from target id = K = {}", K);
        while (true) {
            log.info("Checking K = {}", K);
            if (K == -1 || K == currentSnapshotId) {
                log.info("Done scanning because hit {} snapshot", K == 0 ? "first" : "current");
                if (K == -1 && haveSnapshot()) {
                    // case 3
                    log.info("Scanned back to the first snapshot, so loading preamble's full dump");
                    loadFullDump = this.recording;
                }
                // cases 2, 3
                break;
            }
            // TODO: It is wasteful to sequentially scan for a full dump. Is there an optimization (eg binary search)?
            Path snapshot = makeSnapshotPath(this.recording, K);
            if (hasFullDump(snapshot)) {
                log.info("Scanned to snapshot {} with a dull dump", K);
                loadFullDump = snapshot;
                break;
            }
            K--;
        }
        // load full dump (case 1)
        if (loadFullDump != null) {
            log.info("Loading full dumps from {}", loadFullDump);
            // TODO: Loading the full dumps and then building this new index are very slow. To support snapping
            // TODO: to an arbitrary snapshot, specifically backwards in time, this is unavoidable (!?). But for
            // TODO: where we just snap from one snapshot to the following, it is very heavy.  If we ever get to
            // TODO: the point of fully trusting ChangeApplier to perfectly track the organisms and substances,
            // TODO: then we could speed things up a lot by ignoring these full dumps!
            this.organisms = this.fullDumpReader.maybeGetOrganisms(loadFullDump).get();
            this.organismIndex = makeIndex(this.organisms, Organism::getId);
            this.substances = this.fullDumpReader.maybeGetSubstances(loadFullDump).get();
            wipeCallback.run();
            this.organisms.forEach(
                    (lat, lon, organism) -> organismCreatedCallback.accept(Location.of(lat, lon), organism));
            this.substances.forEach(
                    (lat, lon, substance) -> substanceCreatedCallback.accept(Location.of(lat, lon), substance));
        }
        // apply changes for J = K+1, K+2, ..., id
        List<String> dirt = new ArrayList<>();
        final long start = K + 1;
        log.info("Applying changes from {} to {}", start, id);
        for (long J = start; J <= id; J++) {
            this.nextSnapshotId = J + 1;
            this.snapshot = makeSnapshotPath(this.recording, J);
            if (hasFullDump(this.snapshot)) {
                // sanity checks: ensure the current organisms & substances are correct
                // note that we actually ignore these full dumps
                log.info("Sanity-checking full dump for {}", this.snapshot);
                this.fullDumpReader.maybeGetOrganisms(this.snapshot)
                        .flatMap(organisms -> this.organisms.anyDiscrepancy(organisms, Organism::getId, Organism::getState))
                        .ifPresent(dirt::add);
                this.fullDumpReader.maybeGetSubstances(this.snapshot)
                        .flatMap(substances -> this.substances.anyDiscrepancy(substances, Substance::getId))
                        .ifPresent(dirt::add);
            }
            log.info("Applying changes from {}", this.snapshot);
            this.changeApplier.changeOrganisms(
                    this.organisms,
                    Location.ORIGIN, // recorded changes are absolute not relative
                    getOrganismChanges(),
                    this.organismIndex::get,
                    (location, organism) -> {
                        this.organismIndex.put(organism.getId(), organism);
                        organismCreatedCallback.accept(location, organism);
                    },
                    (location, organism) -> {
                        this.organismIndex.remove(organism.getId());
                        organismDestroyedCallback.accept(location, organism);
                    });
            this.changeApplier.changeSubstances(
                    this.substances,
                    Location.ORIGIN, // recorded changes are absolute not relative
                    getSubstanceChanges(),
                    this.substanceFactory::make,
                    substanceCreatedCallback,
                    substanceDestroyedCallback);
        }
        // TODO: It is a little strange that we need these next two lines, because
        // TODO: we set these inside the above `for` loop. They are needed in case
        // TODO: the loop iterates zero times. Could this be simplified?
        if (this.nextSnapshotId - 1 != id || !Objects.equals(this.snapshot, makeSnapshotPath(this.recording, id))) {
            log.warn("INEXPLICABLY need to update nextSnapshotId to {} and snapshot to {}", id + 1, makeSnapshotPath(this.recording, id));
            this.nextSnapshotId = id + 1;
            this.snapshot = makeSnapshotPath(this.recording, id);
        }
        return dirt.isEmpty() ? Result.SUCCESS : Result.dirty(dirt);
    }

    private static boolean hasFullDump(Path snapshot) {
        return Files.exists(snapshot.resolve(ORGANISMS_DOT + COMPRESSION)) &&
                Files.exists(snapshot.resolve(SUBSTANCES_DOT + COMPRESSION));
    }

}
