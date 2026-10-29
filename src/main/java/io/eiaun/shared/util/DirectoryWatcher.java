package io.eiaun.shared.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.Predicates;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public class DirectoryWatcher implements AutoCloseable {

    public enum Verb {
        MONITORING, // a special verb indicating we're done enumerating existing children
        CREATE,
        DELETE,
        MODIFY,
    }

    public interface Listener {
        void notify(Verb verb, Path child)
                throws Exception; // Listeners may throw exceptions, but we log them and continue
        static Listener ifCreate(FailableConsumer<Path, Exception> listener) {
            return (verb, path) -> {
                if (verb == Verb.CREATE) {
                    listener.accept(path);
                }
            };
        }
        static Listener ifMonitoring(
                FailableRunnable<Exception> monitoringListener,
                Listener listener
        ) {
            return (verb, path) -> {
                if (verb == Verb.MONITORING) {
                    monitoringListener.run();
                } else {
                    listener.notify(verb, path);
                }
            };
        }
        static Listener before(
                FailableRunnable<Exception> before,
                Listener listener
        ) {
            return (verb, path) -> {
                before.run();
                listener.notify(verb, path);
            };
        }
    }

    private static final Map<WatchEvent.Kind<Path>, Verb> VERBS = Map.of(
            StandardWatchEventKinds.ENTRY_CREATE, Verb.CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, Verb.DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY, Verb.MODIFY
    );

    @SuppressWarnings("unchecked")
    private static final WatchEvent.Kind<Path>[] KINDS = VERBS.keySet().toArray(new WatchEvent.Kind[0]);

    @Data
    private static class Registration {

        private final Future<?> initializer;
        private final Set<Path> children;
        private final Predicate<Path> filter;
        private final Listener listener;
        private WatchKey key; // set after initialize is complete

        public Registration(
                Future<?> initializer,
                Set<Path> children,
                Predicate<Path> filter,
                Listener listener
        ) {
            this.initializer = initializer;
            this.children = children;
            this.filter = filter;
            this.listener = listener;
        }

        public void close() {
            if (this.initializer != null) {
                this.initializer.cancel(true);
            }
            if (this.key != null) {
                this.key.cancel();
            }
        }
    }

    private final ExecutorService executor;
    private final WatchService watcher;
    private final Future<?> loop;
    private final Map<Path, Registration> registrations = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public DirectoryWatcher(ExecutorService executor) throws IOException {
        this.executor = executor;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.loop = executor.submit(() -> {
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
                    if (!registration.initializer.isDone()) {
                        log.info("Postponing event for path {}", path);
                        // TODO: Does this lose events?
                        // TODO: Does this cause a busy-loop?
                        key.reset();
                        continue;
                    }
                    key.pollEvents().stream()
                            .map(event -> (WatchEvent<Path>) event)
                            .map(event -> Pair.of(
                                    VERBS.get(event.kind()),
                                    path.resolve(event.context())))
                            // discard events with a Kind that we don't recognize
                            // TODO: [{**}] Probably we got OVERFLOW, in which case we should
                            // TODO: do a full rescan, but that's a lot of work, especially
                            // TODO: if we want to preserve our guarantee about notifying
                            // TODO: exactly once for CREATE & DELETE.
                            .filter(pair -> pair.getLeft() != null)
                            // discard events for uninteresting children
                            .filter(pair -> registration.getFilter().test(pair.getRight()))
                            .forEach(pair -> {
                                Verb verb = pair.getLeft();
                                Path child = pair.getRight();
                                Set<Path> children = registration.getChildren();
                                boolean notify = switch (verb) {
                                    case MONITORING -> false; // this can never happen
                                    case CREATE -> children.add(child);
                                    case MODIFY -> { children.add(child); yield true; }
                                    case DELETE -> children.remove(child);
                                };
                                if (notify) {
                                    notify(path, registration.getListener(), verb, child);
                                } else {
                                    log.trace("Swallowing {} {}", verb, child);
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
     * satisfies a given filter.  Note that the listener is first notified about
     * existing files, and then notified about subsequent new, deleted or modified
     * files.
     * @param path The directory to monitor. Do not register a directory again without
     *             invoking the AutoCloseable returned by the previous registration.
     * @param filter A filter for which the subset of files about which to be notified.
     * @param listener A method that will be invoked for each file in the directory.
     *                 Listeners are encouraged to be idempotent, but we guarantee
     *                 that they will be invoked exactly once with a CREATE (DELETE)
     *                 notification for each file that is created (deleted); note that
     *                 multiple MODIFY notification may be sent for a given (sequence of)
     *                 file change(s), depending on the details of the underlying filesystem.
     * @return An AutoCloseable that must be invoked when the client no longer wants
     * to monitor this directory.
     * @throws IllegalStateException If `path` has already been registered but the
     * AutoCloseable returned by its registration has not been invoked.
     * @throws IOException If `path` does not exist or is not a directory, and so forth.
     * @throws InterruptedException The initial enumeration of children may be very slow
     * because it involves IO but more importantly the invoked listeners may be very slow,
     * so this method explicitly checks whether it has been interrupted.
     */
    public AutoCloseable register(
            Path path,
            Predicate<Path> filter,
            Listener listener
    ) throws IOException, InterruptedException {
        return register(path, true, filter, listener);
    }

    /**
     * Register to receive a notification for files in a directory satisfying a given filter
     * that are created, modified or deleted. Optionally, the listener is first notified about
     * existing files.
     * @param path The directory to monitor. Do not register a directory again without
     *             invoking the AutoCloseable returned by the previous registration.
     * @param existing If true, the listener is first notified of existing children. If false,
     *                 the listener is notified only of children that are created, modified
     *                 or deleted after registration.
     * @param filter A filter for which the subset of files about which to be notified.
     * @param listener A method that will be invoked for each file in the directory.
     *                 Listeners are encouraged to be idempotent, but we guarantee
     *                 that they will be invoked exactly once with a CREATE (DELETE)
     *                 notification for each file that is created (deleted); note that
     *                 multiple MODIFY notification may be sent for a given (sequence of)
     *                 file change(s), depending on the details of the underlying filesystem.
     * @return An AutoCloseable that must be invoked when the client no longer wants
     * to monitor this directory.
     * @throws IllegalStateException If `path` has already been registered but the
     * AutoCloseable returned by its registration has not been invoked.
     * @throws IOException If `path` does not exist or is not a directory, and so forth.
     * @throws InterruptedException The initial enumeration of children may be very slow
     * because it involves IO but more importantly the invoked listeners may be very slow,
     * so this method explicitly checks whether it has been interrupted.
     */
    public AutoCloseable register(
            Path path,
            boolean existing,
            Predicate<Path> filter,
            Listener listener
    ) throws InterruptedException, IOException {
        if (this.registrations.containsKey(path)) {
            throw new IllegalStateException("Path " + path + " is already registered");
        }
        // Listeners are encouraged to be idempotent.  But to make life easier on the caller, we
        // guarantee to notify about each child creation and deletion exactly once. (But note that
        // we send a notification for every modification.)  See also the "TO DO" now [{**}] above
        // about OVERFLOW events.
        Set<Path> children = ConcurrentHashMap.newKeySet();
        // first, start an initialization thread that hallucinates CREATE for all existing children,
        // and then registers the path to listen for future changes.
        CountDownLatch storedRegistration = new CountDownLatch(1);
        Future<?> initializer = CompletableFuture.completedFuture(null);
        if (existing) {
            initializer = this.executor.submit(() -> {
                try (Stream<Path> paths = Files.list(path)) {
                    paths.filter(filter).forEach(child -> {
                        children.add(child);
                        notify(path, listener, Verb.CREATE, child);
                    });
                } catch (IOException failure) {
                    // log but squelch this failure in case it is transient
                    log.warn("Failure while enumerating children of {}", path, failure);
                    return;
                }
                // inform caller that we are done enumerating children
                notify(path, listener, Verb.MONITORING, null);
                // register for future events
                try {
                    // We need the registration stored so we can update it with the WatchService registration key,
                    // but we can't store the registration until this thread has started because the registration
                    // contain this thread's Future. Hence this latch avoids a race condition by pausing until
                    // the registration has been stored.
                    storedRegistration.await();
                    this.registrations.get(path)
                            .setKey(path.register(this.watcher, KINDS));
                } catch (InterruptedException _) {
                    log.info("Interrupted while registering {}", path);
                } catch (IOException failure) {
                    log.warn("Failure while registering {}", path, failure);
                }
            });
        }
        Registration registration = new Registration(initializer, children, filter, listener);
        this.registrations.put(path, registration);
        storedRegistration.countDown();
        return () -> {
            registration.close();
            this.registrations.remove(path);
        };
    }

    @Override
    public void close() throws IOException {
        this.registrations.values().forEach(Registration::close);
        this.registrations.clear();
        this.loop.cancel(true);
        this.watcher.close();
    }

    private static void notify(Path path, Listener listener, Verb verb, Path child) {
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
