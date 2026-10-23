package io.eiaun.shared.snapshot;

import io.eiaun.shared.util.DirectoryWatcher;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.eiaun.shared.snapshot.FileSnapshotManipulator.SNAPSHOT_ID_FORMAT;
import static io.eiaun.shared.snapshot.FileSnapshotManipulator.YMDHMSS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class FileSnapshotReaderTest {

    public static final Path TEST_RECORDINGS = Path.of("src/test/resources/test-recordings");

    @Test
    void canParseSnapshotPath() {
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long id = 12345;
        String path = String.format(SNAPSHOT_ID_FORMAT, id) + "-" + formattedTimestamp;
        Optional<Pair<Long, Long>> parsed = FileSnapshotManipulator.parseSnapshot(Path.of(path));
        assertTrue(parsed.isPresent());
        assertEquals(id, parsed.get().getLeft());
        long timestamp = LocalDateTime.parse(formattedTimestamp, YMDHMSS)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        assertEquals(timestamp, parsed.get().getRight());
    }

    @Test
    void canRecognizeSnapshotPath() {
        FileSnapshotReader reader = new FileSnapshotReader(null, null);
        String formattedTimestamp = "2026-01-02-03-04-05-678";
        long id = 12345;
        String path = String.format(SNAPSHOT_ID_FORMAT, id) + "-" + formattedTimestamp;
        assertTrue(reader.isSnapshot(Path.of(path)));
    }

    @Test
    void canRecognizeInvalidSnapshotPath() {
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
        Path testRecordings = TEST_RECORDINGS;
        Set<Path> recordings = ConcurrentHashMap.newKeySet();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            FileSnapshotReader reader = new FileSnapshotReader(
                    executor,
                    testRecordings);
            try (var _ = reader.watchRecordings(
                    DirectoryWatcher.Listener.ifCreate(recordings::add))
            ) {
                Path recording = testRecordings.resolve("2026-10-12-07/2026-10-12-07-15/2026-10-12-07-15-56-647");
                await().untilAsserted(() -> assertEquals(
                        Set.of(recording),
                        recordings));
            }
        }
    }

    @Test
    void canWatchSnapshots() throws Exception {
        Set<Path> snapshots = ConcurrentHashMap.newKeySet();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            FileSnapshotReader reader = new FileSnapshotReader(executor, TEST_RECORDINGS);
            try (var _ = reader.watchSnapshots(
                    TEST_RECORDINGS.resolve("2026-10-12-07/2026-10-12-07-15/2026-10-12-07-15-56-647"),
                    DirectoryWatcher.Listener.ifCreate(snapshots::add))
            ) {
                await().untilAsserted(() -> assertEquals(
                        Set.of(TEST_RECORDINGS.resolve("2026-10-12-07/2026-10-12-07-15/2026-10-12-07-15-56-647/snapshots/2026-10-12-07/2026-10-12-07-16/2026-10-12-07-16-02-788/0000000000000-2026-10-12-07-16-02-788"),
                                TEST_RECORDINGS.resolve("2026-10-12-07/2026-10-12-07-15/2026-10-12-07-15-56-647/snapshots/2026-10-12-07/2026-10-12-07-16/2026-10-12-07-16-02-788/0000000000001-2026-10-12-07-16-02-788")),
                        snapshots));
            }
        }
    }

}