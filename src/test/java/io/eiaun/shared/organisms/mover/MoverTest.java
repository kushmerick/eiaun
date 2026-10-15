package io.eiaun.shared.organisms.mover;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.util.Generic;
import io.eiaun.shared.util.JSON;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.eiaun.shared.organisms.mover.Mover.*;
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
    void canPickASingleProperty() {
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
                        DESIRABLE_PROPERTIES_PROPERTY, 1d);
        when(jakku.getAllSubstanceProperties()).thenReturn(substanceProperties);
        Mover mover = new Mover(jakku, organismProperties);
        Set<Map<String, String>> possibilities = substanceProperties.entrySet().stream()
                .map(e -> Map.of(e.getKey(), e.getValue().iterator().next()))
                .collect(Collectors.toSet());
        Map<String, String> selected = mover.getGenome().getDesirableSubstanceProperties();
        assertTrue(possibilities.contains(selected));
    }

    @Test
    void canPickASingleValue() {
        Jakku jakku = mock(Jakku.class);
        Map<String, Set<String>> substanceProperties = Map.of(
                "P1", Set.of("V1", "V2"));
        Map<String, Double> organismProperties =
                Map.of(VISION_RADIUS_PROPERTY, 10d,
                        PEAK_ENERGY_PROPERTY, 20d,
                        MOVE_ENERGY_PROPERTY, 30d,
                        REST_ENERGY_PROPERTY, 40d,
                        DESIRABLE_PROPERTIES_PROPERTY, 1d);
        when(jakku.getAllSubstanceProperties()).thenReturn(substanceProperties);
        Mover mover = new Mover(jakku, organismProperties);
        Set<Map<String, String>> possibilities = Set.of(
                Map.of("P1", "V1"),
                Map.of("P1", "V2"));
        Map<String, String> selected = mover.getGenome().getDesirableSubstanceProperties();
        assertTrue(possibilities.contains(selected));
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

    @Test
    void canEncodeWithGeneric() {
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
        int id = Organism.ID.intValue();
        Mover mover = new Mover(jakku, organismProperties);
        Generic generic = Generic.encode(mover, Organism.ORGANISM_KINDS);
        String encoded = JSON.toJson(generic);
        assertEquals(
                """
                {"k":"M","j":"{\\"id\\":""" + id + """
                ,\\"state\\":{\\"energy\\":20.0},\\"genome\\":{\\"desirableSubstanceProperties\\":{\\"P1\\":\\"V1\\",\\"P2\\":\\"V2\\",\\"P3\\":\\"V3\\"},\\"visionRadius\\":10.0,\\"peakEnergy\\":20.0,\\"moveEnergy\\":30.0,\\"restEnergy\\":40.0}}"}
                """.strip(),
                encoded);
    }

    @Test
    void canDecodeWithGeneric() {
        Generic generic = new Generic(
                "M",
                """
                {"id":123,"state":{"energy":20.0},"genome":{"desirableSubstanceProperties":{"P1":"V1","P2":"V2","P3":"V3"},"visionRadius":10.0,"peakEnergy":20.0,"moveEnergy":30.0,"restEnergy":40.0}}
                """.strip());
        Mover mover = generic.decode(Mover.KIND_ORGANISMS);
        assertEquals(123, mover.getId());
        assertEquals(20, mover.getState().getEnergy());
        assertEquals(40, mover.getGenome().getRestEnergy());
    }

}