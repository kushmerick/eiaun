package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.*;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.Functionals;
import io.eiaun.shared.util.JSON;
import io.eiaun.shared.util.TwoD;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fory.reflect.TypeRef;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    private static final TypeRef<List<Change<Long>>> ORGANISM_CHANGES_TYPE = new TypeRef<>() {};
    private static final TypeRef<List<Change<String>>> SUBSTANCE_CHANGES_TYPE = new TypeRef<>() {};

    private final Path recording;
    @Getter private final Physics physics;
    @Getter private final Config config;
    private final FullDumpReader fullDumpReader;
    private final ChangeApplier changeApplier;

    private int nextSnapshotIndex = 0;
    @Getter private TwoD<Organism> organisms = null;
    private Map<Long,Organism> organismIndex = null;
    @Getter @Setter(AccessLevel.PRIVATE) private TwoD<Substance> substances = null;

    private FileSnapshots(Path recording) {
        this.recording = recording;
        this.physics = loadRecordingMetadata(PHYSICS, Physics.class);
        this.config = loadRecordingMetadata(CONFIG, Config.class);
        this.fullDumpReader = new FullDumpReader(COMPRESSION, new SubstanceFactory(this.physics.getSubstanceSpecs().values()));
        this.changeApplier = new ChangeApplier(
                this.config.getGrid(),
                log::warn, // TODO: Replace with a thrower?
                new SubstanceFactory(this.physics.getSubstanceSpecs().values()));

    }

    private <T> T loadRecordingMetadata(String metadata, Class<T> clazz) {
        return Functionals.get(() -> JSON.fromJson(
                Files.readString(recording.resolve(metadata)),
                clazz));
    }

    @Override
    public long getRecordingTimestamp() {
        return parse(this.recording, YMDHMSS).get();
    }

    @Override
    public Pair<Long,Long> getSnapshotIndexAndTimestamp(Path snapshot) {
        return parseSnapshot(snapshot).get();
    }

    @Override
    public List<Change<String>> getSubstanceChanges(Path snapshot) {
        return Functionals.get(() -> JSON.fromJson(
                        Files.readString(snapshot.resolve(SUBSTANCE_CHANGES)),
                        SUBSTANCE_CHANGES_TYPE))
                .stream()
                .toList();
    }

    @Override
    public List<Change<Long>> getOrganismChanges(Path snapshot) {
        return Functionals.get(() -> JSON.fromJson(
                        Files.readString(snapshot.resolve(ORGANISM_CHANGES)),
                        ORGANISM_CHANGES_TYPE))
                .stream()
                .toList();
    }

    @Override
    public void stepTo(Path snapshot) {
        long index = getSnapshotIndexAndTimestamp(snapshot).getLeft();
        if (index != this.nextSnapshotIndex++) {
            throw new IllegalArgumentException(String.format("Expecting index %s but got %s: %s", index, this.nextSnapshotIndex, snapshot));
        }
        this.fullDumpReader.maybeGetOrganisms(snapshot).ifPresentOrElse(
                organisms -> {
                    if (this.organisms == null) {
                        this.organismIndex = makeIndex(organisms, Organism::getId);
                    }
                    this.organisms = organisms;
                },
                () -> {
                    if (this.organisms == null) {
                        throw new IllegalStateException("Missing index 0 organisms full dump");
                    }
                    this.changeApplier.changeOrganisms(
                            this.organisms,
                            Location.ORIGIN,
                            getOrganismChanges(snapshot),
                            organismIndex);
                });
        this.fullDumpReader.maybeGetSubstances(snapshot).ifPresentOrElse(
                this::setSubstances,
                () -> {
                    if (this.substances == null) {
                        throw new IllegalStateException("Missing index 0 substances full dump");
                    }
                    this.changeApplier.changeSubstances(
                            this.substances,
                            Location.ORIGIN,
                            getSubstanceChanges(snapshot));
                });
    }

    private static <Id, Thing> Map<Id, Thing> makeIndex(
            TwoD<Thing> things,
            Function<Thing, Id> identifier
    ) {
        Map<Id, Thing> index = new HashMap<>();
        for (int lat: things.firstIndices()) {
            for (int lon: things.secondIndices(lat)) {
                Thing thing = things.get(lat, lon);
                if (thing != null) { // guard against race conditions
                    index.put(identifier.apply(thing), thing);
                }
            }
        }
        return index;
    }


        /*
    private void updateOrganismsAndSubstances(Path snapshot) {
        if (this.organisms.isEmpty()) {
            // presumably this is our first snapshot, so we expect to see a full dump
            // of the organisms and snapshots.
            try {
                processFullDump(snapshot, this.fullDumpReader::maybeGetOrganisms, this.organisms::replace, "organisms");
                processFullDump(snapshot, this.fullDumpReader::maybeGetSubstances, this.substances::replace, "substances");
            } catch (RuntimeException e) {
                log.warn("Missing full dump(s) for {}", snapshot, e);
            }
        } else {
            this.changeApplier.changeOrganisms(getOrganismChanges(snapshot));
            this.changeApplier.changeSubstances(getSubstanceChanges(snapshot));
            // Sanity check
            fullDumpReader.maybeGetOrganisms(snapshot).ifPresent(organisms -> {
                if (!this.organisms.equals(organisms, Organism::getId)) {
                    this.organisms.replace(organisms);
                    log.warn("Organisms mismatch!");
                }
            });
            fullDumpReader.maybeGetSubstances(snapshot).ifPresent(substances -> {
                if (!this.substances.equals(substances, Substance::getId)) {
                    this.substances.replace(substances);
                    log.warn("Substances mismatch!");
                }
            });
        }
    }



    private <Thing> void processFullDump(
            Path snapshot,
            Function<Path, Optional<TwoD<Thing>>> fullDumpGetter,
            Consumer<TwoD<Thing>> processor,
            String label
    ) {
            fullDumpGetter.apply(snapshot).ifPresentOrElse(
                    processor,
                    () -> { throw new RuntimeException(String.format("Missing %s full dump", label)); });
    }

     */






}
