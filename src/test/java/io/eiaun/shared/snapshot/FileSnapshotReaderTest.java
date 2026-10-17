package io.eiaun.shared.snapshot;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static io.eiaun.shared.snapshot.FileSnapshotManipulator.SNAPSHOT_ID_FORMAT;
import static io.eiaun.shared.snapshot.FileSnapshotManipulator.YMDHMSS_TIMESTAMP_FORMAT;
import static org.junit.jupiter.api.Assertions.*;

class FileSnapshotReaderTest {

    @Test
    void canParseSnapshotPath() throws ParseException {
        FileSnapshotReader reader = new FileSnapshotReader(null, null);
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long id = 12345;
        String path = String.format(SNAPSHOT_ID_FORMAT, id) + "-" + formattedTimestamp;
        Optional<Pair<Long, Long>> parsed = reader.parseSnapshot(Path.of(path));
        assertTrue(parsed.isPresent());
        assertEquals(id, parsed.get().getLeft());
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS_TIMESTAMP_FORMAT)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        assertEquals(timestamp, parsed.get().getRight());
    }

    @Test
    void canRecognizeSnapshotPath() throws ParseException {
        FileSnapshotReader reader = new FileSnapshotReader(null, null);
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long id = 12345;
        String path = String.format(SNAPSHOT_ID_FORMAT, id) + "-" + formattedTimestamp;
        assertTrue(reader.isSnapshot(Path.of(path)));
    }

    @Test
    void canRecognizeInvalidSnapshotPath() throws ParseException {
        FileSnapshotReader reader = new FileSnapshotReader(null, null);
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long id = 12345;
        String path = String.format(SNAPSHOT_ID_FORMAT, id) + "-" + "ABCD" + formattedTimestamp;
        assertFalse(reader.isSnapshot(Path.of(path)));
        path = id + "-" + formattedTimestamp;
        assertFalse(reader.isSnapshot(Path.of(path)));
    }

}