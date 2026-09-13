package io.eiaun.snapshot;

import io.eiaun.organisms.Organism;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;

import java.io.IOException;
import java.util.Map;

public interface SnapshotRecorder {

    String record(
            Jakku jakku,
            Map<Location, Organism> organismChanges,
            Map<Location, Substance> substanceChanges
    ) throws IOException;

}
