package io.eiaun.shared.snapshot;

import io.eiaun.shared.util.DirectoryWatcher;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.eiaun.shared.testresources.TestRecordings.RECORDINGS;
import static io.eiaun.shared.testresources.TestRecordings.RECORDING;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileSnapshotReaderTest {

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
                        Set.of(RECORDING),
                        recordings));
            }
        }
    }

}