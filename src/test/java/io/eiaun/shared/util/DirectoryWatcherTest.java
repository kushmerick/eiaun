package io.eiaun.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class DirectoryWatcherTest {

    @BeforeAll
    static void setUp() {
        Awaitility.setDefaultPollInterval(500, TimeUnit.MILLISECONDS);
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultTimeout(Duration.ofSeconds(10));
    }

    @AfterAll
    static void tearDown() {
        Awaitility.reset();
    }

    @Test
    void canWatch(@TempDir Path dir) throws Exception {
        Path a1 = Files.createFile(dir.resolve("A1"));
        Files.createFile(dir.resolve("B1"));
        Set<Path> paths = ConcurrentHashMap.newKeySet();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             DirectoryWatcher watcher = new DirectoryWatcher(executor);
             AutoCloseable _ = watcher.register(
                    dir,
                    child -> child.getFileName().toString().startsWith("A"),
                    (verb, child) -> {
                        if (verb == DirectoryWatcher.Verb.DELETE) {
                            paths.remove(child);
                        } else {
                            paths.add(child);
                        }
                    })
        ) {
            assertEquals(Set.of(a1), paths);
            Path a2 = Files.createFile(dir.resolve("A2"));
            Path b2 = Files.createFile(dir.resolve("B2"));
            await().untilAsserted(() -> assertEquals(Set.of(a1, a2), paths));
            Files.delete(a2);
            Files.delete(b2);
            await().untilAsserted(() -> assertEquals(Set.of(a1), paths));
        }
    }

}