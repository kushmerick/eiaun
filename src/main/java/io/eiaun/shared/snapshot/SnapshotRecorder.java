package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface SnapshotRecorder {

    /**
     * Record a snapshot.
     * @param jakku Jakku
     * @param changeOffset The location to which "___Changes" locations are relative.
     * @param organismChanges Organism changes.
     * @param substanceChanges Substance changes.
     * @return A pair comprising a snapshot id, and a runnable.  The id uniquely
     * identifies this specific snapshot. When invoked, the runnable actually
     * writes the snapshot. This runnable is returned because implementations
     * typically write to some slow backing store, so the caller can schedule that
     * heavy lifting however it likes (eg by submitting it to an ExecutorService),
     * rather than complicating implementations with such details.
     */
    Pair<String, Runnable> record(
            Jakku jakku,
            Location changeOffset,
            List<Change<Long>> organismChanges,
            List<Change<String>> substanceChanges
    );

}
