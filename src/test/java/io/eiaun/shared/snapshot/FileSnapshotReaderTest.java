package io.eiaun.shared.snapshot;

import io.eiaun.shared.util.DirectoryWatcher;
import io.eiaun.shared.util.Functionals;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

import static io.eiaun.shared.snapshot.FileSnapshotManipulator.SNAPSHOT_ID_FORMAT;
import static io.eiaun.shared.snapshot.FileSnapshotManipulator.YMDHMSS;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;
import static org.awaitility.Awaitility.await;
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
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS)
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

    @Test
    void canWatchRecordings() throws Exception {
        String testRecordings = "src/test/resources/test-recordings";
        List<Path> recordings = new CopyOnWriteArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            FileSnapshotReader reader = new FileSnapshotReader(
                    executor,
                    ZSTANDARD,
                    Path.of(testRecordings));
            try (var _ = reader.watchRecordings(
                    DirectoryWatcher.Listener.ifCreate(recordings::add))
            ) {
                Path recording = Path.of(testRecordings).resolve("2026-10-12-07/2026-10-12-07-15/2026-10-12-07-15-56-647");
                await().untilAsserted(() -> assertEquals(
                        List.of(recording),
                        recordings));
                assertEquals(1000, reader.getConfig(recording).getGrid());
            }
        }
    }

    @Test
    void canWatchSnapshots() throws Exception {
        List<Path> snapshots = new CopyOnWriteArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            FileSnapshotReader reader = new FileSnapshotReader(
                    executor,
                    ZSTANDARD,
                    Path.of("src/test/resources/test-recordings"));
            try (var _ = reader.watchSnapshots(
                    Path.of("src/test/resources/test-recordings/2026-10-12-07/2026-10-12-07-15/2026-10-12-07-15-56-647"),
                    DirectoryWatcher.Listener.ifCreate(snapshots::add))
            ) {
                await().untilAsserted(() -> assertEquals(
                        List.of(
                                Path.of("src/test/resources/test-recordings/2026-10-12-07/2026-10-12-07-15/2026-10-12-07-15-56-647/snapshots/2026-10-12-07/2026-10-12-07-16/2026-10-12-07-16-02-788/0000000000000-2026-10-12-07-16-02-788"),
                                Path.of("src/test/resources/test-recordings/2026-10-12-07/2026-10-12-07-15/2026-10-12-07-15-56-647/snapshots/2026-10-12-07/2026-10-12-07-16/2026-10-12-07-16-02-788/0000000000001-2026-10-12-07-16-02-788")),
                        snapshots));
                assertEquals(372050, reader.getOrganisms().get(0, 907).getId());
            }
        }
    }

}