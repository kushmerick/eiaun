package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.snapshot.representations.Config;
import io.eiaun.shared.snapshot.representations.Physics;
import io.eiaun.shared.util.TwoD;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.List;

public interface Snapshots {

    Physics getPhysics();

    Config getConfig();

    long getRecordingTimestamp();

    Pair<Long, Long> getSnapshotIndexAndTimestamp(Path snapshot);

    List<Change<String>> getSubstanceChanges(Path snapshot);

    List<Change<Long>> getOrganismChanges(Path snapshot);

    void stepTo(Path snapshot);

    TwoD<Organism> getOrganisms();

    TwoD<Substance> getSubstances();

}
