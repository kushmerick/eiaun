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

    record StepToResult(boolean clean, String dirt) {
        public static StepToResult CLEAN = new StepToResult(true, null);

        public static StepToResult dirty(String dirt) {
            return new StepToResult(false, dirt);
        }

        public boolean dirty() {
            return !this.clean;
        }
    }

    StepToResult stepTo(
            Path snapshot,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback);

    default StepToResult stepTo(Path snapshot) {
        return stepTo(snapshot, (_, _) -> {}, (_, _) -> {}, (_, _) -> {}, (_, _) -> {});
    }

    TwoD<Organism> getOrganisms();

    TwoD<Substance> getSubstances();

    Pair<Long, Long> getSnapshotIndexAndTimestamp();

    Pair<Long, Long> getSnapshotIndexAndTimestamp(Path snapshot);

    List<Change<String, Substance>> getSnapshotSubstanceChanges();

    List<Change<String, Substance>> getSnapshotSubstanceChanges(Path snapshot);

    List<Change<Long, Organism>> getSnapshotOrganismChanges();

    List<Change<Long, Organism>> getSnapshotOrganismChanges(Path snapshot);

}
