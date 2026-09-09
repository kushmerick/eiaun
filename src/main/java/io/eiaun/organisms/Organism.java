package io.eiaun.organisms;

import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;

import java.util.Map;
import java.util.Set;

public interface Organism {

    Response respond(
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location,Organism> neighbors
    );

    long getId();

    void setState(State state);

    Genome getGenome();

}
