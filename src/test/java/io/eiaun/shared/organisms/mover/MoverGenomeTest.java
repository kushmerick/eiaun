package io.eiaun.shared.organisms.mover;

import io.eiaun.shared.fakes.FakeOrganism;
import io.eiaun.shared.fakes.FakeSubstanceFactory;
import io.eiaun.shared.physics.Changes;
import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.*;
import io.eiaun.shared.util.Generic;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoverGenomeTest {

    private static final Map<String, String> DESIRABLE_PROPERTIES =
            Map.of("P1", "V1");
    private static final String METABOLITE = "S2";
    private static final Substance DESIRABLE_SUBSTANCE = new Substance(
            "S1",
            DESIRABLE_PROPERTIES,
            METABOLITE
    );
    private static final double VISION_RADIUS = 10;
    private static final double PEAK_ENERGY = 20;
    private static final double MOVE_ENERGY = 30;
    private static final double REST_ENERGY = 40;

    private static MoverGenome make() {
        return new MoverGenome(
                DESIRABLE_PROPERTIES,
                VISION_RADIUS,
                PEAK_ENERGY,
                MOVE_ENERGY,
                REST_ENERGY
        );
    }

    private static Generic makeState(double energy) {
        return Generic.encode(new MoverState(energy), MoverState.STATE_KINDS);
    }

    @Test
    void canEat() {
        SubstanceFactory substanceFactory = new FakeSubstanceFactory();
        Jakku jakku = mock(Jakku.class);
        when(jakku.getSubstanceFactory()).thenReturn(substanceFactory);
        MoverGenome genome = make();
        genome.setSubstanceFactory(substanceFactory);
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(jakku);
        Changes changes = genome.respond(
                jakku,
                state,
                Collections.emptySet(),
                Map.of(Location.ORIGIN, DESIRABLE_SUBSTANCE),
                Map.of(Location.ORIGIN, self));
        double newEnergy = state.getEnergy() + PEAK_ENERGY - REST_ENERGY;
        assertEquals(
                List.of(
                        Change.destroy(DESIRABLE_SUBSTANCE.getId()),
                        Change.create(substanceFactory.make(METABOLITE), Location.ORIGIN)),
                changes.substanceChanges());
        assertEquals(
                List.of(Change.update(Location.ORIGIN, makeState(newEnergy))),
                changes.organismChanges());
    }

    @Test
    void canDie() {
        MoverGenome genome = make();
        MoverState state = new MoverState(MOVE_ENERGY - 1);
        Organism organism = FakeOrganism.make(null);
        Changes changes = genome.respond(
                null,
                state,
                Collections.emptySet(),
                Collections.emptyMap(),
                Map.of(Location.ORIGIN, organism));
        assertTrue(changes.substanceChanges().isEmpty());
        assertEquals(
                List.of(Change.destroy(organism.getId())),
                changes.organismChanges());
    }

    @Test
    void canBeMotivated() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Organism neighbor = FakeOrganism.make(null);
        Location oneOne = Location.of(1, 1);
        Location oneTwo = Location.of(1, 2);
        Changes changes = genome.respond(
                null,
                state,
                Set.of(oneTwo),
                Map.of(oneOne, DESIRABLE_SUBSTANCE),
                Map.of(Location.ORIGIN, self, oneOne, neighbor));
        assertTrue(changes.substanceChanges().isEmpty());
        double newEnergy = state.getEnergy() - MOVE_ENERGY;
        assertEquals(
                List.of(Change.move(self.getId(), Location.ORIGIN, oneTwo),
                        Change.update(oneTwo, makeState(newEnergy))),
                changes.organismChanges());
    }

    @Test
    void canBeStuck() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Organism neighbor = FakeOrganism.make(null);
        Location oneOne = Location.of(1, 1);
        Changes changes = genome.respond(
                null,
                state,
                Collections.emptySet(),
                Map.of(oneOne, DESIRABLE_SUBSTANCE),
                Map.of(Location.ORIGIN, self, oneOne, neighbor));
        assertTrue(changes.substanceChanges().isEmpty());
        double newEnergy = state.getEnergy() - REST_ENERGY;
        assertEquals(
                List.of(Change.update(Location.ORIGIN, makeState(newEnergy))),
                changes.organismChanges());
    }

    @Test
    void canBeExcited() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Location oneOne = Location.of(1, 1);
        Changes changes = genome.respond(
                null,
                state,
                Collections.emptySet(),
                Map.of(oneOne, DESIRABLE_SUBSTANCE),
                Map.of(Location.ORIGIN, self));
        assertTrue(changes.substanceChanges().isEmpty());
        double newEnergy = state.getEnergy() - MOVE_ENERGY;
        assertEquals(
                List.of(Change.move(self.getId(), Location.ORIGIN, oneOne),
                        Change.update(oneOne, makeState(newEnergy))),
                changes.organismChanges());
    }

    @Test
    void canBeFrustrated() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Location oneOne = Location.of(1, 1);
        Changes changes = genome.respond(
                null,
                state,
                Set.of(oneOne),
                Collections.emptyMap(),
                Map.of(Location.ORIGIN, self));
        assertTrue(changes.substanceChanges().isEmpty());
        double newEnergy = state.getEnergy() - MOVE_ENERGY;
        assertEquals(
                List.of(Change.move(self.getId(), Location.ORIGIN, oneOne),
                        Change.update(oneOne, makeState(newEnergy))),
                changes.organismChanges());
    }

}