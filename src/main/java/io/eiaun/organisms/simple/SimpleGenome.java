package io.eiaun.organisms.simple;

import io.eiaun.organisms.Genome;
import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.organisms.State;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SimpleGenome implements Genome {

    @Getter
    private final double visionRadius;

    public SimpleGenome(double visionRadius) {
        this.visionRadius = visionRadius;
    }

    @Override
    public Response respond(
            State state,
            Set<Location> empty,
            Map<Location, Substance> substances,
            Map<Location, Organism> organisms
    ) {
        return Response.of(
                state,
                Collections.emptyMap(),
                Collections.emptyMap());
    }

}
