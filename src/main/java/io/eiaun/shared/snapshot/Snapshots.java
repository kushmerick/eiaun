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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * This interfaces defines methods for inspecting and traversing snapshots.
 * Implementations of this class are instantiated for a specific recording.
 * Snapshots have a unique sequential long id, starting from zero.
 */
public interface Snapshots {

    Logger log = LoggerFactory.getLogger(Snapshots.class);

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

    /**
     * Bound the given delta so that I = (current snapshot id + delta) is a valid snapshot
     * id -- ie it is in the range 0 <= I < getCount(). But handle the case where there is
     * no current snapshot.
     * @param delta the requested delta
     * @return the most extreme (positive or negative) valid delta.
     */
    default long boundBump(long delta) {
        if (delta == 0) {
            // short-circuit expensive getCount below -- it is always
            // valid (albeit a presumably meaningless UX) to stay where you are
            return 0;
        }
        // There are two cases: there is, or is not, a current snapshot. We determine this
        // by getting the id of the current snapshot, which throws an exception if it does
        // not exist.  It would be possible to collapse the two cases with a special fake
        // value for the current snapshot id (-1?) when it does not exist. But I'm having
        // a hard time reasoning about all the cases so I'm leaving it explicit.
        try {
            // case 1: there is a current snapshot
            long current = getId();
            long requested = current + delta;
            if (delta > 0) {
                long upper = Math.min(requested, getCount() - 1);
                if (requested > upper) {
                    // 1A: delta would bump us beyond the last snapshot
                    log.info("Bounding positive delta {} to {}", delta, upper - current);
                    return upper - current;
                }
            } else { // delta < 0
                if (requested <= 0) {
                    // 1B: delta would bump us before the first snapshot
                    log.info("Bounding negative delta {} to {}", delta, -current);
                    return -current;
                }
            }
            // happy path - case 1C: delta is valid
            log.info("Valid delta {}", delta);
            return delta;
        } catch (IllegalStateException failure) {
            // case 2: there is no current snapshot
            log.info("Bounding bump with no current snapshot");
            if (delta > 0) {
                // 2A: ensure delta doesn't bump us beyond the last snapshot
                long bound = Math.min(delta, getCount());
                log.info("Bounding positive delta {} without snapshot to {}", delta, bound);
                return bound;
            } else { // delta < 0
                // 2B: delta would bump us before the first snapshot
                log.info("Bounding negative delta {} without snapshot to 0", delta);
                return 0;
            }
        }
    }

    default Result bump(
            long delta,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback
    ) {
        long id;
        try {
            id = getId();
        } catch (IllegalStateException failure) {
            log.info("Bumping to first snapshot");
            id = -1;
        }
        return snap(
                Math.max(0, id + delta),
                organismCreatedCallback,
                organismDestroyedCallback,
                substanceCreatedCallback,
                substanceDestroyedCallback);
    }

    Result snap(
            long id,
            BiConsumer<Location, Organism> organismCreatedCallback,
            BiConsumer<Location, Organism> organismDestroyedCallback,
            BiConsumer<Location, Substance> substanceCreatedCallback,
            BiConsumer<Location, Substance> substanceDestroyedCallback);

    long getId() throws IllegalStateException;

    long getCount();

    TwoD<Organism> getOrganisms();

    TwoD<Substance> getSubstances();

    List<Change<String, Substance>> getSubstanceChanges() throws IllegalStateException;

    List<Change<Long, Organism>> getOrganismChanges() throws IllegalStateException;

}
