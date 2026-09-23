package io.eiaun.organisms.mover;

import io.eiaun.organisms.State;
import io.eiaun.physics.Jakku;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.eiaun.organisms.mover.Mover.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoverTest {

    @Test
    void canCreateGenomeAndState() {
        Jakku jakku = mock(Jakku.class);
        Map<String, Set<String>> substanceProperties = Map.of(
                "P1", Set.of("V1"),
                "P2", Set.of("V2"),
                "P3", Set.of("V3"));
        double peakEnergy = 20;
        Map<String, Double> organismProperties =
                Map.of(VISION_RADIUS_PROPERTY, 10d,
                        PEAK_ENERGY_PROPERTY, peakEnergy,
                        MOVE_ENERGY_PROPERTY, 30d,
                        REST_ENERGY_PROPERTY, 40d,
                        DESIRABLE_PROPERTIES_PROPERTY, (double) substanceProperties.size());
        when(jakku.getAllSubstanceProperties()).thenReturn(substanceProperties);
        Mover mover = new Mover(jakku, organismProperties);
        assertEquals(
                peakEnergy,
                mover.getState().getEnergy());
        assertEquals(
                peakEnergy,
                mover.getGenome().getPeakEnergy());
        assertEquals(
                organismProperties.get(MOVE_ENERGY_PROPERTY),
                mover.getGenome().getMoveEnergy());
        assertEquals(
                organismProperties.get(REST_ENERGY_PROPERTY),
                mover.getGenome().getRestEnergy());
        assertEquals(
                substanceProperties.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        s -> s.getValue().iterator().next())),
                mover.getGenome().getDesirableSubstanceProperties());
    }

    @Test
    void rejectsTooManyProperties() {
        Jakku jakku = mock(Jakku.class);
        Map<String, Set<String>> substanceProperties = Map.of(
                "P1", Set.of("V1"),
                "P2", Set.of("V2"),
                "P3", Set.of("V3"));
        Map<String, Double> organismProperties =
                Map.of(VISION_RADIUS_PROPERTY, 10d,
                        PEAK_ENERGY_PROPERTY, 20d,
                        MOVE_ENERGY_PROPERTY, 30d,
                        REST_ENERGY_PROPERTY, 40d,
                        DESIRABLE_PROPERTIES_PROPERTY, 2d * substanceProperties.size());
        when(jakku.getAllSubstanceProperties()).thenReturn(substanceProperties);
        assertThrows(RuntimeException.class, () -> new Mover(jakku, organismProperties));
    }

    @Test
    void canSetState() {
        Jakku jakku = mock(Jakku.class);
        Map<String, Set<String>> substanceProperties = Map.of(
                "P1", Set.of("V1"),
                "P2", Set.of("V2"),
                "P3", Set.of("V3"));
        Map<String, Double> organismProperties =
                Map.of(VISION_RADIUS_PROPERTY, 10d,
                        PEAK_ENERGY_PROPERTY, 20d,
                        MOVE_ENERGY_PROPERTY, 30d,
                        REST_ENERGY_PROPERTY, 40d,
                        DESIRABLE_PROPERTIES_PROPERTY, (double) substanceProperties.size());
        when(jakku.getAllSubstanceProperties()).thenReturn(substanceProperties);
        Mover mover = new Mover(jakku, organismProperties);
        State newState = new MoverState(1234);
        assertNotEquals(newState, mover.getState());
        mover.setState(newState);
        assertEquals(newState, mover.getState());
    }

}