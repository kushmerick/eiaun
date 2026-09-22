package io.eiaun.organisms;

import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;

import java.util.Map;
import java.util.Set;

abstract public class Organism {

    public abstract Response respond(
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location,Organism> neighbors
    );

    public abstract long getId();

    public abstract Genome getGenome();

}
