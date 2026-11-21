package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.mover.MoverState;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.physics.SubstanceFactory;
import io.eiaun.shared.util.TwoD;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import static io.eiaun.shared.organisms.State.KIND_STATES;
import static io.eiaun.shared.snapshot.FileSnapshots.COMPRESSION;
import static io.eiaun.shared.testresources.TestRecordings.RECORDING;
import static io.eiaun.shared.util.Functionals.noop;
import static io.eiaun.shared.util.Functionals.rnoop;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class FileSnapshotsTest {

    private Snapshots snapshots;

    @BeforeEach
    void setUp() {
        this.snapshots = FileSnapshots.of(RECORDING);
    }

    @SuppressWarnings("unchecked")
    private <Thing> BiConsumer<Location,Thing> mockcb() {
        return (BiConsumer<Location, Thing>) mock(BiConsumer.class);
    }

    @Test
    void canGetRecordingTimestamp() {
        assertEquals(1793653159329L, snapshots.getRecordingTimestamp());
    }

    @Test
    void canGetPhysics() {
        assertEquals(30, this.snapshots.getPhysics().getSubstanceSpecs().size());
    }

    @Test
    void canGetConfig() {
        assertEquals(0.4, this.snapshots.getConfig().getOrganismDensity());
    }

    @Test
    void canGetId() {
        this.snapshots.bump(1, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(0, this.snapshots.getId());
        this.snapshots.bump(1, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(1, this.snapshots.getId());
    }

    @Test
    void canNext() {
        this.snapshots.bump(1, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(0, this.snapshots.getId());
    }

    @Test
    void canBump() {
        this.snapshots.bump(5, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(4, this.snapshots.getId());
    }

    @Test
    void canSnap() {
        this.snapshots.snap(10, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(10, this.snapshots.getId());
    }

    @Test
    void canSnapSeveral() {
        this.snapshots.snap(10, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(10, this.snapshots.getId());
        this.snapshots.snap(99, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(99, this.snapshots.getId());
        this.snapshots.snap(5, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(5, this.snapshots.getId());
        this.snapshots.snap(105, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(105, this.snapshots.getId());
    }

    @Test
    public void snappingBefore1stDumpReloadsPreambleDump() {
        SubstanceFactory substanceFactory = new SubstanceFactory(
                FileSnapshots.of(RECORDING).getPhysics().getSubstanceSpecs().values());
        List<Path> mgoPaths = new ArrayList<>();
        List<Path> mgsPaths = new ArrayList<>();
        FullDumpReader fullDumpReader = new FullDumpReader(COMPRESSION, substanceFactory) {
            @Override public Optional<TwoD<Organism>> maybeGetOrganisms(Path path) {
                mgoPaths.add(path);
                return super.maybeGetOrganisms(path);
            }
            @Override public Optional<TwoD<Substance>> maybeGetSubstances(Path path) {
                mgsPaths.add(path);
                return super.maybeGetSubstances(path);
            }
        };
        Snapshots snapshots = new FileSnapshots(RECORDING, _ -> fullDumpReader);
        snapshots.snap(1, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(mgoPaths, List.of(RECORDING));
        assertEquals(mgoPaths, mgsPaths);
        snapshots.snap(0, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(mgoPaths, List.of(RECORDING, RECORDING));
        assertEquals(mgoPaths, mgsPaths);
    }


    @Test
    void canNotGetIdBeforeLoadingFirstSnapshot() {
        assertThrows(IllegalStateException.class, () -> this.snapshots.getId());
    }

    @Test
    void canGetCount() {
        assertEquals(158, this.snapshots.getCount());
    }

    @Test
    void canNextAndGetOrganismChanges() {
        // $ F=$(grep -L -F '[]' src/test/resources/test-recordings/*/*/*/snapshots/*/*/*/*/*/organism-changes.json | sort | head -1); echo "$F"; cat "$F"
        // src/test/resources/test-recordings/2026-11-02-20/2026-11-02-20-59/2026-11-02-20-59-19-329/snapshots/000/000000/000000000/000000000000/000000000000000/organism-changes.json
        // [{"verb":"MOVE","original":42777,"replacement":42777,"from":{"lat":270,"lon":396},"to":{"lat":269,"lon":395}},{"verb":"UPDATE","to":{"lat":269,"lon":395},"state":{"k":"M","j":"{\"e\":4.0}"}}]
        this.snapshots.bump(1, rnoop(), noop(), noop(), noop(), noop());
        List<Change<Long, Organism>> changes = this.snapshots.getOrganismChanges();
        assertEquals(2, changes.size());
        assertEquals(
                Change.<Long, Organism>move(42777L, Location.of(270, 396), Location.of(269, 395)),
                changes.getFirst());
        assertEquals(
                new MoverState(4),
                changes.get(1).getState().decode(KIND_STATES));
    }

    @Test
    void canBumpAndGetSubstanceChanges() {
        // $ F=$(grep -L -F '[]' src/test/resources/test-recordings/*/*/*/snapshots/*/*/*/*/*/substance-changes.json | sort | head -1); echo "$F"; cat "$F"
        // src/test/resources/test-recordings/2026-11-02-20/2026-11-02-20-59/2026-11-02-20-59-19-329/snapshots/000/000000/000000000/000000000000/000000000000004/substance-changes.json
        // [{"verb":"DESTROY","original":"Oxygen","from":{"lat":89,"lon":399}},{"verb":"CREATE","to":{"lat":89,"lon":399},"baby":"Potassium"}]
        this.snapshots.bump(5, rnoop(), noop(), noop(), noop(), noop());
        List<Change<String, Substance>> changes = this.snapshots.getSubstanceChanges();
        assertEquals(
                Change.<String, Substance>destroy("Oxygen", Location.of(89, 399)),
                changes.getFirst());
        assertEquals("Potassium",
                changes.get(1).getBaby().getId());
    }

    @Test
    void canLoadFullOrganismsSnapshot() {
        // $ zstdcat src/test/resources/test-recordings/*/*/*/organisms.json.zstd > /tmp/o1
        // $ zstdcat src/test/resources/test-recordings/*/*/*/snapshots/*/*/*/*/000000000000099/organisms.json.zstd > /tmp/o2
        // $ jq -s -f src/test/resources/scripts/organism-diff.jq /tmp/o1 /tmp/o2
        // {
        //  "outer": "15",
        //  "inner": "169",
        //  "a": null,
        //  "b": 87668
        // }
        assertFalse(this.snapshots.getOrganisms().contains(15, 169));
        this.snapshots.snap(99, rnoop(), noop(), noop(), noop(), noop());
        assertEquals(87668, this.snapshots.getOrganisms().get(15, 169).getId());
    }

    @Test
    void canLoadFullSubstancesSnapshot() {
        // $ zstdcat src/test/resources/test-recordings/*/*/*/substances.json.zstd > /tmp/s1
        // $ zstdcat src/test/resources/test-recordings/*/*/*/snapshots/*/*/*/*/000000000000099/substances.json.zstd > /tmp/s2
        // $ jq -s -f src/test/resources/scripts/substance-diff.jq /tmp/s1 /tmp/s2
        // {
        //  "outer": "49",
        //  "inner": "310",
        //  "a": "Titanium",
        //  "b": null
        // }
        assertEquals("Titanium", this.snapshots.getSubstances().get(49, 310).getId());
        this.snapshots.snap(99, rnoop(), noop(), noop(), noop(), noop());
        assertNull(this.snapshots.getSubstances().get(49, 310));
    }

    @Test
    void callbacksAreInvoked() {
        Runnable wipe = mock(Runnable.class);
        BiConsumer<Location, Organism> oc = mockcb();
        BiConsumer<Location, Organism> od = mockcb();
        BiConsumer<Location, Substance> sc = mockcb();
        BiConsumer<Location, Substance> sd = mockcb();
        // first snapshot has an organism change
        // $ F=$(grep -L -F '[]' src/test/resources/test-recordings/*/*/*/snapshots/*/*/*/*/*/organism-changes.json | sort | head -1); echo "$F"; cat "$F"
        // src/test/resources/test-recordings/2026-11-02-20/2026-11-02-20-59/2026-11-02-20-59-19-329/snapshots/000/000000/000000000/000000000000/000000000000000/organism-changes.json
        // [{"verb":"MOVE","original":42777,"replacement":42777,"from":{"lat":270,"lon":396},"to":{"lat":269,"lon":395}},{"verb":"UPDATE","to":{"lat":269,"lon":395},"state":{"k":"M","j":"{\"e\":4.0}"}}]%
        this.snapshots.bump(1, wipe, oc, od, sc, sd);
        ArgumentCaptor<Organism> organism = ArgumentCaptor.forClass(Organism.class);
        verify(oc).accept(eq(Location.of(269,395)), organism.capture());
        assertEquals(42777L, organism.getValue().getId());
        verify(od).accept(eq(Location.of(270,396)), organism.capture());
        assertEquals(42777L, organism.getValue().getId());
        // fifth snapshot has a substance change
        // F=$(grep -L -F '[]' src/test/resources/test-recordings/*/*/*/snapshots/*/*/*/*/*/substance-changes.json | sort | head -1); echo "$F"; cat "$F"
        // src/test/resources/test-recordings/2026-11-02-20/2026-11-02-20-59/2026-11-02-20-59-19-329/snapshots/000/000000/000000000/000000000000/000000000000004/substance-changes.json
        // [{"verb":"DESTROY","original":"Oxygen","from":{"lat":89,"lon":399}},{"verb":"CREATE","to":{"lat":89,"lon":399},"baby":"Potassium"}]
        this.snapshots.bump(1, wipe, oc, od, sc, sd);
        this.snapshots.bump(1, wipe,  oc, od, sc, sd);
        this.snapshots.bump(1, wipe, oc, od, sc, sd);
        verifyNoInteractions(sc, sd);
        this.snapshots.bump(1, wipe, oc, od, sc, sd);
        ArgumentCaptor<Substance> substance = ArgumentCaptor.forClass(Substance.class);
        verify(sc).accept(eq(Location.of(89,399)), substance.capture());
        assertEquals("Potassium", substance.getValue().getId());
        verify(sd).accept(eq(Location.of(89,399)), substance.capture());
        assertEquals("Oxygen", substance.getValue().getId());
        verifyNoInteractions(wipe);
    }

    @Test
    void wipeCallbackIsInvoked() {
        Runnable wipe = mock(Runnable.class);
        this.snapshots.snap(1, wipe, noop(), noop(), noop(), noop());
        verifyNoInteractions(wipe);
        this.snapshots.snap(0, wipe, noop(), noop(), noop(), noop());
        verify(wipe).run();
    }

}