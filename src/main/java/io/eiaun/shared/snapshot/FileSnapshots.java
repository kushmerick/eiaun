package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.*;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.Functionals;
import io.eiaun.shared.util.Generic;
import io.eiaun.shared.util.JSON;
import io.eiaun.shared.util.TwoD;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fory.reflect.TypeRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.eiaun.shared.organisms.Organism.KIND_ORGANISMS;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;

@Slf4j
public class FileSnapshots extends FileSnapshotManipulator implements Snapshots {

    public static Snapshots of(Path recording) {
        return new FileSnapshots(recording);
    }

    public static long getRecordingTimestamp(Path recording) {
        return parse(recording, YMDHMSS).get();
    }

    // this is configurable from application.yaml, but we assume it is never
    // changed, since this default is so effective.
    private static final String COMPRESSION = ZSTANDARD;

    private static final TypeRef<List<Change<Long, Generic>>> ORGANISM_CHANGES_TYPE = new TypeRef<>() {};
    private static final TypeRef<List<Change<String, String>>> SUBSTANCE_CHANGES_TYPE = new TypeRef<>() {};

    // snapshots/2026-10-26-15/2026-10-26-15-49/2026-10-26-15-49-56-942/0000000000009-2026-10-26-15-49-56-942
    private static final String STEP_TO_NEXT_PATTERN =
            String.join(File.separator,
                    SNAPSHOTS,
                    "*",                        // 2026-10-26-15
                    "*",                        // 2026-10-26-15-49
                    "*",                        // 2026-10-26-15-49-56-942
                    SNAPSHOT_ID_FORMAT + "-*"); // 0000000000009-2026-10-26-15-49-56-942
    private static final int STEP_TO_NEXT_DEPTH = 5;

    private final Path recording;
    @Getter
    private final Physics physics;
    @Getter
    private final Config config;
    private final SubstanceFactory substanceFactory;
    private final FullDumpReader fullDumpReader;
    private final ChangeApplier changeApplier;

    private int nextSnapshotIndex = 0;
    private Path snapshot;
    @Getter private TwoD<Organism> organisms = null;
    private Map<Long, Organism> organismIndex = null;
    @Getter private TwoD<Substance> substances = null;

    private FileSnapshots(Path recording) {
        this.recording = recording;
        this.physics = loadRecordingMetadata(PHYSICS, Physics.class);
        this.config = loadRecordingMetadata(CONFIG, Config.class);
        this.substanceFactory = new SubstanceFactory(this.physics.getSubstanceSpecs().values());
        this.fullDumpReader = new FullDumpReader(COMPRESSION, this.substanceFactory);
        this.organisms = this.fullDumpReader.maybeGetOrganisms(recording).orElseThrow(() -> new RuntimeException("Missing initial organisms full dump"));
        this.organismIndex = makeIndex(organisms, Organism::getId);
        this.substances = this.fullDumpReader.maybeGetSubstances(recording).orElseThrow(() -> new RuntimeException("Missing initial substances full dump"));
        this.changeApplier = new ChangeApplier(
                this.config.getGrid(),
                log::info);
    }

    private <T> T loadRecordingMetadata(String metadata, Class<T> clazz) {
        return Functionals.get(() -> JSON.fromJson(
                Files.readString(this.recording.resolve(metadata)),
                clazz));
    }

    @Override
    public long getRecordingTimestamp() {
        return parse(this.recording, YMDHMSS).get();
    }

    @Override
    public void bumpNextSnapshotIndex(int delta) {
        // TODO: This is WRONG when `snapshotIndexDelta` is not zero. Let N = nextSnapshotIndex + snapshotIndexDelta
        // TODO: be the index of the target snapshot. We need to find the newest snapshot index K <= N that has a
        // TODO: full dump, then apply the changes from snapshots K+1, K+2, ..., N.
        this.nextSnapshotIndex += delta;
    }

    @Override
    public StepToResult stepToSnapshot(
            Path snapshot,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback
    ) {
        long index = getSnapshotIndexAndTimestamp(snapshot).getLeft();
        if (index != this.nextSnapshotIndex) {
            throw new IllegalArgumentException(String.format("Expecting index %s but got %s: %s", this.nextSnapshotIndex, index, snapshot));
        }
        this.nextSnapshotIndex++;
        this.snapshot = snapshot;
        String[] dirt = {null};
        this.fullDumpReader.maybeGetOrganisms(snapshot).ifPresentOrElse(
                // TODO: Loading the full dumps and then building this new index are very slow. If we ever
                // TODO: get to the point of fully trusting ChangeApplier to perfectly track the organisms
                // TODO: and substances, then we could speed things a lot up by ignoring the full dumps!
                organisms -> {
                    // sanity check: ensure the current organisms are correct
                    dirt[0] = this.organisms.anyDiscrepancy(organisms, Organism::getId, Organism::getState);
                    if (dirt[0] == null) {
                        this.organisms = organisms;
                        this.organismIndex = makeIndex(organisms, Organism::getId);
                    }
                },
                () -> this.changeApplier.changeOrganisms(
                        this.organisms,
                        Location.ORIGIN, // recorded changes are absolute not relative
                        getSnapshotOrganismChanges(),
                        this.organismIndex::get,
                        (location, organism) -> {
                            this.organismIndex.put(organism.getId(), organism);
                            organismCreatedCallback.accept(location, organism);
                        },
                        (location, organism) -> {
                            this.organismIndex.remove(organism.getId());
                            organismDestroyedCallback.accept(location, organism);
                        }));
        if (dirt[0] != null) {
            return StepToResult.dirty(snapshot, dirt[0]);
        }
        this.fullDumpReader.maybeGetSubstances(snapshot).ifPresentOrElse(
                // TODO: Ditto - Eventually ignore full dumps?
                substances -> {
                    // sanity check: ensure the current substances are correct
                    dirt[0] = this.substances.anyDiscrepancy(substances, Substance::getId);
                    if (dirt[0] == null) {
                       this.substances = substances;
                    }
                },
                () -> this.changeApplier.changeSubstances(
                        this.substances,
                        Location.ORIGIN, // recorded changes are absolute not relative
                        getSnapshotSubstanceChanges(),
                        this.substanceFactory::make,
                        substanceCreatedCallback,
                        substanceDestroyedCallback));
        return dirt[0] == null ? StepToResult.clean(snapshot) : StepToResult.dirty(snapshot, dirt[0]);
    }

    @Override
    public Pair<Long, Long> getSnapshotIndexAndTimestamp() {
        return getSnapshotIndexAndTimestamp(this.snapshot);
    }

    @Override
    public Pair<Long, Long> getSnapshotIndexAndTimestamp(Path snapshot) {
        return parseSnapshot(snapshot).get();
    }

    @Override
    public List<Change<String, Substance>> getSnapshotSubstanceChanges() {
        return getSnapshotSubstanceChanges(this.snapshot);
    }

    @Override
    public List<Change<String, Substance>> getSnapshotSubstanceChanges(Path snapshot) {
        return Functionals.get(() -> JSON.fromJson(
                        Files.readString(snapshot.resolve(SUBSTANCE_CHANGES)),
                        SUBSTANCE_CHANGES_TYPE))
                .stream()
                .map(change -> change.translate(this.substanceFactory::make))
                .toList();
    }

    public List<Change<Long, Organism>> getSnapshotOrganismChanges() {
        return getSnapshotOrganismChanges(this.snapshot);
    }

    public List<Change<Long, Organism>> getSnapshotOrganismChanges(Path snapshot) {
        return Functionals.get(() -> JSON.fromJson(
                        Files.readString(snapshot.resolve(ORGANISM_CHANGES)),
                        ORGANISM_CHANGES_TYPE))
                .stream()
                .map(change -> change.translate(generic -> {
                    Organism organism = generic.decode(KIND_ORGANISMS);
                    organism.getGenome().setSubstanceFactory(this.substanceFactory);
                    return organism;
                }))
                .toList();
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

    @Override
    public StepToResult stepToNextSnapshot(
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback
    ) {
        // find the snapshot with index = `this.nextSNapshotIndex`
        // this.recordings = recordings/2026-10-26-15/2026-10-26-15-49/2026-10-26-15-49-54-676
        // we are looking for something like:
        // snapshots/2026-10-26-15/2026-10-26-15-49/2026-10-26-15-49-56-942/0000000000009-2026-10-26-15-49-56-942
        PathMatcher matcher = this.recording.getFileSystem()
                .getPathMatcher("glob:" + String.format(STEP_TO_NEXT_PATTERN, this.nextSnapshotIndex));
        Path[] snapshot = { null };
        try {
            Files.walkFileTree(
                    this.recording,
                    Collections.emptySet(),
                    STEP_TO_NEXT_DEPTH + 1,
                    new SimpleFileVisitor<>() {
                        @Override public FileVisitResult preVisitDirectory(Path maybeSnapshot, BasicFileAttributes attrs) {
                            Path relative = FileSnapshots.this.recording.relativize(maybeSnapshot);
                            int depth = relative.getNameCount();
                            if (depth == STEP_TO_NEXT_DEPTH && matcher.matches(relative)) {
                                snapshot[0] = maybeSnapshot;
                                return FileVisitResult.TERMINATE;
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException failure) {
            throw new RuntimeException(String.format("Failure while stepping to %s", this.nextSnapshotIndex), failure);
        }
        if (snapshot[0] == null) {
            // no such snapshot -- ie we got to the end
            return StepToResult.clean(null);
        }
        return stepToSnapshot(
                snapshot[0],
                organismCreatedCallback,
                organismDestroyedCallback,
                substanceCreatedCallback,
                substanceDestroyedCallback);
    }

}
