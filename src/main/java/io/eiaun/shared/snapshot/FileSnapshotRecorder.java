package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.Generic;
import io.eiaun.shared.util.JSON;
import io.eiaun.shared.util.TwoD;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.eiaun.shared.organisms.Organism.ORGANISM_KINDS;

// TODO: Organism full dumps consist mainly of their genomes serialized as long JSON strings.
// TODO: But they don't change (all changes are reflected in the state). So we could instead
// TODO: serialize organism genomes only when they are born (ie, for the initial full dump,
// TODO: and for every CREATE change).  This would make full dumps much smaller, at the cost
// TODO: of some extra complexity and new organism-genome files. On the other hand, we take
// TODO: full dumps for just 1% of the steps, so maybe it is not worth it?

@Slf4j
public class FileSnapshotRecorder extends FileSnapshotManipulator implements SnapshotRecorder {

    private final boolean enabled;
    private final Path recording;
    private long snapshotCounter;
    private final String compression;
    private final int dumpInterval;

    public FileSnapshotRecorder(
            boolean enabled,
            String compression,
            int dumpInterval
    ) {
        this.enabled = enabled;
        long recordingTimestamp = System.currentTimeMillis();
        this.recording = Path.of(
                ROOT,
                formatYMDH(recordingTimestamp),
                formatYMDHM(recordingTimestamp),
                formatYMDHMSS(recordingTimestamp));
        this.snapshotCounter = 0;
        this.compression = compression;
        this.dumpInterval = dumpInterval;
        if (this.enabled) {
            if (this.recording.toFile().exists()) {
                // should never happen, but we may lose data if it does, so let's be overly cautious
                throw new RuntimeException(String.format("Recording timestamp collision: %s", this.recording));
            }
            log.info("Recording to {}", this.recording);
        }
    }

    // Taking a snapshot has two phases: preparing the data, then writing the files.
    // THe first must be synchronized across all steps in flight, while the second
    // can be parallelized. The Runnable returned by this method encapsulates the
    // second phase, so that the caller can schedule it asynchronously.
    @Override
    public Pair<String, Runnable> record(
            Jakku jakku,
            Location offset,
            List<Change<Long,Organism>> organismChanges,
            List<Change<String,Substance>> substanceChanges
    ) {
        if (!enabled) {
            log.trace("Snapshots disabled");
            return null;
        }
        List<Runnable> writes = new ArrayList<>(3);
        long snapshotId = this.snapshotCounter++;
        // take a full dump every `dumpInterval` snapshots
        boolean dump = (snapshotId % this.dumpInterval) == (this.dumpInterval - 1);
        Path snapshotPath = makeSnapshotPath(this.recording, snapshotId);
        if (dump) {
            writes.add(writeFullDump(jakku, snapshotPath));
        } else {
            log.trace("Skipping full dump for {}", snapshotPath);
        }
        int grid = jakku.getGrid();
        writes.add(() -> writeOrganismChanges(organismChanges, offset, grid, snapshotPath));
        writes.add(() -> writeSubstanceChanges(substanceChanges, offset, grid, snapshotPath));
        return Pair.of(
                snapshotPath.toString(),
                () -> writes.forEach(Runnable::run));
    }

    @Override
    public Runnable preamble(Jakku jakku) {
        if (!enabled) {
            log.trace("Preamble disabled");
            return null;
        }
        List<Runnable> writes = new ArrayList<>(3);
        writes.add(() -> write(JSON.toJson(Physics.from(jakku)),
                this.recording.resolve(PHYSICS)));
        writes.add(() -> write(JSON.toJson(Config.from(jakku)),
                this.recording.resolve(CONFIG)));
        writes.add(writeFullDump(jakku, this.recording));
        return () -> writes.forEach(Runnable::run);
    }

    private Runnable writeFullDump(Jakku jakku, Path snapshotPath) {
        // note that we clone the organisms & substances here in the current thread,
        // allowing Jakku to schedule the returned Runnable without a risk that some
        // other change pollutes this snapshot.
        Map<String, Map<String, Generic>> organisms = thingsAsMap(jakku.getOrganisms(), o -> Generic.encode(o, ORGANISM_KINDS));
        Map<String, Map<String, String>> substances = thingsAsMap(jakku.getSubstances(), Substance::getId);
        return () -> {
            writeOrganisms(organisms, snapshotPath);
            writeSubstances(substances, snapshotPath);
        };
    }

    private void writeOrganisms(
            Map<String, Map<String, Generic>> organisms,
            Path path
    ) {
        writeCompressed(
                JSON.toJson(organisms),
                path.resolve(ORGANISMS_DOT + this.compression));
    }

    private void writeSubstances(
            Map<String, Map<String, String>> substances,
            Path path
    ) {
        writeCompressed(
                JSON.toJson(substances),
                path.resolve(SUBSTANCES_DOT + this.compression));
    }

    private static <Thing, Representation> Map<String, Map<String, Representation>> thingsAsMap(
            TwoD<Thing> things,
            Function<Thing, Representation> representer
    ) {
        Map<String, Map<String, Representation>> map = new HashMap<>(); // lat -> lon -> representation
        for (int lat: things.firstIndices()) {
            for (int lon: things.secondIndices(lat)) {
                Thing thing = things.get(lat, lon);
                if (thing != null) {
                    map.computeIfAbsent(Integer.toString(lat), _ -> new HashMap<>())
                            .put(Integer.toString(lon), representer.apply(thing));
                }
            }
        }
        return map;
    }

    private void writeOrganismChanges(
            List<Change<Long,Organism>> organismChanges,
            Location offset,
            int grid,
            Path path
    ) {
        write(JSON.toJson(
                translate(
                        offset(organismChanges, offset, grid),
                        change -> change.translate(organism -> Generic.encode(organism, ORGANISM_KINDS)))),
                path.resolve(ORGANISM_CHANGES));
    }

    private void writeSubstanceChanges(
            List<Change<String,Substance>> substanceChanges,
            Location offset,
            int grid,
            Path path
    ) {
        write(JSON.toJson(
                translate(
                        offset(substanceChanges, offset, grid),
                        change -> change.translate(Substance::getId))),
                path.resolve(SUBSTANCE_CHANGES));
    }

    // Genomes deal with locations that are relative to the organism's location.
    // Specifically, changes having relative locations. When writing changes to
    // a snapshot, these relative locations are offset by the organism's location,
    // to get an absolute location of the change.
    private <Id,Thing> List<Change<Id,Thing>> offset(
            List<Change<Id,Thing>> changes,
            Location offset,
            int grid
    ) {
        return translate(
                changes,
                change -> change.offset(offset, grid));
    }

    private <Id,Thing,Thang> List<Change<Id, Thang>> translate(
            List<Change<Id, Thing>> changes,
            Function<Change<Id,Thing>,Change<Id,Thang>> translator
    ) {
        return changes.stream().map(translator).toList();
    }

    private void write(String payload, Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, payload.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException failure) {
            throw new RuntimeException("Failure while writing payload", failure);
        }
    }

    private void writeCompressed(String payload, Path path) {
        try {
            Files.createDirectories(path.getParent());
            String format = PathUtils.getExtension(path);
            try (var r = new StringReader(payload);
                 var in = ReaderInputStream.builder().setReader(r).get();
                 var os = Files.newOutputStream(path);
                 var buf = new BufferedOutputStream(os);
                 var out = new CompressorStreamFactory().createCompressorOutputStream(format, buf)
            ) {
                IOUtils.copy(in, out);
            }
        } catch (IOException failure) {
            throw new RuntimeException("Failure while writing compressed payload", failure);
        }
    }

    static String formatYMDH(long timestamp) {
        return YMDH.format(getDateTime(timestamp));
    }

    static String formatYMDHM(long timestamp) {
        return YMDHM.format(getDateTime(timestamp));
    }

    static String formatYMDHMSS(long timestamp) {
        return YMDHMSS.format(getDateTime(timestamp));
    }

    private static LocalDateTime getDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }

    /*
    // Some garbage throwaway code for running this experiment:
    // https://docs.google.com/spreadsheets/d/1MlanJPIokGAOB5kAZ0fXQ58LH6g6LBmlZMTjXMT3Z8Y/edit?usp=sharing

    private void writeCompressed(String payload, Path path) throws IOException {
        long s = System.nanoTime();
        write(payload, Path.of(path + ".json"));
        Duration d = Duration.ofNanos(System.nanoTime() - s);
        log.info("XXXX Compressed {} using NONE in {} milliseconds --> {} KB", PathUtils.getBaseName(path), d.toMillis(),
                Path.of(path + ".json").toFile().length()/1024d);
        Files.createDirectories(path.getParent());
        for (String format: io.eiaun.config.Config.SNAPSHOT_COMPRESSION) {
            long start = System.nanoTime();
            Path p = Path.of(path.toString() + "." + format);
            try (var r = new StringReader(payload);
                 var in = ReaderInputStream.builder().setReader(r).get();
                 var os = Files.newOutputStream(p);
                 var buf = new BufferedOutputStream(os);
                 var out = new CompressorStreamFactory().createCompressorOutputStream(format, buf)
            ) {
                IOUtils.copy(in, out);
            }
            Duration duration = Duration.ofNanos(System.nanoTime() - start);
            log.info("XXXX Compressed {} using {} in {} milliseconds --> {} KB", PathUtils.getBaseName(path), format, duration.toMillis(),
                    p.toFile().length() / 1024d);
        }
    }

    // End of garbage throwaway code.
    */

}
