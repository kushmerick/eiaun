package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Snapshots are written to files like:
 * <pre>
 *  recordings/                                                        Root path
 *   2026-10-13-09/                                                    YYYY-MM-dd-HH when this run began
 *    2026-10-13-09-15-31-672/                                         Full timestamp when this run began
 *     physics.json                                                    Substance properties and other physical details
 *     config.json                                                     Configuration (grid size, initial densities, etc)
 *     snapshots/                                                      Snapshots live here
 *      2026-10-14-10/                                                 YYYY-MM-dd-HH when this snapshot was taken
 *       0000023782371-2026-10-14-10-51-17-186/                        ID-Timestamp of this snapshot
 *        000023782371-2026-10-14-10-51-17-186.organisms.json.XX       Locations and contents of all organisms
 *        0000023782371-2026-10-14-10-51-17-186.substances.json.XX     Locations and contents of all substances
 *        0000023782371-2026-10-14-10-51-17-186.organism-changes.json  Organism changes that produced this state from the prior state
 *        0000023782371-2026-10-14-10-51-17-186.substance-changes.json Substance changes that produced this state from the prior state
 *  </pre>
 *  where XX is the compression algorithm (see {@link io.eiaun.shared.config.Config#SNAPSHOT_COMPRESSION}).
 */
@Slf4j
public class SnapshotFileRecorder implements SnapshotRecorder {

    private static final SimpleDateFormat YMDH_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH");
    private static final SimpleDateFormat FULL_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    private static final String ID_FORMAT = "%013d"; // pad ids with enough 0's so that a trillion sorts alphabetically
    private static final String ROOT = "recordings";
    private static final String DOT_JSON = ".json";
    private static final String PHYSICS = "physics" + DOT_JSON;
    private static final String CONFIG = "config" + DOT_JSON;
    private static final String SNAPSHOTS = "snapshots";
    private static final String DOT_JSON_DOT = DOT_JSON + ".";
    private static final String ORGANISMS_DOT = "organisms" + DOT_JSON_DOT;
    private static final String ORGANISM_CHANGES = "organism-changes" + DOT_JSON;
    private static final String SUBSTANCES_DOT = "substances" + DOT_JSON_DOT;
    private static final String SUBSTANCE_CHANGES = "substance-changes" + DOT_JSON;

    private final boolean enabled;
    private final Path recordingsPath;
    private long snapshotCounter;
    private boolean wrotePreamble = false;
    private final String compression;
    private final int dumpInterval;

    public SnapshotFileRecorder(
            boolean enabled,
            String compression,
            int dumpInterval
    ) {
        this.enabled = enabled;
        long recordingTimestamp = System.currentTimeMillis();
        this.recordingsPath = Path.of(
                ROOT,
                formatYMDHTimestamp(recordingTimestamp),
                formatFullTimestamp(recordingTimestamp));
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
                        formatYMDHTimestamp(snapshotTimestamp),
                        formatFullTimestamp(snapshotTimestamp),
                        String.format(ID_FORMAT + "-%s", snapshotId, formatFullTimestamp(snapshotTimestamp))));
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

    private String formatFullTimestamp(long timestamp) {
        return FULL_TIMESTAMP_FORMAT.format(new Date(timestamp));
    }

    private String formatYMDHTimestamp(long timestamp) {
        return YMDH_TIMESTAMP_FORMAT.format(new Date(timestamp));
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
        writeCompressed(JSON.toJson(thingsAsMap(organisms, Function.identity())),
                path.resolve(ORGANISMS_DOT + this.compression));
    }

    private void writeSubstances(
            TwoD<Substance> substances,
            Path path
    ) {
        writeCompressed(JSON.toJson(thingsAsMap(substances, Substance::getId)),
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
                                organism -> Long.toString(organism.getId()))),
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

    private <Thing> List<Change<String>> changesAsList(
            List<Change<Thing>> changes,
            Location changeOffset,
            int grid,
            Function<Thing,String> describer
    ) {
        Function<Location, Location> offsetter = l -> l.add(changeOffset, grid);
        return changes.stream()
                .map(change ->
                        new Change<>(
                                Optional.ofNullable(change.getOriginal()).map(describer).orElse(null),
                                Optional.ofNullable(change.getReplacement()).map(describer).orElse(null),
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
