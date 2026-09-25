package io.eiaun.organisms;

import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;

import java.util.Map;
import java.util.Set;

public abstract class Genome {

    public abstract double getVisionRadius();

    public abstract Response respond(
            Jakku jakku,
            State state,
            Set<Location> empty,
            Map<Location, Substance> substances,
            Map<Location, Organism> neighbors);

}
