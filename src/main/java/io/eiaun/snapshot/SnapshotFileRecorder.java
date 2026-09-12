package io.eiaun.snapshot;

import io.eiaun.physics.Jakku;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
public class SnapshotFileRecorder implements SnapshotRecorder {

    public static SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    private static final File ROOT = new File("recordings");

    private final File recordings;
    private final Object lock = new Object();

    public SnapshotFileRecorder() {
        String recordingTimestamp = formatTimestamp(System.currentTimeMillis());
        this.recordings = new File(ROOT, recordingTimestamp);
        if (this.recordings.exists()) {
            // should never happen, but we may lose data if it does, so let's be overly cautious
            throw new RuntimeException(String.format("Recording timestamp collision: %s", recordingTimestamp));
        }
        log.info("Recording to {}", this.recordings.getPath());
    }

    @Override
    public CompletableFuture<Long> record(Jakku jakku, Executor executor) {
        long[] snapshotTimestamp = new long[1];
        return CompletableFuture.runAsync(() -> {
                    synchronized (lock) {
                        snapshotTimestamp[0] = System.currentTimeMillis();
                        while (snapshotExists(snapshotTimestamp[0])) {
                            log.info("Avoiding snapshot timestamp collision");
                            snapshotTimestamp[0]++;
                        }
                        File snapshotDir = snapshotDir(snapshotTimestamp[0]);
                        log.info("Snapshotting to {}", snapshotDir.getPath());
                        write(jakku, snapshotDir);
                    }
                }, executor)
                .thenApply(_ -> snapshotTimestamp[0]);
    }

    private String formatTimestamp(long timestamp) {
        return TIMESTAMP_FORMATTER.format(new Date(timestamp));
    }

    private File snapshotDir(long timestamp) {
        return new File(this.recordings, String.valueOf(timestamp));
    }

    private boolean snapshotExists(long timestamp) {
        return snapshotDir(timestamp).exists();
    }

    private void write(Jakku jakku, File snapshotDir) {
        // TODO
    }

}
