package io.eiaun.shared.organisms.simple;

import io.eiaun.shared.organisms.Genome;
import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.Response;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SimpleGenome extends Genome {

    @Getter
    private final double visionRadius;

    public SimpleGenome(double visionRadius) {
        this.visionRadius = visionRadius;
    }

    @Override
    public Response respond(
            Jakku jakku,
            State state,
            Set<Location> empty,
            Map<Location, Substance> substances,
            Map<Location, Organism> organisms
    ) {
        return Response.of(
                state,
                Collections.emptyList(),
                Collections.emptyList());
    }

}
