package io.eiaun.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.Predicates;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public class DirectoryWatcher implements AutoCloseable {

    private final WatchService watcher;

    public enum Verb {
        CREATE,
        DELETE,
        MODIFY,
    }

    public interface Listener {
        void notify(Verb verb, Path child)
                throws Exception; // Listeners may throw exceptions, but we log them and continue
        static Listener ifCreate(FailableConsumer<Path, Exception> listener) {
            return (verb, path) -> {
                if (verb == DirectoryWatcher.Verb.CREATE) {
                    listener.accept(path);
                }
            };
        }
    }

    private static final Map<WatchEvent.Kind<Path>, Verb> VERBS = Map.of(
            StandardWatchEventKinds.ENTRY_CREATE, Verb.CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, Verb.DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY, Verb.MODIFY
    );

    private record Registration(
            Set<Path> children,
            Predicate<Path> filter,
            Listener listener
    ) {}

    @SuppressWarnings("unchecked")
    private static final WatchEvent.Kind<Path>[] KINDS = VERBS.keySet().toArray(new WatchEvent.Kind[0]);

    private final Future<?> future;
    private final Map<Path, Registration> registrations = new ConcurrentHashMap<>();

    public DirectoryWatcher(ExecutorService executor) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.future = executor.submit(() -> {
            try {
                while (true) {
                    WatchKey key = this.watcher.take();
                    Path path = (Path) key.watchable();
                    Registration registration = registrations.get(path);
                    if (registration == null) {
                        // Should not happen - maybe a shut-down race condition - let's find out
                        log.warn("Ignoring event for path {}", path);
                        key.reset();
                        continue;
                    }
                    Set<Path> children = registration.children();
                    Predicate<Path> filter = registration.filter();
                    Listener listener = registration.listener();
                    key.pollEvents().forEach(e -> {
                        @SuppressWarnings("unchecked") WatchEvent<Path> event = (WatchEvent<Path>) e;
                        Path child = path.resolve(event.context());
                        WatchEvent.Kind<Path> kind = event.kind();
                        Verb verb = VERBS.get(kind);
                        if (verb != null) {
                            if (filter.test(child)) {
                                boolean notify = switch (verb) {
                                    case CREATE -> children.add(child);
                                    case MODIFY -> { children.add(child); yield true; }
                                    case DELETE -> children.remove(child);
                                };
                                if (notify) {
                                    notify(path, listener, verb, child);
                                } else {
                                    log.info("Swallowing {} {}", verb, child);
                                }
                            }
                        } else {
                            // TODO: [{**}] Probably we got OVERFLOW, in which case we should
                            // TODO: do a full rescan, but that's a lot of work, especially
                            // TODO: if we want to preserve our guarantee about notifying
                            // TODO: exactly once for CREATE & DELETE.
                            log.warn("Unknown kind: {}", kind);
                        }
                    });
                    key.reset();
                }
            } catch (InterruptedException _) {
                log.info("Interrupted");
            } catch (ClosedWatchServiceException _) {
                log.info("Closed");
            }
        });
    }

    /**
     * Register to receive a notification for every file in a directory that
     * passes a given test.  Note that the listener is first notified about
     * existing files, and then notified about subsequent new, deleted or modified
     * files. Note also that notifications for the initial set of files are sent in
     * alphabetical order.  As a general-purpose utility, this may not be a very
     * helpful. But for our EIAUN use-case, this is extremely helpful, because
     * the filenames in question contain timestamps, so this ordering is
     * equivalent to a guarantee that notifications are ordered from oldest
     * to newest file.
     * @param path The directory to monitor.
     * @param filter A filter for which the subset of files about which to be notified.
     * @param listener A method that will be invoked for each file in the directory.
     *                 Listeners are encouraged to be idempotent, but we guarantee
     *                 that they will be invoked exactly once with a CREATE (DELETE)
     *                 notification for each file that is created (deleted); note that
     *                 multiple MODIFY notification may be sent for a given (sequence of)
     *                 file change(s), depending on the details of the underlying filesystem.
     * @return An AutoCloseable that must be invoked when the client no longer wants
     * to monitor this directory.
     * @throws IOException If `path` does not exist or is not a directory, and so forth.
     */
    public AutoCloseable register(Path path, Predicate<Path> filter, Listener listener) throws IOException {
        // Listeners are encouraged to be idempotent.  But to make life easier on the caller, we
        // guarantee to notify about each child creation and deletion exactly once. (But note that
        // we send a notification for every modification.)  See also the "TO DO" now [{**}] above
        // about OVERFLOW events.
        Set<Path> children = ConcurrentHashMap.newKeySet();
        // first, record this registration
        this.registrations.put(path, new Registration(children, filter, listener));
        // second, hallucinate CREATE for all existing children
        try (Stream<Path> paths = Files.list(path)) {
            paths
                .filter(filter)
                .sorted()
                .forEachOrdered(child -> {
                    children.add(child);
                    notify(path, listener, Verb.CREATE, child);
            });
        }
        // third, start watching for changes
        WatchKey key = path.register(this.watcher, KINDS);
        return () -> {
            key.cancel();
            this.registrations.remove(path);
        };
    }

    @Override
    public void close() throws IOException {
        this.registrations.clear();
        this.watcher.close();
        this.future.cancel(true);
    }

    private void notify(Path path, Listener listener, Verb verb, Path child) {
        try {
            listener.notify(verb, child);
        } catch (Exception failure) {
            log.warn("Failure while notifying listener of {} that {} {}", path, verb, child, failure);
        }
    }

    static void main(String[] ignored) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             DirectoryWatcher watcher = new DirectoryWatcher(executor);
             AutoCloseable _ = watcher.register(
                     Path.of("/tmp"),
                     Predicates.truePredicate(),
                     (verb, path) -> log.info("{} --> {}", verb, path)))
        {
            new CountDownLatch(1).await();
        }
    }

}
