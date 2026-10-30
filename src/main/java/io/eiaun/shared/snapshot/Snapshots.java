package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.TwoD;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public interface Snapshots {

    long getRecordingTimestamp();

    Physics getPhysics();

    Config getConfig();

    record Result(boolean found, @NotNull List<String> dirt) {
        public static Result SUCCESS = new Result(true, Collections.emptyList());
        public static Result NOT_FOUND = new Result(false, Collections.emptyList());
        public static Result dirty(@NotEmpty List<String> dirt) {
            return new Result(true, dirt);
        }
    }

    default Result next(
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback
    ) {
        return bump(0,
                organismCreatedCallback,
                organismDestroyedCallback,
                substanceCreatedCallback,
                substanceDestroyedCallback);
    }

    Result bump(
            long delta,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback);

    Result snap(
            long id,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback);

    long getId();
    long getMaxId();

    TwoD<Organism> getOrganisms();

    TwoD<Substance> getSubstances();

    List<Change<String, Substance>> getSubstanceChanges();

    List<Change<Long, Organism>> getOrganismChanges();

}
