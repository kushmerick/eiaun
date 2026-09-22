package io.eiaun.snapshot;

import io.eiaun.organisms.Genome;
import io.eiaun.organisms.Organism;
import io.eiaun.physics.Change;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
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
