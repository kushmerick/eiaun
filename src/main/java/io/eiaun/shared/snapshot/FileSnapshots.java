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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.eiaun.shared.organisms.Organism.KIND_ORGANISMS;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;

@Slf4j
public class FileSnapshots extends FileSnapshotManipulator implements Snapshots {

    public static Snapshots of(Path recording) {
        return new FileSnapshots(recording);
    }

    // this is configurable from application.yaml, but we assume it is never
    // changed, since this default is so effective.
    private static final String COMPRESSION = ZSTANDARD;

    private static final TypeRef<List<Change<Long, Generic>>> ORGANISM_CHANGES_TYPE = new TypeRef<>() {};
    private static final TypeRef<List<Change<String, String>>> SUBSTANCE_CHANGES_TYPE = new TypeRef<>() {};

    private final Path recording;
    @Getter private final Physics physics;
    @Getter private final Config config;
    private final SubstanceFactory substanceFactory;
    private final FullDumpReader fullDumpReader;
    private final ChangeApplier changeApplier;

    private long nextSnapshotId = 0;  // alwaystracks  one ahead of snapshot
    private Path snapshot = null;     // always tracks one behind nextSnapshotId
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
        if (this.nextSnapshotId == 0) {
            throw new IllegalStateException("No snapshots has been loaded");
        }
        return this.nextSnapshotId - 1;
    }

    @Override
    public long getMaxId() {
        // TODO: traverse the file system, entering the numerically largest subdirectory at each level, until
        // TODO: we reach the deepest level, then return the numerically largest child
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public List<Change<String, Substance>> getSubstanceChanges() {
        return getSnapshotSubstanceChanges(this.snapshot);
    }

    private List<Change<String, Substance>> getSnapshotSubstanceChanges(Path snapshot) {
        return Functionals.get(() -> JSON.fromJson(
                        Files.readString(snapshot.resolve(SUBSTANCE_CHANGES)),
                        SUBSTANCE_CHANGES_TYPE))
                .stream()
                .map(change -> change.translate(this.substanceFactory::make))
                .toList();
    }

    @Override
    public List<Change<Long, Organism>> getOrganismChanges() {
        return getSnapshotOrganismChanges(this.snapshot);
    }

    private List<Change<Long, Organism>> getSnapshotOrganismChanges(Path snapshot) {
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
    public Result bump(
            long delta,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback
    ) {
        return snap(this.nextSnapshotId + delta,
                organismCreatedCallback,
                organismDestroyedCallback,
                substanceCreatedCallback,
                substanceDestroyedCallback);
    }

    @Override
    public Result snap(
            long id,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback
    ) {
        // TODO: This is WRONG when id != this.snapshotId + 1.
        // TODO: We need to find the newest snapshot index K <= id that has a
        // TODO: full dump, then apply the changes from snapshots K+1, K+2, ..., id.
        Path snapshot = makeSnapshotPath(this.recording, id);
        if (!Files.exists(snapshot)) {
            return Snapshots.Result.NOT_FOUND;
        }
        this.nextSnapshotId = id;
        this.snapshot = snapshot;
        List<String> dirt = new ArrayList<>();
        this.fullDumpReader.maybeGetOrganisms(snapshot).ifPresentOrElse(
                // TODO: Loading the full dumps and then building this new index are very slow. If we ever
                // TODO: get to the point of fully trusting ChangeApplier to perfectly track the organisms
                // TODO: and substances, then we could speed things a lot up by ignoring the full dumps!
                organisms -> {
                    // sanity check: ensure the current organisms are correct
                    String d = this.organisms.anyDiscrepancy(organisms, Organism::getId, Organism::getState);
                    if (d != null) dirt.add(d);
                    this.organisms = organisms;
                    this.organismIndex = makeIndex(organisms, Organism::getId);
                },
                () -> this.changeApplier.changeOrganisms(
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
                        }));
        this.fullDumpReader.maybeGetSubstances(snapshot).ifPresentOrElse(
                // TODO: Ditto - Eventually ignore full dumps?
                substances -> {
                    // sanity check: ensure the current substances are correct
                    String d = this.substances.anyDiscrepancy(substances, Substance::getId);
                    if (d != null) dirt.add(d);
                    this.substances = substances;
                },
                () -> this.changeApplier.changeSubstances(
                        this.substances,
                        Location.ORIGIN, // recorded changes are absolute not relative
                        getSubstanceChanges(),
                        this.substanceFactory::make,
                        substanceCreatedCallback,
                        substanceDestroyedCallback));
        return dirt.isEmpty() ? Snapshots.Result.SUCCESS : Snapshots.Result.dirty(dirt);
    }

}
