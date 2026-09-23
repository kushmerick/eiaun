package io.eiaun.organisms.mover;

import io.eiaun.fakes.FakeOrganism;
import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.physics.Change;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoverGenomeTest {

    private static final Map<String, String> DESIRABLE_PROPERTIES =
            Map.of("P1", "V1");
    private static final Substance DESIRABLE_SUBSTANCE = new Substance(
            "S1",
            DESIRABLE_PROPERTIES
    );
    private static final double VISION_RADIUS = 10;
    private static final double PEAK_ENERGY = 20;
    private static final double MOVE_ENERGY = 30;
    private static final double REST_ENERGY = 40;

    MoverGenome make() {
        return new MoverGenome(
                DESIRABLE_PROPERTIES,
                VISION_RADIUS,
                PEAK_ENERGY,
                MOVE_ENERGY,
                REST_ENERGY
        );
    }

    @Test
    void canEat() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Response response = genome.respond(
                state,
                Collections.emptySet(),
                Map.of(Location.ORIGIN, DESIRABLE_SUBSTANCE),
                Map.of(Location.ORIGIN, self));
        MoverState newState = (MoverState) response.newState();
        assertEquals(
                state.getEnergy() + PEAK_ENERGY - REST_ENERGY,
                newState.getEnergy());
        assertEquals(
                List.of(Change.destroy(DESIRABLE_SUBSTANCE)),
                response.substanceChanges());
        assertTrue(response.organismChanges().isEmpty());
    }

    @Test
    void canDie() {
        MoverGenome genome = make();
        MoverState state = new MoverState(MOVE_ENERGY - 1);
        Organism organism = FakeOrganism.make(null);
        Response response = genome.respond(
                state,
                Collections.emptySet(),
                Collections.emptyMap(),
                Map.of(Location.ORIGIN, organism));
        assertEquals(state, response.newState());
        assertTrue(response.substanceChanges().isEmpty());
        assertEquals(
                List.of(Change.destroy(organism)),
                response.organismChanges());
    }

    @Test
    void canBeMotivated() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Organism neighbor = FakeOrganism.make(null);
        Location oneOne = Location.of(1, 1);
        Location oneTwo = Location.of(1, 2);
        Response response = genome.respond(
                state,
                Set.of(oneTwo),
                Map.of(oneOne, DESIRABLE_SUBSTANCE),
                Map.of(Location.ORIGIN, self, oneOne, neighbor));
        MoverState newState = (MoverState) response.newState();
        assertEquals(
                state.getEnergy() - MOVE_ENERGY,
                newState.getEnergy());
        assertTrue(response.substanceChanges().isEmpty());
        assertEquals(
                List.of(Change.move(self, Location.ORIGIN, oneTwo)),
                response.organismChanges());
    }

    @Test
    void canBeStuck() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Organism neighbor = FakeOrganism.make(null);
        Location oneOne = Location.of(1, 1);
        Response response = genome.respond(
                state,
                Collections.emptySet(),
                Map.of(oneOne, DESIRABLE_SUBSTANCE),
                Map.of(Location.ORIGIN, self, oneOne, neighbor));
        MoverState newState = (MoverState) response.newState();
        assertEquals(
                state.getEnergy() - REST_ENERGY,
                newState.getEnergy());
        assertTrue(response.substanceChanges().isEmpty());
        assertTrue(response.organismChanges().isEmpty());
    }

    @Test
    void canBeExcited() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Location oneOne = Location.of(1, 1);
        Response response = genome.respond(
                state,
                Collections.emptySet(),
                Map.of(oneOne, DESIRABLE_SUBSTANCE),
                Map.of(Location.ORIGIN, self));
        MoverState newState = (MoverState) response.newState();
        assertEquals(
                state.getEnergy() - MOVE_ENERGY,
                newState.getEnergy());
        assertTrue(response.substanceChanges().isEmpty());
        assertEquals(
                List.of(Change.move(self, Location.ORIGIN, oneOne)),
                response.organismChanges());
    }

    @Test
    void canBeFrustrated() {
        MoverGenome genome = make();
        MoverState state = new MoverState(1234);
        Organism self = FakeOrganism.make(null);
        Location oneOne = Location.of(1, 1);
        Response response = genome.respond(
                state,
                Set.of(oneOne),
                Collections.emptyMap(),
                Map.of(Location.ORIGIN, self));
        MoverState newState = (MoverState) response.newState();
        assertEquals(
                state.getEnergy() - MOVE_ENERGY,
                newState.getEnergy());
        assertTrue(response.substanceChanges().isEmpty());
        assertEquals(
                List.of(Change.move(self, Location.ORIGIN, oneOne)),
                response.organismChanges());
    }

}