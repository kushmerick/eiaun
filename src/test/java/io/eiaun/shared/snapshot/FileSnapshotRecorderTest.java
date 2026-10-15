package io.eiaun.shared.snapshot;

import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static io.eiaun.shared.snapshot.FileSnapshotManipulator.YMDHMSS_TIMESTAMP_FORMAT;
import static io.eiaun.shared.snapshot.FileSnapshotManipulator.SNAPSHOT_ID_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSnapshotRecorderTest {

    @Test
    void canFormatFullTimestamp() throws ParseException {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = YMDHMSS_TIMESTAMP_FORMAT.parse(formattedTimestamp).getTime();
        assertEquals(formattedTimestamp, FileSnapshotRecorder.formatYMDHMSS(timestamp));
    }

    @Test
    void canFormatYMDHTimestamp() throws ParseException {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = YMDHMSS_TIMESTAMP_FORMAT.parse(formattedTimestamp).getTime();
        assertEquals("2026-01-02-03", FileSnapshotRecorder.formatYMDH(timestamp));
    }

    @Test
    void canFormatSnapshotPath() throws ParseException {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long timestamp = YMDHMSS_TIMESTAMP_FORMAT.parse(formattedTimestamp).getTime();
        long id = 12345;
        assertEquals(
                String.format(SNAPSHOT_ID_FORMAT, id) + "-" + formattedTimestamp,
                FileSnapshotRecorder.formatSnapshot(id, timestamp));
    }

}
