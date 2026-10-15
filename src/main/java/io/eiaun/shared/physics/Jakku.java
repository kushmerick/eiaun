package io.eiaun.shared.physics;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.Response;
import io.eiaun.shared.snapshot.SnapshotRecorder;
import io.eiaun.shared.util.TwoD;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class Jakku {

    private static final Random RANDOM = new Random();

    @Getter private final int grid;

    @Getter private final double organismDensity;

    @Getter private final double substanceDensity;

    private final ChangeApplier changeApplier;

    @Getter private final TwoD<Organism> organisms;

    private final Supplier<Organism> organismCreator;

    @Getter private final TwoD<Substance> substances;

    @Getter private final SubstanceFactory substanceFactory;

    private final Function<Collection<Location>, Iterator<Location>> organismLocationsIteratorGenerator;

    private Iterator<Location> organismLocationsIterator;

    @Getter private final Consumer<String> rejectedChangeRecorder;

    private final SnapshotRecorder snapshotRecorder;

    private final Object lock = new Object();

    public Jakku(
            int grid,
            double organismDensity,
            Function<Jakku, Organism> organismCreator,
            double substanceDensity,
            SubstanceFactory substanceFactory,
            Function<Collection<Location>, Iterator<Location>> organismLocationsIteratorGenerator,
            Consumer<String> rejectedChangeRecorder,
            SnapshotRecorder snapshotRecorder
    ) {
        this.organisms = new TwoD<>();
        this.substances = new TwoD<>();
        this.grid = grid;
        this.organismDensity = organismDensity;
        this.organismCreator = () -> organismCreator.apply(this);
        this.substanceDensity = substanceDensity;
        this.substanceFactory = substanceFactory;
        this.organismLocationsIteratorGenerator = organismLocationsIteratorGenerator;
        this.rejectedChangeRecorder = rejectedChangeRecorder;
        this.changeApplier = new ChangeApplier(
                this.organisms,
                this.substances,
                this.grid,
                this.rejectedChangeRecorder);
        this.snapshotRecorder = snapshotRecorder;
    }

    public void initialize() {
        log.info("Initializing");
        // random initial organisms
        this.organisms.clear();
        int want = (int) (this.organismDensity * this.grid * this.grid);
        int created = 0;
        while (created < want) {
            int lat = RANDOM.nextInt(this.grid);
            int lon = RANDOM.nextInt(this.grid);
            if (!organisms.contains(lat, lon)) {
                organisms.set(lat, lon, this.organismCreator.get());
                created++;
            }
        }
        updateOrganismLocationsIterator();
        // random initial substances
        this.substances.clear();
        want = (int) (this.substanceDensity * this.grid * this.grid);
        created = 0;
        while (created < want) {
            int lat = RANDOM.nextInt(this.grid);
            int lon = RANDOM.nextInt(this.grid);
            if (!substances.contains(lat, lon)) {
                substances.set(lat, lon, this.substanceFactory.make());
                created++;
            }
        }
    }

    public Map<String, Set<String>> getAllSubstanceProperties() {
        Map<String, Set<String>> substanceProperties = new HashMap<>();
        this.substanceFactory.getSubstanceSpecs().values().forEach(spec ->
                spec.getProperties().forEach((p, v) ->
                        substanceProperties.computeIfAbsent(p, (_) -> new HashSet<>()).add(v)));
        return substanceProperties;
    }

    private void updateOrganismLocationsIterator() {
        List<Location> organismLocations = new ArrayList<>();
        for (int lat : this.organisms.firstIndices()) {
            for (int lon : this.organisms.secondIndices(lat)) {
                organismLocations.add(Location.of(lat, lon));
            }
        }
        this.organismLocationsIterator = this.organismLocationsIteratorGenerator.apply(organismLocations);
    }

    public CompletableFuture<Boolean> step(ExecutorService executor) {
        log.trace("Stepping");
        return CompletableFuture
                .supplyAsync(
                        () -> {
                            Location location;
                            Organism organism;
                            Map<Location, Substance> substances;
                            Map<Location, Organism> neighbors;
                            Set<Location> empties;
                            synchronized (this.lock) {
                                if (!this.organismLocationsIterator.hasNext()) {
                                    // Protect ourselves against race/edge cases where extinction is not caught in
                                    // time to halt this step. The most obvious case is just concurrent step
                                    // execution. A subtler scenario is where some prior step both lead to extinction
                                    // and failed, in which case Control logged the failure but did not terminate
                                    // the run.
                                    log.warn("Undetected extinction");
                                    return true;
                                }
                                location = this.organismLocationsIterator.next();
                                int lat = location.getLat();
                                int lon = location.getLon();
                                organism = this.organisms.get(lat, lon);
                                double radius = organism.getGenome().getVisionRadius();
                                empties = lookForEmpties(lat, lon, radius);
                                substances = lookForSubstances(lat, lon, radius);
                                neighbors = lookForNeighbors(lat, lon, radius);
                            }
                            log.debug("Stepping organism {} at {} with {} neighbors, {} substances, {} empties",
                                    organism.getId(), location, neighbors.size(), substances.size(), empties.size());
                            Response response = organism.respond(this, empties, substances, neighbors);
                            log.debug("Updating for organism {}'s response with {} substance changes and {} organism changes",
                                    organism.getId(), response.substanceChanges().size(), response.organismChanges().size());
                            Runnable fileWrites = null;
                            synchronized (this.lock) {
                                this.changeApplier.changeSubstances(location, response.substanceChanges());
                                if (this.changeApplier.changeOrganisms(location, response.organismChanges())) {
                                    updateOrganismLocationsIterator();
                                }
                                Pair<String, Runnable> info = this.snapshotRecorder.record(
                                        this,
                                        location,
                                        response.organismChanges(),
                                        response.substanceChanges());
                                if (info != null) {
                                    // null means snapshots are disabled
                                    log.info("Snapshot {}", info.getLeft());
                                    fileWrites = info.getRight();
                                }
                            }
                            if (fileWrites != null) {
                                // null means snapshots are disabled
                                fileWrites.run();
                            }
                            synchronized (this.lock) {
                                return !this.organismLocationsIterator.hasNext();
                            }
                        }, executor);
    }

    public Set<Location> lookForEmpties(int lat, int lon, double radius) {
        Set<Location> empties = new HashSet<>();
        visit(
                this.organisms,
                lat,
                lon,
                radius,
                (deltaLat, deltaLon, organism) -> {
                    if (organism == null) {
                        empties.add(Location.of(deltaLat, deltaLon));
                    }
                });
        return empties;
    }

    public Map<Location, Substance> lookForSubstances(int lat, int lon, double radius) {
        return getThings(this.substances, lat, lon, radius);
    }

    public Map<Location, Organism> lookForNeighbors(int lat, int lon, double radius) {
        return getThings(this.organisms, lat, lon, radius);
    }

    private <Thing> Map<Location, Thing> getThings(
            TwoD<Thing> things,
            int lat,
            int lon,
            double radius
    ) {
        Map<Location, Thing> neighbors = new HashMap<>();
        visit(
                things,
                lat,
                lon,
                radius,
                (deltaLat, deltaLon, thing) -> {
                    if (thing != null) {
                        neighbors.put(Location.of(deltaLat, deltaLon), thing);
                    }
                });
        return neighbors;
    }

    private <Thing> void visit(
            TwoD<Thing> things,
            int lat,
            int lon,
            double radius,
            TriConsumer<Integer, Integer, Thing> visitor
    ) {
        // TODO: True circular vision (instead of square)?
        int minDelta = -Math.toIntExact(Math.round(radius));
        double maxDelta = radius;
        for (int deltaLat = minDelta; deltaLat <= maxDelta; deltaLat++) {
            int lat_ = wrap(lat + deltaLat); // north-south wrap-around
            for (int deltaLon = minDelta; deltaLon <= maxDelta; deltaLon++) {
                int lon_ = wrap(lon + deltaLon); // east-west wrap-around
                visitor.accept(
                        deltaLat,
                        deltaLon,
                        things.get(lat_, lon_));
            }
        }
    }

    int wrap(int index) {
        return Location.wrap(index, this.grid);
    }

}