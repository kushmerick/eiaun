package io.eiaun.snapshot;

import io.eiaun.organisms.Organism;
import io.eiaun.physics.Change;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;

import java.io.IOException;
import java.util.List;

public interface SnapshotRecorder {

    String record(
            Jakku jakku,
            Location changeOffset,
            List<Change<Organism>> organismChanges,
            List<Change<Substance>> substanceChanges
    ) throws IOException;

}
