package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface SnapshotRecorder {

    Pair<String, Runnable> record(
            Jakku jakku,
            Location changeOffset,
            List<Change<Organism>> organismChanges,
            List<Change<Substance>> substanceChanges
    );

}
