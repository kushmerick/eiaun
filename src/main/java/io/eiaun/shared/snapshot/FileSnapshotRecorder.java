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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;


@Slf4j
public class FileSnapshotRecorder extends FileSnapshotManipulator implements SnapshotRecorder {

    private final boolean enabled;
    private final Path recordingsPath;
    private long snapshotCounter;
    private boolean wrotePreamble = false;
    private final String compression;
    private final int dumpInterval;

    public FileSnapshotRecorder(
            boolean enabled,
            String compression,
            int dumpInterval
    ) {
        this.enabled = enabled;
        long recordingTimestamp = System.currentTimeMillis();
        this.recordingsPath = Path.of(
                ROOT,
                formatYMDH(recordingTimestamp),
                formatYMDHM(recordingTimestamp),
                formatYMDHMSS(recordingTimestamp));
        this.snapshotCounter = 0;
        this.compression = compression;
        this.dumpInterval = dumpInterval;
        if (this.enabled) {
            if (this.recordingsPath.toFile().exists()) {
                // should never happen, but we may lose data if it does, so let's be overly cautious
                throw new RuntimeException(String.format("Recording timestamp collision: %s", this.recordingsPath));
            }
            log.info("Recording to {}", this.recordingsPath);
        }
    }

    // Taking a snapshot has two phases: preparing the data, then writing the files.
    // THe first must be synchronized across all steps in flight, while the second
    // can be parallelized. The Runnable returned by this method encapsulates the
    // second phase, so that the caller can schedule it asynchronously.
    @Override
    public Pair<String, Runnable> record(
            Jakku jakku,
            Location changeOffset,
            List<Change<Organism>> organismChanges,
            List<Change<Substance>> substanceChanges
            // TODO: This has a huge gap: we do not record the state change of the organism!
            // eg if a Mover does not move, it may lose energy but will not appear in
            // organismChanges.
    ) {
        if (!enabled) {
            log.trace("Snapshots disabled");
            return null;
        }
        if (!wrotePreamble) {
            // no need to make this small file write asynchronous
            writePreamble(jakku, this.recordingsPath);
            wrotePreamble = true;
        }
        long snapshotId = this.snapshotCounter++;
        boolean dump = snapshotId % this.dumpInterval == 0;
        long snapshotTimestamp = System.currentTimeMillis();
        Path snapshotPath = this.recordingsPath.resolve(
                Path.of(SNAPSHOTS,
                        formatYMDH(snapshotTimestamp),
                        formatYMDHM(snapshotTimestamp),
                        formatYMDHMSS(snapshotTimestamp),
                        formatSnapshot(snapshotId, snapshotTimestamp)));
        List<Runnable> fileWrites = new ArrayList<>();
        if (dump) {
            fileWrites.add(() -> writeOrganisms(jakku.getOrganisms(), snapshotPath));
            fileWrites.add(() -> writeSubstances(jakku.getSubstances(), snapshotPath));
        } else {
            log.trace("Skipping full dump for {}", snapshotPath);
        }
        int grid = jakku.getGrid();
        fileWrites.add(() -> writeOrganismChanges(organismChanges, changeOffset, grid, snapshotPath));
        fileWrites.add(() -> writeSubstanceChanges(substanceChanges, changeOffset, grid, snapshotPath));
        return Pair.of(
                snapshotPath.toString(),
                () -> fileWrites.forEach(Runnable::run));
    }

    private void writePreamble(
            Jakku jakku,
            Path path
    ) {
        write(JSON.toJson(Physics.from(jakku)),
                path.resolve(PHYSICS));
        write(JSON.toJson(Config.from(jakku)),
                path.resolve(CONFIG));
    }

    private void writeOrganisms(
            TwoD<Organism> organisms,
            Path path
    ) {
        writeCompressed(
                JSON.toJson(thingsAsMap(organisms, o -> Generic.encode(o, Organism.ORGANISM_KINDS))),
                path.resolve(ORGANISMS_DOT + this.compression));
    }

    private void writeSubstances(
            TwoD<Substance> substances,
            Path path
    ) {
        writeCompressed(
                JSON.toJson(thingsAsMap(substances, Substance::getId)),
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
            List<Change<Organism>> organismChanges,
            Location changeOffset,
            int grid,
            Path path
    ) {
        write(JSON.toJson(
                        changesAsList(
                                organismChanges,
                                changeOffset,
                                grid,
                                Organism::getId)),
                path.resolve(ORGANISM_CHANGES));
    }

    private void writeSubstanceChanges(
            List<Change<Substance>> substanceChanges,
            Location changeOffset,
            int grid,
            Path path
    ) {
        write(JSON.toJson(
                        changesAsList(
                                substanceChanges,
                                changeOffset,
                                grid,
                                Substance::getId)),
                path.resolve(SUBSTANCE_CHANGES));
    }

    private <Thing, Representation> List<Change<Representation>> changesAsList(
            List<Change<Thing>> changes,
            Location changeOffset,
            int grid,
            Function<Thing,Representation> representer
    ) {
        Function<Location, Location> offsetter = l -> l.add(changeOffset, grid);
        return changes.stream()
                .map(change ->
                        new Change<>(
                                change.getVerb(),
                                Optional.ofNullable(change.getOriginal()).map(representer).orElse(null),
                                Optional.ofNullable(change.getReplacement()).map(representer).orElse(null),
                                Optional.ofNullable(change.getFrom()).map(offsetter).orElse(null),
                                Optional.ofNullable(change.getTo()).map(offsetter).orElse(null)))
                .toList();
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
        long start = System.nanoTime();
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
        } finally {
            if (path.getFileName().toString().contains("organisms")) {
                log.info("XXXX organisms write took {} millis", Duration.ofNanos(System.nanoTime() - start).toMillis());
            }
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

    static String formatSnapshot(long snapshotId, long snapshotTimestamp) {
        return String.format(SNAPSHOT_ID_FORMAT + "-%s", snapshotId, formatYMDHMSS(snapshotTimestamp));
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
