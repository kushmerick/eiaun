package io.eiaun.shared.snapshot;

import io.eiaun.shared.util.DirectoryWatcher;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileSnapshotReaderTest {

    public static final Path TEST_RECORDINGS = Path.of("src/test/resources/test-recordings");

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

}