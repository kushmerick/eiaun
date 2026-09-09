package io.eiaun.physics;

import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class Jakku {

    private static final Random RANDOM = new Random();

    private final int grid;

    private final double organismInitialDensity;

    @Getter
    private Organism[][] organisms;

    private final Supplier<Organism> organismCreator;

    private final double substanceInitialDensity;

    @Setter
    @Getter
    private Substance[][] substances;

    private final SubstanceFactory substanceFactory;

    private final Function<Collection<Location>, Iterator<Location>> organismLocationsIteratorGenerator;

    private Iterator<Location> organismLocationsIterator;

    private final Consumer<String> rejectedChangeRecorder;

    private final Object lock = new Object();

    public Jakku(
            int grid,
            double organismInitialDensity,
            Function<Jakku, Organism> organismCreator,
            double substanceInitialDensity,
            SubstanceFactory substanceFactory,
            Function<Collection<Location>, Iterator<Location>> organismLocationsIteratorGenerator,
            Consumer<String> rejectedChangeRecorder
    ) {
        this.grid = grid;
        this.organismInitialDensity = organismInitialDensity;
        this.organismCreator = () -> organismCreator.apply(this);
        this.substanceInitialDensity = substanceInitialDensity;
        this.substanceFactory = substanceFactory;
        this.organismLocationsIteratorGenerator = organismLocationsIteratorGenerator;
        this.rejectedChangeRecorder = rejectedChangeRecorder;
    }

    public void initialize() {
        log.info("Initializing");
        // random initial organisms
        Organism[][] organisms = new Organism[this.grid][this.grid];
        int want = (int) (this.organismInitialDensity * this.grid * this.grid);
        int created = 0;
        while (created < want) {
            int lat = RANDOM.nextInt(this.grid);
            int lon = RANDOM.nextInt(this.grid);
            if (organisms[lat][lon] == null) {
                organisms[lat][lon] = this.organismCreator.get();
                created++;
            }
        }
        setOrganisms(organisms);
        // random initial substances
        Substance[][] substances = new Substance[this.grid][this.grid];
        want = (int) (this.substanceInitialDensity * this.grid * this.grid);
        created = 0;
        while (created < want) {
            int lat = RANDOM.nextInt(this.grid);
            int lon = RANDOM.nextInt(this.grid);
            if (substances[lat][lon] == null) {
                substances[lat][lon] = this.substanceFactory.make();
                created++;
            }
        }
        setSubstances(substances);
    }

    public Map<String,Set<String>> getAllSubstanceProperties() {
        Map<String,Set<String>> substanceProperties = new HashMap<>();
        for (SubstanceSpec spec: this.substanceFactory.getSubstanceSpecs()) {
            spec.getProperties().forEach((p, v) ->
                    substanceProperties.computeIfAbsent(p, (_) -> new HashSet<>()).add(v));
        }
        return substanceProperties;
    }

    public void setOrganisms(Organism[][] organisms) {
        this.organisms = organisms;
        updateOrganismLocationsIterator();
    }

    private void updateOrganismLocationsIterator() {
        List<Location> organismLocations = new ArrayList<>();
        for (int lat = 0; lat < this.grid; lat++) {
            for (int lon = 0; lon < this.grid; lon++) {
                if (this.organisms[lat][lon] != null) {
                    organismLocations.add(Location.of(lat, lon));
                }
            }
        }
        this.organismLocationsIterator = this.organismLocationsIteratorGenerator.apply(organismLocations);
    }

    public CompletableFuture<Void> step(ExecutorService executor) {
        log.trace("Stepping");
        return CompletableFuture.runAsync(() -> {
            Location location;
            Organism organism;
            Map<Location, Substance> substances;
            Map<Location, Organism> neighbors;
            Set<Location> empties;
            synchronized (this.lock) {
                location = this.organismLocationsIterator.next();
                int lat = location.getLat();
                int lon = location.getLon();
                organism = this.organisms[lat][lon];
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
            Map<Location, Thing> changes,
            Thing[][] things,
            // a substance can be changed to a different substance,
            // but an organism cannot be changed to a different organism
            boolean mutable
    ) {
        boolean modified = false;
        for (var entry : changes.entrySet()) {
            Location destination = location.add(entry.getKey(), this.grid);
            Thing current = things[destination.getLat()][destination.getLon()];
            Thing replacement = entry.getValue();
            if ((mutable && current != null && replacement != null)
                    || (current == null && replacement != null)
                    || (current != null && replacement == null)
            ) {
                things[destination.getLat()][destination.getLon()] = replacement;
                modified |= current != replacement;
            } else {
                this.rejectedChangeRecorder.accept(String.format(
                        "Ignoring invalid %s modification at %s from %s to %s",
                        label, destination,
                        Optional.ofNullable(current).map(describer).orElse(null),
                        Optional.ofNullable(replacement).map(describer).orElse(null)));
            }
        }
        return modified;
    }

    public Set<Location> lookForEmpties(int lat, int lon, double radius) {
        Set<Location> empties = new HashSet<>();
        visit(
                (i, j) -> this.organisms[i][j],
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
        return getThings(this.substances, lat, lon, radius, false);
    }

    public Map<Location, Organism> lookForNeighbors(int lat, int lon, double radius) {
        return getThings(this.organisms, lat, lon, radius, true);
    }

    private <Thing> Map<Location, Thing> getThings(
            Thing[][] things,
            int lat,
            int lon,
            double radius,
            boolean skipCenter
    ) {
        Map<Location, Thing> neighbors = new HashMap<>();
        visit(
                (i, j) -> things[i][j],
                lat,
                lon,
                radius,
                (deltaLat, deltaLon, thing) -> {
                    if (!skipCenter || deltaLat != 0 || deltaLon != 0) {
                        if (thing != null) {
                            neighbors.put(Location.of(deltaLat, deltaLon), thing);
                        }
                    }
                });
        return neighbors;
    }

    private interface TwoD<Thing> {
        Thing get(int i, int j);
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