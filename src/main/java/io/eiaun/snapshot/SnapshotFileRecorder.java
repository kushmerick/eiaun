package io.eiaun.snapshot;

import com.google.gson.Gson;
import io.eiaun.physics.Jakku;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;

/**
 * Snapshots are written to files like:
 *   recordings/                                                           Root path
 *      2026-10-13-09/                                                     YYYY-MM-dd-HH when this run began
 *         2026-10-13-09-15-31-672/                                        Full timestamp when this run began
 *             2026-10-14-10/                                              YYYY-MM-dd-HH when this snapshot was taken
 *                0000023782371-2026-10-14-10-51-17-186/                   ID-Timestamp of this snapshot
 *                   0000023782371-2026-10-14-10-51-17-186.state.gz        Locations and contents of all substances and organisms
 *                   0000023782371-2026-10-14-10-51-17-186.transitions.gz  Substance and organism changes that produced this state from the previous state.
 */
@Slf4j
public class SnapshotFileRecorder implements SnapshotRecorder {

    private final static SimpleDateFormat YMDH_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH");
    private final static SimpleDateFormat FULL_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    private final static String ID_FORMAT = "%013d"; // pad ids with enough 0's so that a trillion sorts alphabetically
    private final static String ROOT = "recordings";

    private final Path recordingsPath;
    private long snapshotCounter;
    private final Gson gson = new Gson();

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
    public String record(Jakku jakku) {
        long snapshotId = this.snapshotCounter++;
        long snapshotTimestamp = System.currentTimeMillis();
        Path snapshotPath = this.recordingsPath.resolve(
                Path.of(formatYMDHTimestamp(snapshotTimestamp),
                        formatFullTimestamp(snapshotTimestamp),
                        String.format(ID_FORMAT + "-%s", snapshotId, formatFullTimestamp(snapshotTimestamp))));
        write(jakku, snapshotPath);
        return snapshotPath.toString();
    }

    private String formatFullTimestamp(long timestamp) {
        return FULL_TIMESTAMP_FORMAT.format(new Date(timestamp));
    }

    private String formatYMDHTimestamp(long timestamp) {
        return YMDH_TIMESTAMP_FORMAT.format(new Date(timestamp));
    }

    private void write(Jakku jakku, Path snapshotPath) {
        // TODO
    }

}
