package io.eiaun.shared.snapshot;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static io.eiaun.shared.snapshot.FileSnapshotManipulator.SNAPSHOT_ID_FORMAT;
import static io.eiaun.shared.snapshot.FileSnapshotManipulator.YMDHMSS_TIMESTAMP_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSnapshotRecorderTest {

    @Test
    void canFormatFullTimestamp() {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS_TIMESTAMP_FORMAT)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        assertEquals(formattedTimestamp, FileSnapshotRecorder.formatYMDHMSS(timestamp));
    }

    @Test
    void canFormatYMDHTimestamp() {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS_TIMESTAMP_FORMAT)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        assertEquals("2026-01-02-03", FileSnapshotRecorder.formatYMDH(timestamp));
    }

    @Test
    void canFormatSnapshotPath() {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS_TIMESTAMP_FORMAT)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        long id = 12345;
        assertEquals(
                String.format(SNAPSHOT_ID_FORMAT, id) + "-" + formattedTimestamp,
                FileSnapshotRecorder.formatSnapshot(id, timestamp));
    }

}
