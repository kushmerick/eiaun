package io.eiaun.physics;

import io.eiaun.concepts.ecosystem.Ecosystem;
import io.eiaun.concepts.ecosystem.Location;
import io.eiaun.concepts.ecosystem.Organism;
import io.eiaun.concepts.ecosystem.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class Jakku implements Ecosystem {

    private static final Random RANDOM = new Random();

    private final int grid;

    @Getter
    private Organism[][] organisms;

    @Setter
    @Getter
    private Substance[][] substances;

    private final Function<Collection<Location>, Iterator<Location>> organismLocationsIteratorGenerator;

    private Iterator<Location> organismLocationsIterator;

    private final Consumer<String> rejectedChangeRecorder;
    private static final Object lock = new Object();

    public Jakku(
            int grid,
            double organismInitialDensity,
            Supplier<Organism> organismCreator,
            double substanceInitialDensity,
            Supplier<Substance> substanceCreator,
            Function<Collection<Location>, Iterator<Location>> organismLocationsIteratorGenerator,
            Consumer<String> rejectedChangeRecorder
    ) {
        log.info("Initializing");
        this.grid = grid;
        // random initial organisms
        this.organismLocationsIteratorGenerator = organismLocationsIteratorGenerator;
        Organism[][] organisms = new Organism[grid][grid];
        int want = (int) (organismInitialDensity * grid * grid);
        int created = 0;
        while (created < want) {
            int lat = RANDOM.nextInt(grid);
            int lon = RANDOM.nextInt(grid);
            if (organisms[lat][lon] == null) {
                organisms[lat][lon] = organismCreator.get();
                created++;
            }
        }
        setOrganisms(organisms);
        // random initial substances
        Substance[][] substances = new Substance[grid][grid];
        want = (int) (substanceInitialDensity * grid * grid);
        created = 0;
        while (created < want) {
            int lat = RANDOM.nextInt(grid);
            int lon = RANDOM.nextInt(grid);
            if (substances[lat][lon] == null) {
                substances[lat][lon] = substanceCreator.get();
                created++;
            }
        }
        setSubstances(substances);
        this.rejectedChangeRecorder = rejectedChangeRecorder;
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

    @Override
    public void step() {
        log.info("Stepping");
        Location location;
        Organism organism;
        Map<Location, Substance> substances;
        Map<Location, Organism> neighbors;
        Set<Location> empty;
        synchronized (lock) {
            location = this.organismLocationsIterator.next();
            int lat = location.getLat();
            int lon = location.getLon();
            organism = this.organisms[lat][lon];
            double radius = organism.getGenome().getVisionRadius();
            empty = lookForEmpties(lat, lon, radius);
            substances = lookForSubstances(lat, lon, radius);
            neighbors = lookForNeighbors(lat, lon, radius);
        }
        Response response = organism.respond(empty, substances, neighbors);
        synchronized (lock) {
            organism.setState(response.newState());
            changeSubstances(location, response);
            if (changeOrganisms(location, response)) {
                updateOrganismLocationsIterator();
            }
        }
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
        for (Map.Entry<Location, Thing> entry : changes.entrySet()) {
            Location destination = location.add(entry.getKey(), things.length, things[0].length);
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
                        "Ignoring invalid %s modification at (%s,%s) from %s to %s",
                        label, destination.getLat(), destination.getLon(),
                        Optional.ofNullable(current).map(describer).orElse(null),
                        Optional.ofNullable(replacement).map(describer).orElse(null)));
            }
        }
        return modified;
    }

    public Set<Location> lookForEmpties(int lat, int lon, double radius) {
        Set<Location> empties = new HashSet<>();
        visit(
                (i, j) -> Pair.of(this.substances[i][j], this.organisms[i][j]),
                lat,
                lon,
                radius,
                (deltaLat, deltaLon, pair) -> {
                    if (pair.getLeft() == null && pair.getRight() == null) {
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
            int lat_ = (lat + deltaLat) % this.grid; // north-south wrap-around
            for (int deltaLon = minDelta; deltaLon <= maxDelta; deltaLon++) {
                int lon_ = (lon + deltaLon) % this.grid; // east-west wrap-around
                visitor.accept(
                        deltaLat,
                        deltaLon,
                        things.get(lat_, lon_));
            }
        }
    }

}