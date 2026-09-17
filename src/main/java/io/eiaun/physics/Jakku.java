package io.eiaun.physics;

import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.snapshot.SnapshotRecorder;
import io.eiaun.util.TwoD;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class Jakku {

    private static final Random RANDOM = new Random();

    @Getter
    private final int grid;

    @Getter
    private final double organismDensity;

    @Getter
    private final double substanceDensity;

    @Getter
    private TwoD<Organism> organisms;

    private final Supplier<Organism> organismCreator;

    @Setter
    @Getter
    private TwoD<Substance> substances;

    @Getter
    private final SubstanceFactory substanceFactory;

    private final Function<Collection<Location>, Iterator<Location>> organismLocationsIteratorGenerator;

    private Iterator<Location> organismLocationsIterator;

    private final Consumer<String> rejectedChangeRecorder;

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
        this.grid = grid;
        this.organismDensity = organismDensity;
        this.organismCreator = () -> organismCreator.apply(this);
        this.substanceDensity = substanceDensity;
        this.substanceFactory = substanceFactory;
        this.organismLocationsIteratorGenerator = organismLocationsIteratorGenerator;
        this.rejectedChangeRecorder = rejectedChangeRecorder;
        this.snapshotRecorder = snapshotRecorder;
    }

    public void initialize() {
        log.info("Initializing");
        // random initial organisms
        TwoD<Organism> organisms = new TwoD<>();
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
        setOrganisms(organisms);
        // random initial substances
        TwoD<Substance> substances = new TwoD<>();
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
        setSubstances(substances);
    }

    public Map<String, Set<String>> getAllSubstanceProperties() {
        Map<String, Set<String>> substanceProperties = new HashMap<>();
        for (SubstanceSpec spec : this.substanceFactory.getSubstanceSpecs()) {
            spec.getProperties().forEach((p, v) ->
                    substanceProperties.computeIfAbsent(p, (_) -> new HashSet<>()).add(v));
        }
        return substanceProperties;
    }

    public void setOrganisms(TwoD<Organism> organisms) {
        this.organisms = organisms;
        updateOrganismLocationsIterator();
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
                            Response response = organism.respond(empties, substances, neighbors);
                            log.debug("Updating for organism {}'s response with {} substance changes and {} organism changes",
                                    organism.getId(), response.substanceChanges().size(), response.organismChanges().size());
                            synchronized (this.lock) {
                                organism.setState(response.newState());
                                changeSubstances(location, response);
                                if (changeOrganisms(location, response)) {
                                    updateOrganismLocationsIterator();
                                }
                                try {
                                    String snapshotId = this.snapshotRecorder.record(
                                            this,
                                            location,
                                            response.organismChanges(),
                                            response.substanceChanges());
                                    log.info("Snapshot {}", snapshotId);
                                } catch (IOException failure) {
                                    throw new RuntimeException("Failure while writing snapshot", failure);
                                }
                                return !this.organismLocationsIterator.hasNext();
                            }
                        }, executor);
    }

    private void changeSubstances(Location location, Response response) {
        changeThings("substance",
                Substance::getId,
                location,
                response.substanceChanges(),
                this.substances,
                true);
    }

    private boolean changeOrganisms(Location location, Response response) {
        return changeThings("organism",
                Organism::getId,
                location,
                response.organismChanges(),
                this.organisms,
                false);
    }

    private <Thing> boolean changeThings(
            String label,
            Function<Thing, Object> describer,
            Location location,
            List<Change<Thing>> changes,
            TwoD<Thing> things,
            // a substance can be changed to a different substance,
            // but an organism cannot be changed to a different organism
            boolean mutable
    ) {
        boolean modified = false;
        for (var change : changes) {
            boolean rejected = false;
            if (change.isCreation()) {
                Location to = location.add(change.getTo(), this.grid);
                int lat = to.getLat();
                int lon = to.getLon();
                if (things.contains(lat, lon)) {
                    // another thing is already at the destination
                    rejected = true;
                } else {
                    things.set(lat, lon, change.getReplacement());
                    modified = true;
                }
            } else if (change.isDestroy()) {
                Location from = location.add(change.getFrom(), this.grid);
                int lat = from.getLat();
                int lon = from.getLon();
                if (!Objects.equals(things.get(lat, lon), change.getOriginal())) {
                    // already changed, or no-op (already deleted)
                    rejected = true;
                } else {
                    things.remove(lat, lon);
                    modified = true;
                }
            } else if (change.isReplacement()) {
                Location from = location.add(change.getFrom(), this.grid);
                int lat = from.getLat();
                int lon = from.getLon();
                Thing thing = things.get(lat, lon);
                if (!Objects.equals(thing, change.getOriginal()) ||
                        Objects.equals(thing, change.getReplacement())) {
                    // original is already gone
                    // no-op (already replaced)
                    rejected = true;
                } else {
                    things.set(lat, lon, change.getReplacement());
                    modified = true;
                }
            } else if (change.isMove()) {
                Location from = location.add(change.getFrom(), this.grid);
                int flat = from.getLat();
                int flon = from.getLon();
                Location to = location.add(change.getTo(), this.grid);
                int tlat = to.getLat();
                int tlon = to.getLon();
                if (!Objects.equals(things.get(flat, flon), change.getOriginal()) ||
                        things.contains(tlat, tlon)) {
                    // original object changed or been removed,
                    // or another thing is already at the destination
                    rejected = true;
                } else {
                    things.remove(flat, flon);
                    things.set(tlat, tlon, change.getOriginal());
                    modified = true;
                }
            } else {
                log.warn(String.format("Gibberish change: %s", change));
                rejected = true;
            }
            if (rejected) {
                this.rejectedChangeRecorder.accept(String.format(
                        "Rejecting %s change: %s -> %s; %s -> %s",
                        label,
                        Optional.ofNullable(change.getOriginal()).map(describer).orElse(null),
                        Optional.ofNullable(change.getReplacement()).map(describer).orElse(null),
                        change.getFrom(),
                        change.getTo()));
            }
        }
        return modified;
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