package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileSnapshotsTest {

    private static final Path RECORDINGS = Path.of("src/test/resources/test-recordings");
    public static final Path RECORDING = RECORDINGS.resolve("2026-11-01-15/2026-11-01-15-12/2026-11-01-15-12-31-275");

    private Snapshots snapshots;

    @BeforeEach
    void setUp() {
        this.snapshots = FileSnapshots.of(RECORDING);
    }

    private <Thing> BiConsumer<Location,Thing> noop() {
        return (_, _) -> {};
    }

    @SuppressWarnings("unchecked")
    private <Thing> BiConsumer<Location,Thing> mockcb() {
        return (BiConsumer<Location, Thing>) mock(BiConsumer.class);
    }

    @Test
    void canGetRecordingTimestamp() {
        assertEquals(1793545951275L, snapshots.getRecordingTimestamp());
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
        this.snapshots.next(noop(), noop(), noop(), noop());
        assertEquals(0, this.snapshots.getId());
        this.snapshots.next(noop(), noop(), noop(), noop());
        assertEquals(1, this.snapshots.getId());
    }

    @Test
    void canNext() {
        this.snapshots.next(noop(), noop(), noop(), noop());
        assertEquals(0, this.snapshots.getId());
    }

    @Test
    void canBump() {
        this.snapshots.bump(5, noop(), noop(), noop(), noop());
        assertEquals(4, this.snapshots.getId());
    }

    @Test
    void canSnap() {
        this.snapshots.snap(10, noop(), noop(), noop(), noop());
        assertEquals(10, this.snapshots.getId());
    }

    @Test
    void canNotGetIdBeforeLoadingFirstSnapshot() {
        assertThrows(RuntimeException.class, () -> this.snapshots.getId());
    }

    @Test
    void canGetCount() {
        assertEquals(106, this.snapshots.getCount());
    }

    @Test
    void canNextAndGetOrganismChanges() {
        this.snapshots.next(noop(), noop(), noop(), noop());
        List<Change<Long, Organism>> changes = this.snapshots.getOrganismChanges();
        assertEquals(2, changes.size());
        assertEquals(
                Change.<Long,Organism>move(17381L, Location.of(332,50), Location.of(333,48)),
                changes.getFirst());
    }

    @Test
    void canBumpAndGetSubstanceChanges() {
        // the third snapshot destroys Hydrogen @ (215, 370)
        this.snapshots.bump(3, noop(), noop(), noop(), noop());
        List<Change<String, Substance>> changes = this.snapshots.getSubstanceChanges();
        assertEquals(1, changes.size());
        assertEquals(
                Change.<String, Substance>destroy("Hydrogen", Location.of(215, 370)),
                changes.getFirst());
    }

    @Disabled // TODO fails because changes are incorrectly rejected
    @Test
    void canLoadFullOrganismsSnapshot() {
        // the 99'th snapshot has a full dump, which differs from the start by
        // (among other things) organism 4526 moving from (1, 409) to (1, 412)
        assertEquals(4526, this.snapshots.getOrganisms().get(1, 409).getId());
        this.snapshots.snap(99, noop(), noop(), noop(), noop());
        assertEquals(4526, this.snapshots.getOrganisms().get(1, 412).getId());
    }

    @Disabled // TODO fails because changes are incorrectly rejected
    @Test
    void canLoadFullSubstancesSnapshot() {
        // the 99'th snapshot has a full dump, which differs from the start by
        // (among other things) th Oxygen at (0,318) getting metabolized
        // to Potassium.
        assertEquals("Oxygen", this.snapshots.getSubstances().get(0, 318).getId());
        this.snapshots.snap(99, noop(), noop(), noop(), noop());
        assertEquals("Potassium", this.snapshots.getSubstances().get(0, 318).getId());
    }

    @Disabled // TODO fails because changes are incorrectly rejected
    @Test
    void callbacksAreInvoked() {
        // TODO: Make this comre comprehensive: all four callbacks not just one!
        BiConsumer<Location, Organism> oc = mockcb();
        BiConsumer<Location, Organism> od = mockcb();
        BiConsumer<Location, Substance> sc = mockcb();
        BiConsumer<Location, Substance> sd = mockcb();
        this.snapshots.next(oc, od, sc, sd);
        verifyNoInteractions(oc, od, sc, sd);
        this.snapshots.next(oc, od, sc, sd);
        verifyNoInteractions(oc, od, sc, sd);
        this.snapshots.next(oc, od, sc, sd);
        verifyNoInteractions(oc, od, sc);
        ArgumentCaptor<Substance> substance = ArgumentCaptor.forClass(Substance.class);
        verify(sd).accept(eq(Location.of(215, 370)), substance.capture());
        assertEquals("Hydrogen", substance.getValue().getId());
    }

}