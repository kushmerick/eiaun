package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.TwoD;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public interface Snapshots {

    Physics getPhysics();

    Config getConfig();

    long getRecordingTimestamp();

    void bumpNextSnapshotIndex(int delta);

    record StepToResult(Path snapshot, boolean clean, String dirt) {
        public static StepToResult clean(Path snapshot) {
            return new StepToResult(snapshot, true, null);
        }
        public static StepToResult dirty(Path snapshot, String dirt) {
            return new StepToResult(snapshot, false, dirt);
        }
        public boolean dirty() {
            return !this.clean;
        }
    }

    StepToResult stepToSnapshot(
            Path snapshot,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback);

    default StepToResult stepToSnapshot(Path snapshot) {
        return stepToSnapshot(snapshot, (_, _) -> {}, (_, _) -> {}, (_, _) -> {}, (_, _) -> {});
    }

    StepToResult stepToNextSnapshot(
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback);

    TwoD<Organism> getOrganisms();

    TwoD<Substance> getSubstances();

    Pair<Long, Long> getSnapshotIndexAndTimestamp();

    @Deprecated
    Pair<Long, Long> getSnapshotIndexAndTimestamp(Path snapshot);

    List<Change<String, Substance>> getSnapshotSubstanceChanges();

    @Deprecated
    List<Change<String, Substance>> getSnapshotSubstanceChanges(Path snapshot);

    List<Change<Long, Organism>> getSnapshotOrganismChanges();

    @Deprecated
    List<Change<Long, Organism>> getSnapshotOrganismChanges(Path snapshot);

}
