package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.TwoD;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.function.FailableLongSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapshotsTest {

    private record TestSnapshots(
            FailableLongSupplier<IllegalStateException> nextSnapshotIdSupplier,
            long count,
            Consumer<Long> snapProbe
    ) implements Snapshots {
        public TestSnapshots(
                long nextSnapshotId,
                long count,
                Consumer<Long> snapProbe) {
            this(() -> {
                        if (nextSnapshotId <= 0) {
                            // Mimic this FileSnapshots behavior because Snapshots relies on it
                            // TODO: Resolve this bad smell?
                            throw new IllegalStateException();
                        } else {
                            return nextSnapshotId - 1;
                        }
                    },
                    count,
                    snapProbe);
        }
        @Override public long getRecordingTimestamp() {
            throw new NotImplementedException();
        }
        @Override public Physics getPhysics() {
            throw new NotImplementedException();
        }
        @Override public Config getConfig() {
            throw new NotImplementedException();
        }
        @Override public Result snap(long id, BiConsumer<Location, Organism> organismCreatedCallback, BiConsumer<Location, Organism> organismDestroyedCallback, BiConsumer<Location, Substance> substanceCreatedCallback, BiConsumer<Location, Substance> substanceDestroyedCallback) {
            this.snapProbe.accept(id);
            return Result.SUCCESS; // TODO: Is this a bad smell? Opportunity for more testing?
        }
        @Override public TwoD<Organism> getOrganisms() {
            throw new NotImplementedException();
        }
        @Override public TwoD<Substance> getSubstances() {
            throw new NotImplementedException();
        }
        @Override public List<Change<String, Substance>> getSubstanceChanges() throws IllegalStateException {
            throw new NotImplementedException();
        }
        @Override public List<Change<Long, Organism>> getOrganismChanges() throws IllegalStateException {
            throw new NotImplementedException();
        }
        @Override public long getId() throws IllegalStateException {
            return this.nextSnapshotIdSupplier.getAsLong();
        }
        @Override public long getCount() {
            return this.count;
        }
    }

    private <Thing> BiConsumer<Location,Thing> noop() {
        return (_, _) -> {};
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 1, 0, 0",
            "-1, 1, 1, 1",
            "-1, 10, 0, 0",
            "-1, 10, 1, 1",
            "-1, 10, -1, 0",
            "-1, 10, 10, 10",
            "-1, 10, 11, 10",
            "1, 1, 3, 0",
            "1, 10, 3, 3",
            "5, 10, 0, 0",
            "9, 10, 0, 0",
            "9, 10, 1, 1",
            "10, 10, 0, 0",
            "10, 10, 1, 0",
    })
    void testBoundBump(
            long nextSnapshotId,
            long count,
            long delta,
            long boundDelta
    ) {
        Snapshots snapshots = new TestSnapshots(nextSnapshotId, count, null);
        assertEquals(boundDelta, snapshots.boundBump(delta));
    }

    @Test
    void testBumpWithoutCurrentSnapshot() {
        Long[] captured = { null };
        Consumer<Long> snapProbe = id -> captured[0] = id;
        Snapshots snapshots = new TestSnapshots(-1, 10, snapProbe);
        snapshots.bump(2, noop(), noop(), noop(), noop());
        assertEquals(2 - 1, captured[0]);
    }

    @Test
    void testBumpWithCurrentSnapshot() {
        Long[] captured = { null };
        Consumer<Long> snapProbe = id -> captured[0] = id;
        Snapshots snapshots = new TestSnapshots(3, 10, snapProbe);
        snapshots.bump(2, noop(), noop(), noop(), noop());
        assertEquals(3 - 1 + 2, captured[0]);
    }

}