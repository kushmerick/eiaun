package io.eiaun.implementations.simple;

import io.eiaun.concepts.ecosystem.*;
import io.eiaun.physics.Substance;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class SimpleOrganism implements Organism {

    private static final AtomicLong ID = new AtomicLong();
    public static final String VISION_RADIUS_PROPERTY = "vision_radius";

    private final long id;
    private final Genome genome;
    private State state;

    public SimpleOrganism(Map<String, Double> properties) {
        this.id = ID.incrementAndGet();
        this.genome = new SimpleGenome(properties.get(VISION_RADIUS_PROPERTY));
        this.state = new SimpleState();
    }

    @Override
    public Response respond(
            Set<Location> empty,
            Map<Location, Substance> substances,
            Map<Location, Organism> neighbors
    ) {
        return this.genome.respond(this.state, empty, substances, neighbors);
    }

}
