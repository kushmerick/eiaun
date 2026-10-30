package io.eiaun.shared.snapshot;

import io.eiaun.shared.util.Ago;
import io.eiaun.shared.util.DirectoryWatcher;
import io.eiaun.shared.util.DirectoryWatcher.Listener;
import io.eiaun.shared.util.Functionals;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableConsumer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class FileSnapshotReader extends FileSnapshotManipulator implements SnapshotReader {

    private static final double EXPIRATION_SAFETY_FACTOR = 1.1;
    private static final long YMDHM_EXPIRATION = (long) (Duration.ofMinutes(1).toMillis() * EXPIRATION_SAFETY_FACTOR);
    private static final long YMDH_EXPIRATION = (long) (Duration.ofHours(1).toMillis() * EXPIRATION_SAFETY_FACTOR);

    private final ExecutorService executor;
    private final Path root;

    public FileSnapshotReader(
            ExecutorService executor
    ) {
        this(executor, Path.of(ROOT));
    }

    protected FileSnapshotReader(
            ExecutorService executor,
            Path root
    ) {
        this.executor = executor;
        this.root = root;
    }

    /**
     * Receive a callback for every recording, which will be a path like
     * recordings/ymdh/ymdhm/ymdhmss.
     * @param listener Listener
     * @return An AutoCloseable that must be invoked when the caller no longer
     * wants to listen for recordings.
     * @throws RuntimeException Any number of things might go wrong (recording gets
     * deleted, random filesystem errors, and so forth). These exceptions are not
     * throw, rather they are logged and generally cause the watch process to stop
     * (listener is not invoked anymore). The rationale is that in our scenario,
     * there's nothing a caller could do about it anyway.  But let's revisit this
     * decision if it isn't working out.
     **/
    @Override
    public AutoCloseable watchRecordings(
            Listener listener
    ) throws IOException {
        // negative of expiration timestamp -> paths expiring at that timestamp
        SortedMap<Long, Path> expirationPaths = new ConcurrentSkipListMap<>();
        // path -> DW.register's closeable for that path
        Map<Path, AutoCloseable> pathCloseables = new ConcurrentHashMap<>();
        // set of paths whose existing children have been enumerated
        Set<Path> monitoredPaths = ConcurrentHashMap.newKeySet();
        DirectoryWatcher watcher = new DirectoryWatcher(this.executor);
        pathCloseables.put(
                this.root,
                Functionals.get("Adding a recordings watcher for root: " + this.root,
                        () -> watcher.register(
                                this.root,
                                FileSnapshotReader::isYMDH,
                                makeListener(this.root, monitoredPaths, expirationPaths, pathCloseables,
                                        ymdh -> {
                                            expirationPaths.put(
                                                    -getExpiration(ymdh, YMDH, YMDH_EXPIRATION),
                                                    ymdh);
                                            pathCloseables.put(
                                                    ymdh,
                                                    Functionals.get("Adding a recordings watcher for YMDH: " + ymdh,
                                                            () -> watcher.register(
                                                                    ymdh,
                                                                    FileSnapshotReader::isYMDHM,
                                                                    makeListener(ymdh, monitoredPaths, expirationPaths, pathCloseables,
                                                                            ymdhm -> {
                                                                                expirationPaths.put(
                                                                                        -getExpiration(ymdhm, YMDHM, YMDHM_EXPIRATION),
                                                                                        ymdhm);
                                                                                pathCloseables.put(
                                                                                        ymdhm,
                                                                                        Functionals.get("Adding a recordings watcher for YMDHM: " + ymdhm,
                                                                                                () -> watcher.register(
                                                                                                        ymdhm,
                                                                                                        FileSnapshotReader::isYMDHMSS,
                                                                                                        makeListener(ymdhm, monitoredPaths, expirationPaths, pathCloseables, listener))));
                                                                            }))));
                                        }))));
        List<AutoCloseable> closeables = new ArrayList<>(pathCloseables.values());
        closeables.add(watcher);
        return makeCloseable(closeables);
    }

    private long getExpiration(Path path, DateTimeFormatter format, long expirationMillis) {
        return getRecordingTimestamp(path, format) + expirationMillis;
    }

    private Listener makeListener(
            Path path,
            Set<Path> monitoredPaths,
            SortedMap<Long, Path> expirationPaths,
            Map<Path, AutoCloseable> pathCloseables,
            FailableConsumer<Path, Exception> ifCreate
    ) {
        return makeListener(
                path,
                monitoredPaths,
                expirationPaths,
                pathCloseables,
                Listener.ifCreate(ifCreate));
    }

    private Listener makeListener(
            Path path,
            Set<Path> monitoredPaths,
            SortedMap<Long, Path> expirationPaths,
            Map<Path, AutoCloseable> pathCloseables,
            Listener listener
    ) {
        return Listener.before(
                () -> purgeExpiredPaths(monitoredPaths, expirationPaths, pathCloseables),
                Listener.ifMonitoring(
                        () -> monitoredPaths.add(path),
                        listener));
    }

    // Stop monitoring paths that will never get more events.
    // TODO 1: This is invoked only when events are received. Tidy on a schedule.
    // TODO 2: Flip `monitoredPaths` to be "not yet monitored" so doesn't grow indefinitely.
    private void purgeExpiredPaths(
            Set<Path> monitoredPaths,
            SortedMap<Long, Path> expirationPaths,
            Map<Path, AutoCloseable> pathCloseables) {
        try {
            boolean done = false;
            while (!done) {
                log.trace("Purging: {} monitored paths, {} expiration paths, {} path closeables", monitoredPaths.size(), expirationPaths.size(), pathCloseables.size());
                long negativeOldestTimestamp = expirationPaths.firstKey();
                if (-negativeOldestTimestamp < System.currentTimeMillis()) {
                    Path path = expirationPaths.get(negativeOldestTimestamp);
                    if (path != null) { // guard against weird race condition?
                        if (monitoredPaths.contains(path)) {
                            expirationPaths.remove(negativeOldestTimestamp);
                            AutoCloseable closeable = pathCloseables.remove(path);
                            if (closeable != null) { // guard against weird race condition?
                                try {
                                    closeable.close();
                                } catch (Exception failure) {
                                    log.warn("Failure while purging {}", path, failure);
                                }
                            }
                        } else {
                            log.trace("Not purging {} because it is not yet being monitored", path);
                            done = true;
                        }
                    }
                } else {
                    // the oldest timestamp is still too fresh for tidying
                    done = true;
                }
            }
        } catch (NoSuchElementException _) {
            // expirationPaths is empty. This is harmless and should only happen on startup, but let's
            // explicitly catch this exception rather than just check for !expirationPaths.isEmpty()
            // so that we don't need to worry about race conditions.
            log.trace("Nothing to purge!");
        }
    }

    private AutoCloseable makeCloseable(Collection<AutoCloseable> closeables) {
        return () -> {
            Exception failure = new RuntimeException("Failure while closing");
            for (AutoCloseable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Exception f) {
                    failure.addSuppressed(f);
                }
            }
            if (failure.getSuppressed().length > 0) {
                throw failure;
            }
        };
    }

    private static boolean isYMDH(Path path) {
        return isRecordingTimestamp(path, YMDH);
    }

    private static boolean isYMDHM(Path path) {
        return isRecordingTimestamp(path, YMDHM);
    }

    private static boolean isYMDHMSS(Path path) {
        return isRecordingTimestamp(path, YMDHMSS);
    }

    /**
     * A not-very-exciting sample application that watches for new recordings, and then watches for snapshots
     * of each such recording.  Note we abandon the current recording when the next one is discovered -- this
     * aligns to the way VIZ allows the user to select a recording in which they are interested from a
     * dynamically updated list of all recordings.  But this sample is rather silly/useless: because
     * recordings and snapshots are not guaranteed to be enumerated in timestamp order, we log a fairly
     * meaningless subset of each recording's snapshots.
     * @param ignored Command-line args.
     * @throws Exception If something goes wrong.
     */
    static void main(String[] ignored) throws Exception {
        Ago ago = new Ago();
        BlockingQueue<Path> recordings = new LinkedBlockingQueue<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            FileSnapshotReader reader = new FileSnapshotReader(executor);
            // first, start a thread to loop forever, waiting for the next recording, abandoning
            // the previous one, and watching its snapshots.
            Future<Void> snapshotsWatcher = null;
            try {
                snapshotsWatcher = executor.submit(() -> {
                    AutoCloseable previous = null;
                    try {
                        while (true) {
                            Path recording = recordings.take();
                            if (previous != null) {
                                Functionals.run(previous::close);
                            }
                            Snapshots snapshots = FileSnapshots.of(recording);
                            log.info("Recording ({}): {}",
                                    ago.describe(snapshots.getRecordingTimestamp()),
                                    recording);
                            log.info("Config - Organism density {}", snapshots.getConfig().getOrganismDensity());
                            log.info("Physics - Substances {}", snapshots.getPhysics().getSubstanceSpecs().size());
// TODO: Bring back some of the snapshot functionality
//                            previous = reader.watchSnapshots(
//                                    recording,
//                                    Listener.ifCreate(snapshot -> {
//                                        Pair<Long,Long> indexTimestamp = snapshots.getSnapshotIndexAndTimestamp(snapshot);
//                                        long index = indexTimestamp.getLeft();
//                                        log.info("Snapshot {} {} ({} org changes; {} sub changes): {}",
//                                                index,
//                                                ago.describe(indexTimestamp.getRight()),
//                                                snapshots.getSnapshotOrganismChanges(snapshot).size(),
//                                                snapshots.getSnapshotSubstanceChanges(snapshot).size(),
//                                                snapshot);
//                                        if (index == 0) {
//                                            // Just for fun, demonstrate loading organisms from a full dump.
//                                            // This works only for the first snapshot, because we can't guarantee
//                                            // that snapshots are enumerated in order.
//                                            Snapshots.StepToResult result = snapshots.stepToSnapshot(snapshot);
//                                            log.info("First snapshot ({}/{}): {} organisms, {} substances", result.clean(), result.dirt(), snapshots.getOrganisms().firstIndices().size(), snapshots.getSubstances().firstIndices().size());
//                                        }
//                                    }));
                        }
                    } catch (InterruptedException _) {
                        throw new RuntimeException("Interrupted");
                    } catch (Exception failure) {
                        throw new RuntimeException("Oops", failure);
                    } finally {
                        if (previous != null) {
                            Functionals.run(previous::close);
                        }
                    }
                });
                // second, enqueue recordings as they are discovered, for consumption by the above thread
                try (var _ = reader.watchRecordings(Listener.ifCreate(recordings::offer))) {
                    // wait forever
                    new CountDownLatch(1).await();
                }
            } finally {
                if (snapshotsWatcher != null) {
                    snapshotsWatcher.cancel(true);
                }
            }
        }
    }

}
