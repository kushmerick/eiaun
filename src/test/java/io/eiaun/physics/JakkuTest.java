package io.eiaun.physics;

import io.eiaun.fakes.FakeSubstance;
import io.eiaun.fakes.FakeSubstanceFactory;
import io.eiaun.organisms.Genome;
import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.organisms.State;
import io.eiaun.organisms.simple.SimpleOrganism;
import io.eiaun.organisms.simple.SimpleState;
import io.eiaun.snapshot.SnapshotRecorder;
import io.eiaun.util.InfiniteFairIterator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class JakkuTest {

    @Mock
    private Consumer<String> rejectedChangeRecorder;

    @Mock
    private SnapshotRecorder snapshotRecorder;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Organism newSimpleOrganism(Jakku jakku) {
        return new SimpleOrganism(
                jakku,
                Map.of(SimpleOrganism.VISION_RADIUS_PROPERTY, 5d));
    }

    @BeforeEach
    void beforeEach() {
        lenient() // most but not all tests actually rely on this behavior
                .when(this.snapshotRecorder.record(any(Jakku.class), any(Executor.class)))
                .thenReturn(CompletableFuture.completedFuture(1L));
    }

    @Test
    void canConstruct() {
        Jakku jakku = new Jakku(
                1234,
                0.1, this::newSimpleOrganism,
                0.1, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
    }

    @Test
    void canLookForNeighbors() {
        int grid = 1234;
        Jakku jakku = new Jakku(
                1234,
                1, this::newSimpleOrganism,
                1, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
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
                1, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
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
                1, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
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
                0, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
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
                0, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
        int lat = grid / 2;
        int lon = grid / 2;
        // injection a single organism in the middle and a single nearby substance
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = newSimpleOrganism(jakku);
        jakku.setOrganisms(organisms);
        Substance[][] substances = new Substance[grid][grid];
        substances[lat + 1][lon + 1] = new FakeSubstance();
        jakku.setSubstances(substances);
        int radius = 5;
        Set<Location> nearbyEmpties = jakku.lookForEmpties(lat, lon, radius);
        int diameter = 2 * radius + 1;
        assertEquals(diameter * diameter - 1, nearbyEmpties.size());
    }

    @Test
    void canGetStuffWithVisionRadius1() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        // force stepping the organism at (lat, lon)
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = newSimpleOrganism(jakku);
        organisms[lat + 1][lon + 1] = newSimpleOrganism(jakku);
        organisms[lat - 1][lon - 1] = newSimpleOrganism(jakku);
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
                        Location.of(+1, -1),
                        Location.of(0, -1),
                        Location.of(0, +1),
                        Location.of(-1, 0),
                        Location.of(-1, +1)),
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
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, new  FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
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
        organismChanges.put(Location.of(0, -1), newSimpleOrganism(jakku));
        Response response = new Response(newState, substanceChanges, organismChanges);
        Organism[][] organisms = new Organism[grid][grid];
        organisms[lat][lon] = organism;
        organisms[lat + 1][lon + 1] = newSimpleOrganism(jakku);
        organisms[lat - 1][lon - 1] = newSimpleOrganism(jakku);
        jakku.setOrganisms(organisms);
        Substance[][] substances = new Substance[grid][grid];
        substances[lat + 1][lon - 1] = new FakeSubstance();
        substances[lat - 1][lon + 1] = new FakeSubstance();
        jakku.setSubstances(substances);
        when(organism.respond(
                eq(Set.of(
                        Location.of(+1, 0),
                        Location.of(+1, -1),
                        Location.of(0, -1),
                        Location.of(0, +1),
                        Location.of(-1, 0),
                        Location.of(-1, +1))),
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
        verifyNoInteractions(this.rejectedChangeRecorder);
    }

    @Test
    void canMove() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
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
        verifyNoInteractions(this.rejectedChangeRecorder);
    }

    @Test
    void canSnapshot() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
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
        verify(this.snapshotRecorder, times(1))
                .record(eq(jakku), eq(this.executor));
    }

    @Test
    void invalidOrganismChangesAreRejected() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
        Organism organism = mock(Organism.class);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        State newState = new SimpleState();
        Map<Location, Substance> substanceChanges = Collections.emptyMap();
        Map<Location, Organism> organismChanges = Map.of(
                // try to put a child on a neighbor
                Location.of(+1, +1), newSimpleOrganism(jakku));
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
        verify(this.rejectedChangeRecorder, times(1)).accept(anyString());
    }

    @Test
    void removingNonExistentSubstanceAndOrganismAreRejected() {
        int grid = 1234;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        Jakku jakku = new Jakku(
                grid,
                // note zero densities; substances and organisms are injected below
                0, this::newSimpleOrganism,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
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
        verify(this.rejectedChangeRecorder, times(2)).accept(anyString());
    }

    @Test
    public void testWrap() {
        int grid = 1234;
        Jakku jakku = new Jakku(
                grid,
                0, this::newSimpleOrganism,
                0, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
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