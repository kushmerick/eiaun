package io.eiaun.concepts.ecosystem;

import io.eiaun.physics.Substance;

import java.util.Map;
import java.util.Set;

public interface Genome {

    double getVisionRadius();

    Response respond(
            State state,
            Set<Location> empty,
            Map<Location, Substance> substances,
            Map<Location,Organism> neighbors);

}
