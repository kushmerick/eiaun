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

    public static final Path RECORDINGS = Path.of("src/test/resources/test-recordings");

    @Test
    void canWatchRecordings() throws Exception {
        Set<Path> recordings = ConcurrentHashMap.newKeySet();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            FileSnapshotReader reader = new FileSnapshotReader(
                    executor,
                    RECORDINGS);
            try (var _ = reader.watchRecordings(
                    DirectoryWatcher.Listener.ifCreate(recordings::add))
            ) {
                await().untilAsserted(() -> assertEquals(
                        Set.of(RECORDINGS.resolve("2026-11-01-15/2026-11-01-15-12/2026-11-01-15-12-31-275")),
                        recordings));
            }
        }
    }

}