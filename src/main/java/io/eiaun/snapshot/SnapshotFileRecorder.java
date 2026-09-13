package io.eiaun.snapshot;

import com.google.gson.Gson;
import io.eiaun.organisms.Organism;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

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
 *               0000023782371-2026-10-14-10-51-17-186.substance-changes.json.gz Organism changes that produced this state from the prior state
 */
@Slf4j
public class SnapshotFileRecorder implements SnapshotRecorder {

    private final static SimpleDateFormat YMDH_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH");
    private final static SimpleDateFormat FULL_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    private final static String ID_FORMAT = "%013d"; // pad ids with enough 0's so that a trillion sorts alphabetically
    private final static String ROOT = "recordings";
    private static final String PHYSICS = "physics.json";
    private static final String CONFIG = "config.json";
    private static final String SNAPSHOTS = "snapshots";

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
            Map<Location, Organism> organismChanges,
            Map<Location, Substance> substanceChanges
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
        writeOrganismChanges(organismChanges, snapshotPath);
        writeSubstanceChanges(substanceChanges, snapshotPath);
        return snapshotPath.toString();
    }

    private String formatFullTimestamp(long timestamp) {
        return FULL_TIMESTAMP_FORMAT.format(new Date(timestamp));
    }

    private String formatYMDHTimestamp(long timestamp) {
        return YMDH_TIMESTAMP_FORMAT.format(new Date(timestamp));
    }

    private void writePreamble(Jakku jakku, Path path) throws IOException {
        write(this.gson.toJson(Physics.from(jakku)),
                path.resolve(PHYSICS));
        write(this.gson.toJson(Config.from(jakku)),
                path.resolve(CONFIG));
    }

    private void writeOrganisms(Organism[][] organisms, Path path) {
        // TODO
    }

    private void writeSubstances(Substance[][] substances, Path path) {
        // TODO
    }

    private void writeOrganismChanges(Map<Location, Organism> organismChanges, Path path) {
        // TODO
    }

    private void writeSubstanceChanges(Map<Location, Substance> substanceChanges, Path path) {
        // TODO
    }

    private void write(String payload, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, payload.getBytes(), StandardOpenOption.CREATE);
    }


}
