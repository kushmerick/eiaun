package io.eiaun.shared.snapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static io.eiaun.shared.snapshot.FileSnapshotManipulator.SNAPSHOTS;
import static io.eiaun.shared.snapshot.FileSnapshotManipulator.YMDHMSS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSnapshotRecorderTest {

    @Test
    void canFormatFullTimestamp() {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        assertEquals(formattedTimestamp, FileSnapshotRecorder.formatYMDHMSS(timestamp));
    }

    @Test
    void canFormatYMDHTimestamp() {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        assertEquals("2026-01-02-03", FileSnapshotRecorder.formatYMDH(timestamp));
    }

    @Test
    void canFormatYMDHMTimestamp() {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        assertEquals("2026-01-02-03-04", FileSnapshotRecorder.formatYMDHM(timestamp));
    }

}
