package io.eiaun.physics;

import io.eiaun.concepts.ecosystem.*;
import io.eiaun.fakes.FakeSubstance;
import io.eiaun.implementations.simple.SimpleOrganism;
import io.eiaun.implementations.simple.SimpleState;
import io.eiaun.util.InfiniteFairIterator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
class JakkuTest {

    private static class RejectedChangeCounter implements Consumer<String> {

        @Getter
        private int rejections = 0;

        @Override
        public void accept(String ignored) {
            rejections++;
        }

    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Organism newSimpleOrganism() {
        return new SimpleOrganism(Map.of(SimpleOrganism.VISION_RADIUS_PROPERTY, 5d));
    }

    @Test
    void canConstruct() {
        Jakku jakku = new Jakku(
                1234,
                0.1, this::newSimpleOrganism,
                0.1, FakeSubstance::new,
                InfiniteFairIterator::of,
                log::info);
    }

    @Test
    void canLookForNeighbors() {
        int grid = 1234;
        Jakku jakku = new Jakku(
                1234,
                1, this::newSimpleOrganism,
                1, FakeSubstance::new,
                InfiniteFairIterator::of,
                log::info);
        int radius = 5;
        Map<Location, Organism> neighbors = jakku.lookForNeighbors(grid / 2, grid / 2, radius);
        int diameter = 2 * radius + 1;
        assertEquals(diameter * diameter - 1, neighbors.size());
    }

    @Test
    void canLookForSubstances() {
        int grid = 1234;
        Jakku jakku = new Jakku(
                1234,
                1, this::newSimpleOrganism,
                1, FakeSubstance::new,
                InfiniteFairIterator::of,
                log::info);
        int radius = 5;
        Map<Location, Substance> nearbySubstances = jakku.lookForSubstances(grid / 2, grid / 2, radius);
        int diameter = 2 * radius + 1;
        assertEquals(diameter * diameter, nearbySubstances.size());
    }

    @Test
    void canLookForEmptiesWhenFull() {
        int grid = 1234;
        Jakku jakku = new Jakku(
                1234,
                1, this::newSimpleOrganism,
                1, FakeSubstance::new,
                InfiniteFairIterator::of,
                log::info);
        int radius = 5;
        Set<Location> nearbyEmpties = jakku.lookForEmpties(grid / 2, grid / 2, radius);
        assertTrue(nearbyEmpties.isEmpty());
    }

    @Test
    void canLookForEmptiesWhenEmpty() {
        int grid = 1234;
        Jakku jakku = new Jakku(
                1234,
                0, this::newSimpleOrganism,
                0, FakeSubstance::new,
                InfiniteFairIterator::of,
                log::info);
        int radius = 5;
        Set<Location> nearbyEmpties = jakku.lookForEmpties(grid / 2, grid / 2, radius);
        int diameter = 2 * radius + 1;
        assertEquals(diameter * diameter, nearbyEmpties.size());
    }

    @Test
    void canLookForEmptiesWhenIsolated() {
        int grid = 1234;
        Jakku jakku = new Jakku(
                1234,
                0, this::newSimpleOrganism,
                0, FakeSubstance::new,
                InfiniteFairIterator::of,
                log::info);
        int lat = grid / 2;
        int lon = grid / 2;
        // injection a single organism in the middle and a single nearby substance
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = newSimpleOrganism();
        jakku.setOrganisms(organisms);
        Substance[][] substances = new Substance[grid][grid];
        substances[lat + 1][lon + 1] = new FakeSubstance();
        jakku.setSubstances(substances);
        int radius = 5;
        Set<Location> nearbyEmpties = jakku.lookForEmpties(lat, lon, radius);
        int diameter = 2 * radius + 1;
        assertEquals(diameter * diameter - 2, nearbyEmpties.size());
    }

    @Test
    void canGetStuffWithVisionRadius1() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, FakeSubstance::new,
                organismLocationIteratorGenerator,
                log::info);
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = newSimpleOrganism();
        organisms[lat + 1][lon + 1] = newSimpleOrganism();
        organisms[lat - 1][lon - 1] = newSimpleOrganism();
        jakku.setOrganisms(organisms);
        Substance[][] substances = new Substance[grid][grid];
        substances[lat + 1][lon - 1] = new FakeSubstance();
        substances[lat - 1][lon + 1] = new FakeSubstance();
        jakku.setSubstances(substances);
        double radius = 1;
        Set<Location> nearbyEmpties = jakku.lookForEmpties(lat, lon, radius);
        assertEquals(
                Set.of(
                        Location.of(+1, 0),
                        Location.of(0, -1),
                        Location.of(0, +1),
                        Location.of(-1, 0)),
                nearbyEmpties);
        Map<Location, Substance> nearbySubstances = jakku.lookForSubstances(lat, lon, radius);
        assertEquals(
                Map.of(
                        Location.of(+1, -1), substances[lat + 1][lon - 1],
                        Location.of(-1, +1), substances[lat - 1][lon + 1]),
                nearbySubstances);
        Map<Location, Organism> neighbors = jakku.lookForNeighbors(lat, lon, radius);
        assertEquals(
                Map.of(
                        Location.of(+1, +1), organisms[lat + 1][lon + 1],
                        Location.of(-1, -1), organisms[lat - 1][lon - 1]),
                neighbors);
    }

    @Test
    void canStep() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        RejectedChangeCounter rejectedChangeCounter = new RejectedChangeCounter();
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, FakeSubstance::new,
                organismLocationIteratorGenerator,
                rejectedChangeCounter);
        Organism organism = mock(Organism.class);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        State newState = new SimpleState();
        Map<Location, Substance> substanceChanges = new HashMap<>();
        substanceChanges.put(Location.of(+1, -1), null);
        substanceChanges.put(Location.of(-1, +1), new FakeSubstance());
        Map<Location, Organism> organismChanges = new HashMap<>();
        organismChanges.put(Location.of(+1, +1), null);
        organismChanges.put(Location.of(0, -1), newSimpleOrganism());
        Response response = new Response(newState, substanceChanges, organismChanges);
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = organism;
        organisms[lat + 1][lon + 1] = newSimpleOrganism();
        organisms[lat - 1][lon - 1] = newSimpleOrganism();
        jakku.setOrganisms(organisms);
        Substance[][] substances = new Substance[grid][grid];
        substances[lat + 1][lon - 1] = new FakeSubstance();
        substances[lat - 1][lon + 1] = new FakeSubstance();
        jakku.setSubstances(substances);
        when(organism.respond(
                eq(Set.of(
                        Location.of(+1, 0),
                        Location.of(0, -1),
                        Location.of(0, +1),
                        Location.of(-1, 0))),
                eq(Map.of(
                        Location.of(+1, -1), substances[lat + 1][lon - 1],
                        Location.of(-1, +1), substances[lat - 1][lon + 1])),
                eq(Map.of(
                        Location.of(+1, +1), organisms[lat + 1][lon + 1],
                        Location.of(-1, -1), organisms[lat - 1][lon - 1]))))
                .thenReturn(response);
        jakku.step(this.executor).join();
        verify(organism).setState(eq(newState));
        assertNull(substances[lat + 1][lon - 1]);
        assertEquals(substances[lat - 1][lon + 1], substanceChanges.get(Location.of(-1, +1)));
        assertNull(organisms[lat + 1][lon + 1]);
        assertNotNull(organisms[lat - 1][lon - 1]);
        assertEquals(organisms[lat][lon - 1], organismChanges.get(Location.of(0, -1)));
        assertEquals(0, rejectedChangeCounter.getRejections());
    }

    @Test
    void canMove() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        RejectedChangeCounter rejectedChangeCounter = new RejectedChangeCounter();
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, FakeSubstance::new,
                organismLocationIteratorGenerator,
                rejectedChangeCounter);
        Organism organism = mock(Organism.class);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        State newState = new SimpleState();
        Map<Location, Substance> substanceChanges = Collections.emptyMap();
        Map<Location, Organism> organismChanges = new HashMap<>();
        organismChanges.put(Location.of(+1, +1), organism);
        organismChanges.put(Location.of(0, 0), null);
        Response response = new Response(newState, substanceChanges, organismChanges);
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = organism;
        jakku.setOrganisms(organisms);
        when(organism.respond(
                eq(Set.of(
                        Location.of(+1, +1),
                        Location.of(+1, 0),
                        Location.of(+1, -1),
                        Location.of(0, +1),
                        Location.of(0, -1),
                        Location.of(-1, +1),
                        Location.of(-1, 0),
                        Location.of(-1, -1))),
                eq(Collections.emptyMap()),
                eq(Collections.emptyMap())))
                .thenReturn(response);
        jakku.step(this.executor).join();
        assertNull(organisms[lat][lon]);
        assertEquals(organisms[lat + 1][lon + 1], organism);
        assertEquals(0, rejectedChangeCounter.getRejections());
    }

    @Test
    void invalidOrganismChangesAreRejected() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        RejectedChangeCounter rejectedChangeCounter = new RejectedChangeCounter();
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, FakeSubstance::new,
                organismLocationIteratorGenerator,
                rejectedChangeCounter);
        Organism organism = mock(Organism.class);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        State newState = new SimpleState();
        Map<Location, Substance> substanceChanges = Collections.emptyMap();
        Map<Location, Organism> organismChanges = Map.of(
                // try to put a child on a neighbor
                Location.of(+1, +1), newSimpleOrganism());
        Response response = new Response(newState, substanceChanges, organismChanges);
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = organism;
        organisms[lat + 1][lon + 1] = organism;
        jakku.setOrganisms(organisms);
        when(organism.respond(
                eq(Set.of(
                        Location.of(+1, 0),
                        Location.of(+1, -1),
                        Location.of(0, +1),
                        Location.of(0, -1),
                        Location.of(-1, +1),
                        Location.of(-1, 0),
                        Location.of(-1, -1))),
                eq(Collections.emptyMap()),
                eq(Map.of(
                        Location.of(+1, +1), organisms[lat + 1][lon + 1]))))
                .thenReturn(response);
        jakku.step(this.executor).join();
        assertEquals(1, rejectedChangeCounter.getRejections());
    }

    @Test
    void removingNonExistentSubstanceAndOrganismAreRejected() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        RejectedChangeCounter rejectedChangeCounter = new RejectedChangeCounter();
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, FakeSubstance::new,
                organismLocationIteratorGenerator,
                rejectedChangeCounter);
        Organism organism = mock(Organism.class);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        State newState = new SimpleState();
        Map<Location, Substance> substanceChanges = new HashMap<>();
        // consume a non-existent substance
        substanceChanges.put(Location.of(-1, -1), null);
        Map<Location, Organism> organismChanges = new HashMap<>();
        // eat a non-existent neighbor
        organismChanges.put(Location.of(+1, +1), null);
        Response response = new Response(newState, substanceChanges, organismChanges);
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = organism;
        jakku.setOrganisms(organisms);
        when(organism.respond(
                eq(Set.of(
                        Location.of(+1, +1),
                        Location.of(+1, 0),
                        Location.of(+1, -1),
                        Location.of(0, +1),
                        Location.of(0, -1),
                        Location.of(-1, +1),
                        Location.of(-1, 0),
                        Location.of(-1, -1))),
                eq(Collections.emptyMap()),
                eq(Collections.emptyMap())))
                .thenReturn(response);
        jakku.step(this.executor).join();
        assertEquals(2, rejectedChangeCounter.getRejections());
    }

    @Test
    public void testWrap() {
        int grid = 1234;
        Jakku jakku = new Jakku(
                grid,
                0, this::newSimpleOrganism,
                0, FakeSubstance::new,
                InfiniteFairIterator::of,
                log::info);
        assertEquals(grid - 2, jakku.wrap(-2));
        assertEquals(grid - 1, jakku.wrap(-1));
        assertEquals(0, jakku.wrap(0));
        assertEquals(1, jakku.wrap(1));
        assertEquals(2, jakku.wrap(2));
        assertEquals(0, jakku.wrap(-grid));
        assertEquals(0, jakku.wrap(-99*grid));
        assertEquals(grid - 1, jakku.wrap(-99*grid - 1));
        assertEquals(grid - 5, jakku.wrap(-99*grid - 5));
        assertEquals(5, jakku.wrap(-99*grid + 5));
        assertEquals(grid - 1, jakku.wrap(grid - 1));
        assertEquals(0, jakku.wrap(grid));
        assertEquals(1, jakku.wrap(grid + 1));
        assertEquals(0, jakku.wrap(99 * grid));
        assertEquals(1, jakku.wrap(99 * grid + 1));
    }

}