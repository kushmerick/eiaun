package io.eiaun.organisms.mover;

import io.eiaun.organisms.Genome;
import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.organisms.State;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MoverGenome implements Genome {

    @Getter
    private final double visionRadius;

    private final Mover organism;

    private final Map<String, String> desireableSubstanceProperties;

    private final Random random = new Random();

    public MoverGenome(
            Mover organism,
            Map<String, String> desireableSubstanceProperties,
            double visionRadius
    ) {
        this.organism = organism;
        this.desireableSubstanceProperties = desireableSubstanceProperties;
        this.visionRadius = visionRadius;
    }

    @Override
    public Response respond(
            State state,
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location, Organism> organisms
    ) {
        Substance mySubstance = substances.get(Location.ORIGIN); // might be handy for logging
        Map<Location, Organism> organismChanges = new HashMap<>();
        Map<Location, Substance> desirableSubstances = substances.entrySet().stream()
                .filter(entry -> isDesirable(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (desirableSubstances.containsKey(Location.ORIGIN)) {
            log.info("Contented: Organism {} has desirable substance {}",
                    this.organism.getId(), mySubstance.getId());
        } else {
            desirableSubstances.keySet().stream()
                    // calculate distance to each desirable substance
                    .map(location -> Pair.of(location, location.distanceFromOrigin()))
                    // sort by that distance
                    .sorted(Comparator.comparing(Pair::getRight))
                    .map(Pair::getLeft)
                    // we myopically consider only on the closest desirable substance
                    .findFirst()
                    .ifPresentOrElse(location -> {
                        // location of the nearest desirable substance
                        Substance desiredSubstance = substances.get(location);
                        if (organisms.containsKey(location)) {
                            // can't move directly to the substance, so move to the nearest empty location
                            empties.stream()
                                    .map(empty -> Pair.of(empty, empty.distance(location)))
                                    .sorted(Comparator.comparing(Pair::getRight))
                                    .map(Pair::getLeft)
                                    .findFirst()
                                    .ifPresentOrElse(empty -> {
                                        log.info("Motivated: Organism {} moving by {} toward substance {} at {}",
                                                this.organism.getId(), empty, desiredSubstance.getId(), location);
                                        organismChanges.put(Location.ORIGIN, null);
                                        organismChanges.put(empty, this.organism);
                                    }, () -> {
                                        log.info("Stuck: Organism {} desires {} at {} but can't move",
                                                this.organism.getId(), desiredSubstance.getId(), location);
                                    });
                        } else {
                            // we can move directly to the substance because there is no organism already there
                            log.info("Excited: Organism {} moving directly to substance {} at {}",
                                    this.organism.getId(), desiredSubstance.getId(), location);
                            moveTo(organismChanges, location);
                        }
                    }, () -> {
                        // there is no nearby desirable substance, so move to a random empty location
                        if (!empties.isEmpty()) {
                            Location[] asArray = empties.toArray(Location[]::new);
                            Location empty = asArray[this.random.nextInt(asArray.length)];
                            organismChanges.put(Location.ORIGIN, null);
                            organismChanges.put(empty, this.organism);
                            log.info("Frustrated: Organism {} sees no desirable substances, so moving randomly by {}",
                                    this.organism.getId(), empty);
                        }
                    });
        }
        return Response.of(
                state,
                Collections.emptyMap(),
                organismChanges);
    }

    private void moveTo(Map<Location, Organism> organismChanges, Location location) {
        organismChanges.put(Location.ORIGIN, null);
        organismChanges.put(location, this.organism);
    }

    private boolean isDesirable(Substance substance) {
        for (var entry : this.desireableSubstanceProperties.entrySet()) {
            if (substance.getProperties().get(entry.getKey()).equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

}
