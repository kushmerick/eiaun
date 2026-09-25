package io.eiaun.shared.organisms.mover;

import io.eiaun.shared.organisms.Genome;
import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.Response;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MoverGenome extends Genome {

    private static final Random RANDOM = new Random();

    @Getter(AccessLevel.PACKAGE) private final Map<String, String> desirableSubstanceProperties;
    @Getter private final double visionRadius;
    @Getter(AccessLevel.PACKAGE) private final double peakEnergy;
    @Getter(AccessLevel.PACKAGE) private final double moveEnergy;
    @Getter(AccessLevel.PACKAGE) private final double restEnergy;

    public MoverGenome(
            Map<String, String> desirableSubstanceProperties,
            double visionRadius,
            double peakEnergy,
            double moveEnergy,
            double restEnergy
    ) {
        this.desirableSubstanceProperties = desirableSubstanceProperties;
        this.visionRadius = visionRadius;
        this.peakEnergy = peakEnergy;
        this.moveEnergy = moveEnergy;
        this.restEnergy = restEnergy;
    }

    @Override
    public Response respond(
            Jakku jakku,
            State state,
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location, Organism> organisms
    ) {
        Organism organism = organisms.get(Location.ORIGIN);
        MoverState[] moverState = { (MoverState) state };
        List<Change<Organism>> organismChanges = new ArrayList<>();
        List<Change<Substance>> substanceChanges = new ArrayList<>();
        Map<Location, Substance> desirableSubstances = substances.entrySet().stream()
                .filter(entry -> isDesirable(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (desirableSubstances.containsKey(Location.ORIGIN)) {
            Substance substance = desirableSubstances.get(Location.ORIGIN);
            log.info("Content: Organism {} with energy {} found desirable substance {} - {}",
                    organism.getId(), moverState[0].getEnergy(), substance.getId(), explainDesire(substance));
            // eat the substance
            substanceChanges.add(Change.destroy(substance));
            // possibly excrete child substance
            if (substance.getChild() != null) {
                Substance child = jakku.getSubstanceFactory().make(substance.getChild());
                substanceChanges.add(Change.create(child, Location.ORIGIN));
            }
            // replenish energy
            moverState[0] = new MoverState(moverState[0].getEnergy() + this.peakEnergy);
        } else {
            if (moverState[0].getEnergy() < this.moveEnergy) {
                log.info("Dead: Organism {} with energy {} has died", organism.getId(), moverState[0].getEnergy());
                return Response.of(
                        state,
                        Collections.emptyList(),
                        List.of(Change.destroy(organism)));
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
                                                organism.getId(), moverState[0].getEnergy(), empty, desiredSubstance.getId(), location, explanation);
                                        moveTo(organismChanges, empty, organism);
                                    }, () -> {
                                        log.info("Stuck: Organism {} with energy {} desires {} at {} but can't move - {}",
                                                organism.getId(), moverState[0].getEnergy(), desiredSubstance.getId(), location, explanation);
                                    });
                        } else {
                            // we can move directly to the substance because there is no organism already there
                            log.info("Excited: Organism {} with energy {} moving directly to substance {} at {} - {}",
                                    organism.getId(), moverState[0].getEnergy(), desiredSubstance.getId(), location, explanation);
                            moveTo(organismChanges, location, organism);
                        }
                    }, () -> {
                        // there is no nearby desirable substance, so move to a random empty location
                        if (!empties.isEmpty()) {
                            Location[] asArray = empties.toArray(Location[]::new);
                            Location empty = asArray[RANDOM.nextInt(asArray.length)];
                            moveTo(organismChanges, empty, organism);
                            log.info("Frustrated: Organism {} with energy {} sees no desirable substances, so moving randomly by {}",
                                    organism.getId(), moverState[0].getEnergy(), empty);
                        }
                    });
        }
        State newState = organismChanges.isEmpty()
                ? new MoverState(moverState[0].getEnergy() - this.restEnergy)
                : new MoverState(moverState[0].getEnergy() - this.moveEnergy);
        return Response.of(
                newState,
                substanceChanges,
                organismChanges);
    }

    private void moveTo(
            List<Change<Organism>> changes,
            Location location,
            Organism organism
    ) {
        changes.add(Change.move(organism, Location.ORIGIN, location));
    }

    private boolean isDesirable(Substance substance) {
        return this.desirableSubstanceProperties.entrySet().stream()
                .anyMatch(entry ->
                        substance.getProperties().get(entry.getKey()).equals(entry.getValue()));
    }

    private String explainDesire(Substance substance) {
        return SetUtils.intersection(
                        this.desirableSubstanceProperties.keySet(),
                        substance.getProperties().keySet())
                .stream()
                .map(property -> {
                    String desiredValue = this.desirableSubstanceProperties.get(property);
                    String actualValue = substance.getProperties().get(property);
                    return property +
                            (Objects.equals(desiredValue, actualValue)
                                    ? ("=" + desiredValue)
                                    : (":" + desiredValue + "/" + actualValue));
                })
                .collect(Collectors.joining(";"));
    }

}
