package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.ChangeApplier;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.physics.SubstanceFactory;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.DirectoryWatcher;
import io.eiaun.shared.util.Functionals;
import io.eiaun.shared.util.JSON;
import io.eiaun.shared.util.TwoD;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fory.reflect.TypeRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;

@Slf4j
public class FileSnapshotReader extends FileSnapshotManipulator implements SnapshotReader {

    private static final double EXPIRATION_SAFETY_FACTOR = 1.1;
    private static final long YMDHMSS_EXPIRATION = (long) (Duration.ofMillis(5).toMillis() * EXPIRATION_SAFETY_FACTOR); // 1 milli should be fine
    private static final long YMDHM_EXPIRATION = (long) (Duration.ofMinutes(1).toMillis() * EXPIRATION_SAFETY_FACTOR);
    private static final long YMDH_EXPIRATION = (long) (Duration.ofHours(1).toMillis() * EXPIRATION_SAFETY_FACTOR);
    private static final TypeRef<List<Change<Long>>> ORGANISM_CHANGES_TYPE = new TypeRef<>() {};
    private static final TypeRef<List<Change<String>>> SUBSTANCE_CHANGES_TYPE = new TypeRef<>() {};

    private final ExecutorService executor;
    private final String compression;

    // These fields encapsulate the state used to track organisms & substances
    // across snapshots.  (Remember that for performance reasons,
    // FileSnapshotRecorder does not dump the full arrays of organisms and substances
    // for each snapshot; instead it writes just the changes, with a full dump
    // at for the first snapshot and and occasional subsequent snapshots.)
    private boolean watchingSnapshot = false;
    @Getter private final TwoD<Organism> organisms = new TwoD<>();
    @Getter private final TwoD<Substance> substances = new TwoD<>();
    private ChangeApplier changeApplier;
    private FullDumpReader fullDumpReader;
    private SubstanceFactory substanceFactory;

    /**
     * @param executor         An ExecutorService on which monitoring tasks will run.
     * @param compression      Extension used for compressing various snapshot files.
     *                         Note that this value is configured from application.yaml
     *                         hence could vary by recording. That seems implausible
     *                         since the default `zstd` is so good. But if it were ever
     *                         changed, then snapshot-watching would fail!
     **/
    public FileSnapshotReader(
            ExecutorService executor,
            String compression) {
        this.executor = executor;
        this.compression = compression;
    }

    /**
     * Receive a callback for every recording, which will be a path like
     * recordings/ymdh/ymdhm/ymdhmss.
     * @param listener Listener
     * @throws RuntimeException Any number of things might go wrong (recording gets
     * deleted, random filesystem errors, and so forth). These exceptions are not
     * throw, rather they are logged and generally cause the watch process to stop
     * (listener is not invoked anymore). The rationale is that in our scenario,
     * there's nothing a caller could do about it anyway.  But let's revisit this
     * decision if it isn't working out.
     **/
    @Override
    public AutoCloseable watchRecordings(DirectoryWatcher.Listener listener) throws IOException {
        // negative of expiration timestamp -> paths expiring at that timestamp
        SortedMap<Long, Path> expirationPaths = new ConcurrentSkipListMap<>();
        // path -> DW.register closeable for that path
        Map<Path, AutoCloseable> pathCloseables = new ConcurrentHashMap<>();
        Path root = Path.of(ROOT);
        DirectoryWatcher watcher = new DirectoryWatcher(this.executor);
        pathCloseables.put(root, Functionals.get("Adding a recordings watcher for root: " + ROOT,
                () -> watcher.register(
                        root,
                        this::isYMDH,
                        ifCreate(ymdh -> {
                            tidy(expirationPaths, pathCloseables);
                            expirationPaths.put(
                                    -getExpiration(ymdh, YMDH, YMDH_EXPIRATION),
                                    ymdh);
                            pathCloseables.put(ymdh, Functionals.get("Adding a recordings watcher for YMDH: " + ymdh,
                                    () -> watcher.register(
                                            ymdh,
                                            this::isYMDHM,
                                            ifCreate(ymdhm -> {
                                                tidy(expirationPaths, pathCloseables);
                                                expirationPaths.put(
                                                        -getExpiration(ymdhm, YMDHM, YMDHM_EXPIRATION),
                                                        ymdhm);
                                                pathCloseables.put(ymdhm, Functionals.get("Adding a recordings watcher for YMDHM: " + ymdhm,
                                                        () -> watcher.register(
                                                                ymdhm,
                                                                this::isYMDHMSS,
                                                                listener)));
                                            }))));
                        }))));
        return makeCloseable("watching recordings", pathCloseables.values());
    }

    /**
     * Receive a callback for every snapshot in the given recording, which
     * will be a path like recordings/ymdh/ymdhm/ymdhmss/snapshots/ymdh/ymdhm/ymdhmss/00123-ymdhmss
     * Important Note: At most one snapshot can be watched at a time!
     * @param recording Recording path
     * @param listener Listener
     * @throws RuntimeException Any number of things might go wrong (recording gets
     * deleted, random filesystem errors, and so forth). These exceptions are logged
     * and generally cause the watch process to stop (listener is not invoked anymore).
     * The rationale is that in our scenario, there's nothing a caller could do about
     * it anyway.  But let's revisit this decision if it isn't working out.
     */
    @Override
    public AutoCloseable watchSnapshots(Path recording, DirectoryWatcher.Listener listener) throws IOException {
        if (this.watchingSnapshot) {
            throw new IllegalStateException("Already watching a snapshot");
        }
        this.watchingSnapshot = true;
        this.organisms.clear();
        this.substances.clear();
        this.substanceFactory = new SubstanceFactory(
                getPhysics(recording).getSubstanceSpecs().values());
        this.changeApplier = new ChangeApplier(
                this.organisms,
                this.substances,
                getConfig(recording).getGrid(),
                log::info);
        this.fullDumpReader = new FullDumpReader(
                this.compression,
                this.substanceFactory);
        // negative expiration timestamp -> paths for that timestamp
        SortedMap<Long, Path> expirationPaths = new ConcurrentSkipListMap<>();
        // path -> directory-watcher closeable for that path
        Map<Path, AutoCloseable> pathCloseables = new ConcurrentHashMap<>();
        Path snapshots = recording.resolve(SNAPSHOTS);
        DirectoryWatcher watcher = new DirectoryWatcher(this.executor);
        pathCloseables.put(snapshots, Functionals.get("Adding a snapshots watcher for snapshots: " + snapshots,
                () -> watcher.register(
                        snapshots,
                        this::isYMDH,
                        ifCreate(ymdh -> {
                            tidy(expirationPaths, pathCloseables);
                            expirationPaths.put(
                                    -getExpiration(ymdh, YMDH, YMDH_EXPIRATION),
                                    ymdh);
                            pathCloseables.put(ymdh, Functionals.get("Adding a snapshots watcher for YMDH: " + ymdh,
                                    () -> watcher.register(
                                            ymdh,
                                            this::isYMDHM,
                                            ifCreate(ymdhm -> {
                                                tidy(expirationPaths, pathCloseables);
                                                expirationPaths.put(
                                                        -getExpiration(ymdhm, YMDHM, YMDHM_EXPIRATION),
                                                        ymdhm);
                                                pathCloseables.put(ymdhm, Functionals.get("Adding a snapshots watcher for YMDHM: " + ymdhm,
                                                        () -> watcher.register(
                                                                ymdhm,
                                                                this::isYMDHMSS,
                                                                ifCreate(ymdhmss -> {
                                                                    tidy(expirationPaths, pathCloseables);
                                                                    expirationPaths.put(
                                                                            -getExpiration(ymdhmss, YMDHMSS, YMDHMSS_EXPIRATION),
                                                                            ymdhmss);
                                                                    pathCloseables.put(ymdhmss, Functionals.get("Adding a snapshots watcher for YMDHMSS: " + ymdhmss,
                                                                            () -> watcher.register(
                                                                                    ymdhmss,
                                                                                    this::isSnapshot,
                                                                                    (verb, snapshot) -> {
                                                                                        updateOrganismsAndSubstances(snapshot);
                                                                                        listener.notify(verb, snapshot);
                                                                                    })));
                                                                }))));
                                            }))));
                        }))));
        pathCloseables.put(
                // ten thousand apologies for this hack, but it works
                Path.of("¡PLACEHOLDER!"),
                () -> this.watchingSnapshot = false);
        return makeCloseable("watching snapshots", pathCloseables.values());
    }

    private void updateOrganismsAndSubstances(Path snapshot) {
        if (this.organisms.isEmpty()) {
            // presumably this is our first snapshot, so we expect to see a full dump
            // of the organisms and snapshots.
            try {
                processFullDump(snapshot, this.fullDumpReader::maybeGetOrganisms, this.organisms::replace, "organisms");
                processFullDump(snapshot, this.fullDumpReader::maybeGetSubstances, this.substances::replace, "substances");
            } catch (RuntimeException e) {
                log.warn("Missing full dump(s) for {}", snapshot, e);
            }
        } else {
            this.changeApplier.changeOrganisms(getOrganismChanges(snapshot));
            this.changeApplier.changeSubstances(getSubstanceChanges(snapshot));
            // Sanity check
            fullDumpReader.maybeGetOrganisms(snapshot).ifPresent(organisms -> {
                if (!this.organisms.equals(organisms, Organism::getId)) {
                    this.organisms.replace(organisms);
                    log.warn("Organisms mismatch!");
                }
            });
            fullDumpReader.maybeGetSubstances(snapshot).ifPresent(substances -> {
                if (!this.substances.equals(substances, Substance::getId)) {
                    this.substances.replace(substances);
                    log.warn("Substances mismatch!");
                }
            });
        }
    }

    private <Id, Thing> Map<Id, Thing> makeIndex(
            TwoD<Thing> things,
            Function<Thing, Id> identifier
    ) {
        Map<Id, Thing> index = new HashMap<>();
        for (int lat: things.firstIndices()) {
            for (int lon: things.secondIndices(lat)) {
                Thing thing = things.get(lat, lon);
                index.put(identifier.apply(thing), thing);
            }
        }
        return index;
    }

    private <Thing> void processFullDump(
            Path snapshot,
            Function<Path, Optional<TwoD<Thing>>> fullDumpGetter,
            Consumer<TwoD<Thing>> processor,
            String label
    ) {
            fullDumpGetter.apply(snapshot).ifPresentOrElse(
                    processor,
                    () -> { throw new RuntimeException(String.format("Missing %s full dump", label)); });
    }

    // Unregister paths from DirectoryWatcher if they will not change.
    // TODO: Note this is invoked only when CREATE events are received.
    // TODO: It would be better to tidy on a schedule.
    private void tidy(
            SortedMap<Long, Path> expirationPaths,
            Map<Path, AutoCloseable> pathCloseables) {
        try {
            boolean done = false;
            while (!done) {
                log.trace("Tidying! {} expiration paths, {} path closeables", expirationPaths.size(), pathCloseables.size());
                long negativeOldestTimestamp = expirationPaths.firstKey();
                if (-negativeOldestTimestamp < System.currentTimeMillis()) {
                    Path path = expirationPaths.remove(negativeOldestTimestamp);
                    if (path != null) { // guard against weird race condition?
                        AutoCloseable closeable = pathCloseables.remove(path);
                        if (closeable != null) { // guard against weird race condition?
                            try {
                                closeable.close();
                            } catch (Exception failure) {
                                log.warn("Failure while tidying {}", path, failure);
                            }
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
            log.trace("Nothing to tidy!");
        }
    }

    private AutoCloseable makeCloseable(
            String description,
            Collection<AutoCloseable> closeables
    ) {
        return () -> {
            Exception failure = new RuntimeException(String.format("Failure while closing after %s", description));
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

    public Config getConfig(Path recording) {
        return Functionals.get(() -> JSON.fromJson(
                Files.readString(recording.resolve(CONFIG)),
                Config.class));
    }

    public Physics getPhysics(Path recording) {
        return Functionals.get(() -> JSON.fromJson(
                Files.readString(recording.resolve(PHYSICS)),
                Physics.class));
    }

    public List<Change<Substance>> getSubstanceChanges(Path snapshot) {
        // TODO: Generate index once and keep them updated, rather than regenerating
        Map<String, Substance> substanceIndex = makeIndex(this.substances, Substance::getId);
        return Functionals.get(() -> JSON.fromJson(
                Files.readString(snapshot.resolve(SUBSTANCE_CHANGES)),
                SUBSTANCE_CHANGES_TYPE))
                .stream()
                .map(change ->
                        change.translate(substanceIndex::get))
                .toList();
    }

    public List<Change<Organism>> getOrganismChanges(Path snapshot) {
        // TODO: Generate index once and keep them updated, rather than regenerating
        Map<Long, Organism> organismIndex = makeIndex(this.organisms, Organism::getId);
        return Functionals.get(() -> JSON.fromJson(
                Files.readString(snapshot.resolve(ORGANISM_CHANGES)),
                ORGANISM_CHANGES_TYPE))
                .stream()
                .map(change ->
                        change.translate(organismIndex::get))
                .toList();
    }

    private DirectoryWatcher.Listener ifCreate(FailableConsumer<Path, Exception> listener) {
        return (verb, path) -> {
            if (verb == DirectoryWatcher.Verb.CREATE) {
                listener.accept(path);
            }
        };
    }

    Optional<Pair<Long, Long>> parseSnapshot(Path path) {
        // sample:
        // recordings/2026-10-08-21/2026-10-08-21-34/2026-10-08-21-34-26-815/snapshots/2026-10-08-21/2026-10-08-21-35/2026-10-08-21-35-05-848/0000000001129-2026-10-08-21-35-05-848
        String name = path.getFileName().toString();
        int dash = name.indexOf('-');
        if (dash == SNAPSHOT_ID_WIDTH) {
            long id = Long.parseLong(name.substring(0, dash));
            name = name.substring(dash + 1);
            Optional<Long> timestamp = parse(Path.of(name), YMDHMSS);
            return timestamp.map(t -> Pair.of(id, t));
        }
        return Optional.empty();
    }

    private Optional<Long> parse(Path path, DateTimeFormatter format) {
        String name = path.getFileName().toString();
        try {
            long timestamp = LocalDateTime.parse(name, format)
                            .atZone(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli();
            return Optional.of(timestamp);
        } catch (DateTimeParseException failure) {
            log.warn("Failure while parsing `{}` with `{}`", path, format, failure);
        }
        return Optional.empty();
    }

    private long getExpiration(Path path, DateTimeFormatter format, long expirationMillis) {
        return parse(path, format)
                .map(timestamp -> timestamp + expirationMillis)
                .orElseThrow();
    }

    private boolean isYMDH(Path path) {
        return parse(path, YMDH).isPresent();
    }

    private boolean isYMDHM(Path path) {
        return parse(path, YMDHM).isPresent();
    }

    private boolean isYMDHMSS(Path path) {
        return parse(path, YMDHMSS).isPresent();
    }

    boolean isSnapshot(Path path) {
        return parseSnapshot(path).isPresent();
    }

    /**
     * A not-very-exciting sample application that watches for new recordings, and then watches for every snapshot
     * of each such recording.  The trick is that at most one recording can be watched at a time, so we abandon
     * the previous recording when a new recording is encountered.  Real applications will do something more
     * interesting -- eg coming soon, VIZ allows the user to select a recording in which they are interested from
     * a dynamically updated list of all recordings.
     * @param ignored Ignored command-line args.
     * @throws Exception If something goes wrong.
     */
    static void main(String[] ignored) throws Exception {
        final String COMPRESSION = ZSTANDARD; // let's assume this application.yaml property is not altered between recordings
        MutableInt nrecordings = new MutableInt(0);
        MutableInt nsnapshots = new MutableInt(0);
        BlockingQueue<Path> recordings = new LinkedBlockingQueue<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            FileSnapshotReader reader = new FileSnapshotReader(executor, COMPRESSION);
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
                            previous = reader.watchSnapshots(
                                    recording,
                                    (_, snapshot) -> {
                                        log.info("Snapshot {}: {}", nsnapshots.incrementAndGet(), snapshot);
                                        // the following is not intended to be meaningful, it just shows
                                        // some functionality
                                        log.info("Has {} organisms ({} changes) and {} substances ({} changes)",
                                                reader.getOrganisms().firstIndices().size(),
                                                reader.getOrganismChanges(snapshot).size(),
                                                reader.getSubstances().firstIndices().size(),
                                                reader.getSubstanceChanges(snapshot).size());
                                    });
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
                // second, enqueue recordings as they are discovered, for consumption by the above thread.
                try (var _ = reader.watchRecordings(
                        reader.ifCreate(recording -> {
                            log.info("Recording {}: {}", nrecordings.incrementAndGet(), recording);
                            log.info("Config - Organism density {}", reader.getConfig(recording).getOrganismDensity());
                            log.info("Physics - Substances {}", reader.getPhysics(recording).getSubstanceSpecs().size());
                            recordings.offer(recording);
                        }))
                ) {
                    // wait until the process is interrupted
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
