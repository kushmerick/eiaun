package io.eiaun.concepts.ecosystem;

import io.eiaun.physics.Substance;

import java.util.Map;
import java.util.Set;

public interface Organism {

    Response respond(
            Set<Location> empty,
            Map<Location, Substance> substances,
            Map<Location,Organism> neighbors
    );

    long getId();

    void setState(State state);

    Genome getGenome();

}
