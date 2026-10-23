package io.eiaun.shared.physics;

import io.eiaun.shared.fakes.FakeOrganism;
import io.eiaun.shared.fakes.FakeSubstance;
import io.eiaun.shared.fakes.FakeSubstanceFactory;
import io.eiaun.shared.organisms.Genome;
import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.organisms.simple.SimpleState;
import io.eiaun.shared.snapshot.SnapshotRecorder;
import io.eiaun.shared.util.Generic;
import io.eiaun.shared.util.InfiniteFairIterator;
import io.eiaun.shared.util.TwoD;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;
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

    // this is the least horrible way I can think of to allow tests to inject substances and
    // organisms into Jakku, which is something that never happens in production code.
    private static class MonkeyPatchedJakku extends Jakku {

        public MonkeyPatchedJakku(
                int grid,
                double organismDensity,
                Function<Jakku, Organism> organismCreator,
                double substanceDensity,
                SubstanceFactory substanceFactory,
                Function<Collection<Location>, Iterator<Location>> organismLocationsIteratorGenerator,
                Consumer<String> rejectedChangeRecorder,
                SnapshotRecorder snapshotRecorder
        ) {
            super(grid, organismDensity, organismCreator, substanceDensity, substanceFactory, organismLocationsIteratorGenerator, rejectedChangeRecorder, snapshotRecorder);
        }

        public void resetOrganisms(TwoD<Organism> organisms) {
            getOrganisms().clear();
            getOrganismIndex().clear();
            resetThings(
                    organisms,
                    (organism, lat, lon) -> getOrganisms().set(lat, lon, organism),
                    (organism, _, _) -> getOrganismIndex().put(organism.getId(), organism));
        }

        public void resetSubstances(TwoD<Substance> substances) {
            getSubstances().clear();
            resetThings(
                    substances,
                    (substance, lat, lon) -> getSubstances().set(lat, lon, substance));
        }

        @SafeVarargs
        private <Thing> void resetThings(
                TwoD<Thing> things,
                TriConsumer<Thing, Integer, Integer>... resetters
        ) {
            for (int lat: things.firstIndices()) {
                for (int lon: things.secondIndices(lat)) {
                    Thing substance = things.get(lat, lon);
                    for (var resetter : resetters) {
                        resetter.accept(substance, lat, lon);
                    }
                }
            }
        }

    }

    @Mock
    private Consumer<String> rejectedChangeRecorder;

    @Mock
    private SnapshotRecorder snapshotRecorder;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void beforeEach() throws IOException {
        lenient() // most but not all tests actually rely on this behavior
                .when(this.snapshotRecorder.record(any(Jakku.class), eq(Location.ORIGIN), anyList(), anyList()))
                .thenReturn(Pair.of("snapshot-id-123", () -> {}));
    }

    @Test
    void canConstruct() {
        Jakku jakku = new Jakku(
                123,
                0.1, FakeOrganism::make,
                0.1, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
    }

    @Test
    void canLookForNeighbors() {
        int grid = 123;
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
        TwoD<Organism> organisms = new TwoD<>();
        TwoD<Substance> substances = new TwoD<>();
        for (int lat = 0; lat < grid; lat++) {
            for (int lon = 0; lon < grid; lon++) {
                organisms.set(lat, lon, FakeOrganism.make(jakku));
                substances.set(lat, lon, jakku.getSubstanceFactory().make());
            }
        }
        jakku.resetOrganisms(organisms);
        jakku.resetSubstances(substances);
        int radius = 5;
        Map<Location, Organism> neighbors = jakku.lookForNeighbors(grid / 2, grid / 2, radius);
        int diameter = 2 * radius + 1;
        assertEquals(diameter * diameter, neighbors.size());
    }

    @Test
    void canLookForSubstances() {
        int grid = 123;
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
        TwoD<Organism> organisms = new TwoD<>();
        TwoD<Substance> substances = new TwoD<>();
        for (int lat = 0; lat < grid; lat++) {
            for (int lon = 0; lon < grid; lon++) {
                organisms.set(lat, lon, FakeOrganism.make(jakku));
                substances.set(lat, lon, jakku.getSubstanceFactory().make());
            }
        }
        jakku.resetOrganisms(organisms);
        jakku.resetSubstances(substances);
        int radius = 5;
        Map<Location, Substance> nearbySubstances = jakku.lookForSubstances(grid / 2, grid / 2, radius);
        int diameter = 2 * radius + 1;
        assertEquals(diameter * diameter, nearbySubstances.size());
    }

    @Test
    void canLookForEmptiesWhenFull() {
        int grid = 123;
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
        TwoD<Organism> organisms = new TwoD<>();
        TwoD<Substance> substances = new TwoD<>();
        for (int lat = 0; lat < grid; lat++) {
            for (int lon = 0; lon < grid; lon++) {
                organisms.set(lat, lon, FakeOrganism.make(jakku));
                substances.set(lat, lon, jakku.getSubstanceFactory().make());
            }
        }
        jakku.resetOrganisms(organisms);
        jakku.resetSubstances(substances);
        int radius = 5;
        Set<Location> nearbyEmpties = jakku.lookForEmpties(grid / 2, grid / 2, radius);
        assertTrue(nearbyEmpties.isEmpty());
    }

    @Test
    void canLookForEmptiesWhenEmpty() {
        int grid = 123;
        Jakku jakku = new Jakku(
                grid,
                0, FakeOrganism::make,
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
        int grid = 123;
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                InfiniteFairIterator::of,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
        int lat = grid / 2;
        int lon = grid / 2;
        // injection a single organism in the middle and a single nearby substance
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, FakeOrganism.make(jakku));
        jakku.resetOrganisms(organisms);
        TwoD<Substance> substances = new TwoD<>();
        substances.set(lat + 1, lon + 1, new FakeSubstance());
        jakku.resetSubstances(substances);
        int radius = 5;
        Set<Location> nearbyEmpties = jakku.lookForEmpties(lat, lon, radius);
        int diameter = 2 * radius + 1;
        assertEquals(diameter * diameter - 1, nearbyEmpties.size());
    }

    @Test
    void canGetStuffWithVisionRadius1() {
        int grid = 123;
        int lat = grid / 2;
        int lon = grid / 2;
        // force stepping the organism at (lat, lon)
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                log::info,
                this.snapshotRecorder);
        jakku.initialize();
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, FakeOrganism.make(jakku));
        organisms.set(lat + 1, lon + 1, FakeOrganism.make(jakku));
        organisms.set(lat - 1, lon - 1, FakeOrganism.make(jakku));
        jakku.resetOrganisms(organisms);
        TwoD<Substance> substances = new TwoD<>();
        substances.set(lat + 1, lon - 1, new FakeSubstance());
        substances.set(lat - 1, lon + 1, new FakeSubstance());
        jakku.resetSubstances(substances);
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
                        Location.of(+1, -1), jakku.getSubstances().get(lat + 1, lon - 1),
                        Location.of(-1, +1), jakku.getSubstances().get(lat - 1, lon + 1)),
                nearbySubstances);
        Map<Location, Organism> neighbors = jakku.lookForNeighbors(lat, lon, radius);
        assertEquals(
                Map.of(
                        Location.ORIGIN, jakku.getOrganisms().get(lat, lon),
                        Location.of(+1, +1), jakku.getOrganisms().get(lat + 1, lon + 1),
                        Location.of(-1, -1), jakku.getOrganisms().get(lat - 1, lon - 1)),
                neighbors);
    }

    @Test
    void canStep() {
        int grid = 123;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
        Organism organism = mock(Organism.class);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, organism);
        organisms.set(lat + 1, lon + 1, FakeOrganism.make(jakku));
        organisms.set(lat - 1, lon - 1, FakeOrganism.make(jakku));
        jakku.resetOrganisms(organisms);
        TwoD<Substance> substances = new TwoD<>();
        substances.set(lat + 1, lon - 1, new FakeSubstance());
        substances.set(lat - 1, lon + 1, new FakeSubstance());
        jakku.resetSubstances(substances);
        State newState = new SimpleState();
        List<Change<String,Substance>> substanceChanges = List.of(
                Change.destroy(substances.get(lat + 1, lon - 1).getId(), Location.of(+1, -1)));
        Organism baby = FakeOrganism.make(jakku);
        List<Change<Long,Organism>> organismChanges = List.of(
                Change.destroy(organisms.get(lat + 1, lon + 1).getId(), Location.of(+1, +1)),
                Change.create(baby, Location.of(0, -1)),
                Change.update(Location.ORIGIN, Generic.encode(newState, State.STATE_KINDS)));
        Changes changes = new Changes(substanceChanges, organismChanges);
        when(organism.respond(
                eq(jakku),
                eq(Set.of(
                        Location.of(+1, 0),
                        Location.of(+1, -1),
                        Location.of(0, -1),
                        Location.of(0, +1),
                        Location.of(-1, 0),
                        Location.of(-1, +1))),
                eq(Map.of(
                        Location.of(+1, -1), substances.get(lat + 1, lon - 1),
                        Location.of(-1, +1), substances.get(lat - 1, lon + 1))),
                eq(Map.of(
                        Location.ORIGIN, organisms.get(lat, lon),
                        Location.of(+1, +1), organisms.get(lat + 1, lon + 1),
                        Location.of(-1, -1), organisms.get(lat - 1, lon - 1)))))
                .thenReturn(changes);
        jakku.step(this.executor).join();
        verify(organism).setState(any(SimpleState.class));
        assertNull(jakku.getSubstances().get(lat + 1, lon - 1));
        assertNull(jakku.getOrganisms().get(lat + 1, lon + 1));
        assertEquals(organisms.get(lat - 1, lon - 1), jakku.getOrganisms().get(lat - 1, lon - 1));
        assertEquals(baby, jakku.getOrganisms().get(lat, lon - 1));
        verifyNoInteractions(this.rejectedChangeRecorder);
    }

    @Test
    void canMove() {
        int grid = 123;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
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
        List<Change<String,Substance>> substanceChanges = Collections.emptyList();
        Location oneOne = Location.of(+1, +1);
        List<Change<Long,Organism>> organismChanges = List.of(
                Change.move(organism.getId(), Location.ORIGIN, oneOne),
                Change.update(oneOne, Generic.encode(newState, State.STATE_KINDS)));
        Changes changes = new Changes(substanceChanges, organismChanges);
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, organism);
        jakku.resetOrganisms(organisms);
        when(organism.respond(
                eq(jakku),
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
                eq(Map.of(
                        Location.ORIGIN, organisms.get(lat, lon)))))
                .thenReturn(changes);
        jakku.step(this.executor).join();
        assertNull(jakku.getOrganisms().get(lat, lon));
        assertEquals(jakku.getOrganisms().get(lat + 1, lon + 1), organism);
        verify(organism).setState(any(SimpleState.class)); // SimpleStates are indistinguishable, so we can't check for newState
        verifyNoInteractions(this.rejectedChangeRecorder);
    }

    @Test
    void canCreate() {
        int grid = 123;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
        Organism organism = mock(Organism.class);
        when(organism.getId()).thenReturn(1234L);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        State newState = new SimpleState();
        List<Change<String,Substance>> substanceChanges = Collections.emptyList();
        Organism baby = FakeOrganism.make(jakku);
        List<Change<Long,Organism>> organismChanges = List.of(
                Change.create(baby, Location.of(+1, +1)),
                Change.update(Location.ORIGIN, Generic.encode(newState, State.STATE_KINDS)));
        Changes changes = new Changes(substanceChanges, organismChanges);
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, organism);
        jakku.resetOrganisms(organisms);
        when(organism.respond(
                eq(jakku),
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
                eq(Map.of(
                        Location.ORIGIN, organisms.get(lat, lon)))))
                .thenReturn(changes);
        jakku.step(this.executor).join();
        assertEquals(organism, jakku.getOrganisms().get(lat, lon));
        verify(organism).setState(any(SimpleState.class)); // SimpleStates are indistinguishable, so we can't check for newState
        assertEquals(baby, jakku.getOrganisms().get(lat + 1, lon + 1));
        verifyNoInteractions(this.rejectedChangeRecorder);
    }

    @Test
    void canUpdate() {
        int grid = 123;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
        Organism organism = mock(Organism.class);
        when(organism.getId()).thenReturn(1234L);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        State newState = new SimpleState();
        List<Change<String,Substance>> substanceChanges = Collections.emptyList();
        List<Change<Long,Organism>> organismChanges = List.of(
                Change.update(Location.ORIGIN, Generic.encode(newState, State.STATE_KINDS)));
        Changes changes = new Changes(substanceChanges, organismChanges);
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, organism);
        jakku.resetOrganisms(organisms);
        when(organism.respond(
                eq(jakku),
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
                eq(Map.of(
                        Location.ORIGIN, organisms.get(lat, lon)))))
                .thenReturn(changes);
        jakku.step(this.executor).join();
        assertEquals(organism, jakku.getOrganisms().get(lat, lon));
        verify(organism).setState(any(SimpleState.class));
        verifyNoInteractions(this.rejectedChangeRecorder);
    }

    @Test
    void canSnapshot() throws IOException {
        int grid = 123;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
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
        List<Change<String,Substance>> substanceChanges = Collections.emptyList();
        List<Change<Long,Organism>> organismChanges = List.of(
                Change.move(organism.getId(), Location.ORIGIN, Location.of(+1, +1)),
                Change.update(Location.ORIGIN, Generic.encode(newState, State.STATE_KINDS)));
        Changes changes = new Changes(substanceChanges, organismChanges);
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, organism);
        jakku.resetOrganisms(organisms);
        when(organism.respond(
                eq(jakku),
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
                eq(Map.of(
                        Location.ORIGIN, organisms.get(lat, lon)))))
                .thenReturn(changes);
        jakku.step(this.executor).join();
        verify(this.snapshotRecorder, times(1))
                .record(eq(jakku), eq(Location.of(lat, lon)), eq(organismChanges), eq(substanceChanges));
    }

    @Test
    void invalidOrganismChangesAreRejected() {
        int grid = 123;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
                0, new FakeSubstanceFactory(),
                organismLocationIteratorGenerator,
                this.rejectedChangeRecorder,
                this.snapshotRecorder);
        jakku.initialize();
        Organism organism = mock(Organism.class);
        Genome genome = mock(Genome.class);
        when(genome.getVisionRadius()).thenReturn(1d);
        when(organism.getGenome()).thenReturn(genome);
        List<Change<String,Substance>> substanceChanges = Collections.emptyList();
        List<Change<Long,Organism>> organismChanges = List.of(
                // try to put a child on a neighbor
                Change.create(organism, Location.of(+1, +1)));
        Changes changes = new Changes(substanceChanges, organismChanges);
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, organism);
        organisms.set(lat + 1, lon + 1, organism);
        jakku.resetOrganisms(organisms);
        when(organism.respond(
                eq(jakku),
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
                        Location.ORIGIN, organisms.get(lat, lon),
                        Location.of(+1, +1), organisms.get(lat + 1, lon + 1)))))
                .thenReturn(changes);
        jakku.step(this.executor).join();
        verify(this.rejectedChangeRecorder, times(1)).accept(anyString());
    }

    @Test
    void removingNonExistentSubstanceAndOrganismAreRejected() {
        int grid = 123;
        int lat = grid / 2;
        int lon = grid / 2;
        Function<Collection<Location>, Iterator<Location>> organismLocationIteratorGenerator = _ ->
                InfiniteFairIterator.of(List.of(Location.of(lat, lon)));
        MonkeyPatchedJakku jakku = new MonkeyPatchedJakku(
                grid,
                0, FakeOrganism::make,
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
        List<Change<String,Substance>> substanceChanges = List.of(
                // consume a non-existent substance
                Change.destroy(new FakeSubstance().getId(), Location.of(-1, -1)));
        List<Change<Long,Organism>> organismChanges = List.of(
                // eat a non-existent neighbor
                Change.destroy(FakeOrganism.make(jakku).getId(), Location.of(+1, +1)));
        Changes changes = new Changes(substanceChanges, organismChanges);
        TwoD<Organism> organisms = new TwoD<>();
        organisms.set(lat, lon, organism);
        jakku.resetOrganisms(organisms);
        when(organism.respond(
                eq(jakku),
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
                eq(Map.of(
                        Location.ORIGIN, organisms.get(lat, lon)))))
                .thenReturn(changes);
        jakku.step(this.executor).join();
        verify(this.rejectedChangeRecorder, times(2)).accept(anyString());
    }

    @Test
    public void testWrap() {
        int grid = 123;
        Jakku jakku = new Jakku(
                grid,
                0, FakeOrganism::make,
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
        assertEquals(0, jakku.wrap(-99 * grid));
        assertEquals(grid - 1, jakku.wrap(-99 * grid - 1));
        assertEquals(grid - 5, jakku.wrap(-99 * grid - 5));
        assertEquals(5, jakku.wrap(-99 * grid + 5));
        assertEquals(grid - 1, jakku.wrap(grid - 1));
        assertEquals(0, jakku.wrap(grid));
        assertEquals(1, jakku.wrap(grid + 1));
        assertEquals(0, jakku.wrap(99 * grid));
        assertEquals(1, jakku.wrap(99 * grid + 1));
    }

}