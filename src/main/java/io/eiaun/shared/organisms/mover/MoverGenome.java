package io.eiaun.shared.organisms.mover;

import io.eiaun.shared.organisms.Genome;
import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.physics.*;
import io.eiaun.shared.util.Generic;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fory.json.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MoverGenome extends Genome {

    private static final Random RANDOM = new Random();

    @JsonProperty("d") @Getter(AccessLevel.PACKAGE) private Map<String, String> desirableSubstanceProperties;
    @JsonProperty("v") @Getter private double visionRadius;
    @JsonProperty("p") @Getter(AccessLevel.PACKAGE) private double peakEnergy;
    @JsonProperty("m") @Getter(AccessLevel.PACKAGE) private double moveEnergy;
    @JsonProperty("r") @Getter(AccessLevel.PACKAGE) private double restEnergy;

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
    public Changes respond(
            Jakku jakku,
            State state,
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location, Organism> organisms
    ) {
        Organism self = organisms.get(Location.ORIGIN);
        MoverState[] selfState = {(MoverState) state};
        List<Change<Long, Organism>> organismChanges = new ArrayList<>();
        List<Change<String, Substance>> substanceChanges = new ArrayList<>();
        Map<Location, Substance> desirableSubstances = substances.entrySet().stream()
                .filter(entry -> isDesirable(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Location[] newLocation = {Location.ORIGIN};
        if (desirableSubstances.containsKey(Location.ORIGIN)) {
            Substance substance = desirableSubstances.get(Location.ORIGIN);
            log.info("Content: Organism {} with energy {} found desirable substance {} - {}",
                    self.getId(), selfState[0].getEnergy(), substance.getId(), explainDesire(substance));
            // eat the substance
            substanceChanges.add(Change.destroy(substance.getId()));
            // possibly excrete a metabolite
            if (substance.getMetabolite() != null) {
                substanceChanges.add(Change.create(this.substanceFactory.make(substance.getMetabolite()), Location.ORIGIN));
            }
            // replenish energy
            selfState[0] = new MoverState(selfState[0].getEnergy() + this.peakEnergy);
        } else {
            if (selfState[0].getEnergy() < this.moveEnergy) {
                log.info("Dead: Organism {} with energy {} has died", self.getId(), selfState[0].getEnergy());
                return Changes.of(
                        Collections.emptyList(),
                        List.of(Change.destroy(self.getId())));
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
                                                self.getId(), selfState[0].getEnergy(), empty, desiredSubstance.getId(), location, explanation);
                                        moveTo(organismChanges, empty, self);
                                        newLocation[0] = empty;
                                    }, () -> {
                                        log.info("Stuck: Organism {} with energy {} desires {} at {} but can't move - {}",
                                                self.getId(), selfState[0].getEnergy(), desiredSubstance.getId(), location, explanation);
                                    });
                        } else {
                            // we can move directly to the substance because there is no organism already there
                            log.info("Excited: Organism {} with energy {} moving directly to substance {} at {} - {}",
                                    self.getId(), selfState[0].getEnergy(), desiredSubstance.getId(), location, explanation);
                            moveTo(organismChanges, location, self);
                            newLocation[0] = location;
                        }
                    }, () -> {
                        // there is no nearby desirable substance, so move to a random empty location
                        if (!empties.isEmpty()) {
                            Location[] asArray = empties.toArray(Location[]::new);
                            Location empty = asArray[RANDOM.nextInt(asArray.length)];
                            moveTo(organismChanges, empty, self);
                            newLocation[0] = empty;
                            log.info("Frustrated: Organism {} with energy {} sees no desirable substances, so moving randomly by {}",
                                    self.getId(), selfState[0].getEnergy(), empty);
                        }
                    });
        }
        double spent = newLocation[0] == Location.ORIGIN ? this.restEnergy : this.moveEnergy;
        double newEnergy = selfState[0].getEnergy() - spent;
        organismChanges.add(
                Change.update(
                        newLocation[0],
                        Generic.encode(new MoverState(newEnergy), State.STATE_KINDS)));
        return Changes.of(
                substanceChanges,
                organismChanges);
    }

    private void moveTo(
            List<Change<Long,Organism>> changes,
            Location location,
            Organism organism
    ) {
        changes.add(Change.move(organism.getId(), Location.ORIGIN, location));
    }

    private boolean isDesirable(Substance substance) {
        return this.desirableSubstanceProperties.entrySet().stream()
                .anyMatch(entry ->
                        substance.getProperties().get(entry.getKey()).equals(entry.getValue()));
    }

    // just for logging
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
