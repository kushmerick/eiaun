package io.eiaun.snapshot;

import com.google.gson.Gson;
import io.eiaun.organisms.Organism;
import io.eiaun.physics.Change;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

/**
 * Snapshots are written to files like:
 *   recordings/                                                                 Root path
 *     2026-10-13-09/                                                            YYYY-MM-dd-HH when this run began
 *       2026-10-13-09-15-31-672/                                                Full timestamp when this run began
 *         physics.json                                                          Substance properties and other physical details
 *         config.json                                                           Configuration (grid size, initial densities, etc)
 *         snapshots/                                                            Snapshots live here
 *           2026-10-14-10/                                                      YYYY-MM-dd-HH when this snapshot was taken
 *             0000023782371-2026-10-14-10-51-17-186/                            ID-Timestamp of this snapshot
 *               0000023782371-2026-10-14-10-51-17-186.organisms.json.gz         Locations and contents of all organisms
 *               0000023782371-2026-10-14-10-51-17-186.substances.json.gz        Locations and contents of all substances
 *               0000023782371-2026-10-14-10-51-17-186.organism-changes.json.gz  Organism changes that produced this state from the prior state
 *               0000023782371-2026-10-14-10-51-17-186.substance-changes.json.gz Substance changes that produced this state from the prior state
 */
@Slf4j
public class SnapshotFileRecorder implements SnapshotRecorder {

    private static final SimpleDateFormat YMDH_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH");
    private static final SimpleDateFormat FULL_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    private static final String ID_FORMAT = "%013d"; // pad ids with enough 0's so that a trillion sorts alphabetically
    private static final String ROOT = "recordings";
    private static final String PHYSICS = "physics.json";
    private static final String CONFIG = "config.json";
    private static final String SNAPSHOTS = "snapshots";
    private static final String ORGANISMS = "organisms";
    private static final String ORGANISM_CHANGES = "organism-changes";
    private static final String SUBSTANCES = "substances";
    private static final String SUBSTANCE_CHANGES = "substances-changes";
    private static final String DOT_JSON_GZ = ".json.gz";
    private static final String DOT_JSON = ".json";

    private final Path recordingsPath;
    private long snapshotCounter;
    private final Gson gson = new Gson();
    private boolean wrotePreamble = false;

    public SnapshotFileRecorder() {
        long recordingTimestamp = System.currentTimeMillis();
        this.recordingsPath = Path.of(
                ROOT,
                formatYMDHTimestamp(recordingTimestamp),
                formatFullTimestamp(recordingTimestamp));
        if (this.recordingsPath.toFile().exists()) {
            // should never happen, but we may lose data if it does, so let's be overly cautious
            throw new RuntimeException(String.format("Recording timestamp collision: %s", this.recordingsPath));
        }
        log.info("Recording to {}", this.recordingsPath);
        this.snapshotCounter = 0;
    }

    @Override
    public String record(
            Jakku jakku,
            Location changeOffset,
            List<Change<Organism>> organismChanges,
            List<Change<Substance>> substanceChanges
    ) throws IOException {
        if (!wrotePreamble) {
            writePreamble(jakku, this.recordingsPath);
            wrotePreamble = true;
        }
        long snapshotId = this.snapshotCounter++;
        long snapshotTimestamp = System.currentTimeMillis();
        Path snapshotPath = this.recordingsPath.resolve(
                Path.of(SNAPSHOTS,
                        formatYMDHTimestamp(snapshotTimestamp),
                        formatFullTimestamp(snapshotTimestamp),
                        String.format(ID_FORMAT + "-%s", snapshotId, formatFullTimestamp(snapshotTimestamp))));
        writeOrganisms(jakku.getOrganisms(), snapshotPath);
        writeSubstances(jakku.getSubstances(), snapshotPath);
        int grid = jakku.getGrid();
        writeOrganismChanges(organismChanges, changeOffset, grid, snapshotPath);
        writeSubstanceChanges(substanceChanges, changeOffset, grid, snapshotPath);
        return snapshotPath.toString();
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
    ) throws IOException {
        write(this.gson.toJson(Physics.from(jakku)),
                path.resolve(PHYSICS));
        write(this.gson.toJson(Config.from(jakku)),
                path.resolve(CONFIG));
    }

    private void writeOrganisms(
            Organism[][] organisms,
            Path path
    ) throws IOException {
        gzWrite(gson.toJson(thingsAsMap(organisms, Function.identity())),
                path.resolve(ORGANISMS + DOT_JSON_GZ));
    }

    private void writeSubstances(
            Substance[][] substances,
            Path path
    ) throws IOException {
        gzWrite(gson.toJson(thingsAsMap(substances, Substance::getId)),
                path.resolve(SUBSTANCES + DOT_JSON_GZ));
    }

    private static <Thing, Representation> Map<String, Map<String, Representation>> thingsAsMap(
            Thing[][] things,
            Function<Thing, Representation> representer
    ) {
        Map<String, Map<String, Representation>> map = new HashMap<>(); // lat -> lon -> representation
        for (int lat = 0; lat < things.length; lat++) {
            for (int lon = 0; lon < things[lat].length; lon++) {
                if (things[lat][lon] != null) {
                    map.computeIfAbsent(Integer.toString(lat), _ -> new HashMap<>())
                            .put(Integer.toString(lon), representer.apply(things[lat][lon]));
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
    ) throws IOException {
        write(this.gson.toJson(
                        changesAsList(
                                organismChanges,
                                changeOffset,
                                grid,
                                organism -> Long.toString(organism.getId()))),
                path.resolve(ORGANISM_CHANGES + DOT_JSON));
    }

    private void writeSubstanceChanges(
            List<Change<Substance>> substanceChanges,
            Location changeOffset,
            int grid,
            Path path
    ) throws IOException {
        write(this.gson.toJson(
                        changesAsList(
                                substanceChanges,
                                changeOffset,
                                grid,
                                Substance::getId)),
                path.resolve(SUBSTANCE_CHANGES + DOT_JSON));
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

    private void write(String payload, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, payload.getBytes(), StandardOpenOption.CREATE);
    }

    private void gzWrite(String payload, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (FileOutputStream fos = new FileOutputStream(path.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             WritableByteChannel out = Channels.newChannel(gzos)
        ) {
            out.write(ByteBuffer.wrap(payload.getBytes()));
        }
    }

}
