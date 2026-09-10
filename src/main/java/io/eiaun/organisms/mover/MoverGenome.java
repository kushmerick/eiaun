package io.eiaun.organisms.mover;

import io.eiaun.organisms.Genome;
import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.organisms.State;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MoverGenome implements Genome {

    private final Mover organism;
    private final Map<String, String> desireableSubstanceProperties;
    @Getter private final double visionRadius;
    private final double peakEnergy;
    private final double moveEnergy;
    private final double restEnergy;
    private final Random random = new Random();

    public MoverGenome(
            Mover organism,
            Map<String, String> desireableSubstanceProperties,
            double visionRadius,
            double peakEnergy,
            double moveEnergy,
            double restEnergy
    ) {
        this.organism = organism;
        this.desireableSubstanceProperties = desireableSubstanceProperties;
        this.visionRadius = visionRadius;
        this.peakEnergy = peakEnergy;
        this.moveEnergy = moveEnergy;
        this.restEnergy = restEnergy;
    }

    @Override
    public Response respond(
            State state,
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location, Organism> organisms
    ) {
        MoverState moverState = (MoverState) state;
        Map<Location, Organism> organismChanges = new HashMap<>();
        Map<Location, Substance> substanceChanges = new HashMap<>();
        Map<Location, Substance> desirableSubstances = substances.entrySet().stream()
                .filter(entry -> isDesirable(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (desirableSubstances.containsKey(Location.ORIGIN)) {
            Substance substance = desirableSubstances.get(Location.ORIGIN);
            log.info("Content: Organism {} with energy {} found desirable substance {} - {}",
                    this.organism.getId(), moverState.getEnergy(), substance.getId(), explainDesire(substance));
            // eat the substance
            substanceChanges.put(Location.ORIGIN, null);
            // replenish energy
            state = new MoverState(moverState.getEnergy() + this.peakEnergy);
        } else {
            if (moverState.getEnergy() < moveEnergy) {
                log.info("Dead: Organism {} with energy {} has died", this.organism.getId(), moverState.getEnergy());
                return Response.of(
                        state,
                        Collections.emptyMap(),
                        Collections.singletonMap(Location.ORIGIN, null));
            }
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
                        String explanation = explainDesire(desiredSubstance);
                        if (organisms.containsKey(location)) {
                            // can't move directly to the substance, so move to the nearest empty location
                            empties.stream()
                                    .map(empty -> Pair.of(empty, empty.distance(location)))
                                    .sorted(Comparator.comparing(Pair::getRight))
                                    .map(Pair::getLeft)
                                    .findFirst()
                                    .ifPresentOrElse(empty -> {
                                        log.info("Motivated: Organism {} with energy {} moving by {} toward substance {} at {} - {}",
                                                this.organism.getId(), moverState.getEnergy(), empty, desiredSubstance.getId(), location, explanation);
                                        moveTo(organismChanges, empty);
                                    }, () -> {
                                        log.info("Stuck: Organism {} with energy {} desires {} at {} but can't move - {}",
                                                this.organism.getId(), moverState.getEnergy(), desiredSubstance.getId(), location, explanation);
                                    });
                        } else {
                            // we can move directly to the substance because there is no organism already there
                            log.info("Excited: Organism {} with energy {} moving directly to substance {} at {} - {}",
                                    this.organism.getId(), moverState.getEnergy(), desiredSubstance.getId(), location, explanation);
                            moveTo(organismChanges, location);
                        }
                    }, () -> {
                        // there is no nearby desirable substance, so move to a random empty location
                        if (!empties.isEmpty()) {
                            Location[] asArray = empties.toArray(Location[]::new);
                            Location empty = asArray[this.random.nextInt(asArray.length)];
                            moveTo(organismChanges, empty);
                            log.info("Frustrated: Organism {} with energy {} sees no desirable substances, so moving randomly by {}",
                                    this.organism.getId(), moverState.getEnergy(), empty);
                        }
                    });
        }
        State newState = organismChanges.isEmpty()
                ? new MoverState(moverState.getEnergy() - restEnergy)
                : new MoverState(moverState.getEnergy() - moveEnergy);
        return Response.of(
                newState,
                substanceChanges,
                organismChanges);
    }

    private void moveTo(Map<Location, Organism> organismChanges, Location location) {
        organismChanges.put(Location.ORIGIN, null);
        organismChanges.put(location, this.organism);
    }

    private boolean isDesirable(Substance substance) {
        return this.desireableSubstanceProperties.entrySet().stream()
                .anyMatch(entry ->
                        substance.getProperties().get(entry.getKey()).equals(entry.getValue()));
    }

    private String explainDesire(Substance substance) {
        return SetUtils.intersection(
                    this.desireableSubstanceProperties.keySet(),
                    substance.getProperties().keySet())
                .stream()
                .map(property -> {
                    String desiredValue = this.desireableSubstanceProperties.get(property);
                    String actualValue = substance.getProperties().get(property);
                    return property +
                            (Objects.equals(desiredValue, actualValue)
                                    ? ("=" + desiredValue)
                                    : (":" + desiredValue + "/" + actualValue));
                })
                .collect(Collectors.joining(";"));
    }

}
