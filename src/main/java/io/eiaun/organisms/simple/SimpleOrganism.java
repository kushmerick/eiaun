package io.eiaun.organisms.simple;

import io.eiaun.organisms.Genome;
import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.organisms.State;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An utterly trivial organism that doesn't do anything.
 */
public class SimpleOrganism extends Organism {

    private static final AtomicLong ID = new AtomicLong();
    public static final String VISION_RADIUS_PROPERTY = "vision_radius";

    @Getter private final long id;
    private final SimpleGenome genome;
    private SimpleState state;

    public SimpleOrganism(
            Jakku ignored,
            Map<String, Double> properties
    ) {
        this.id = ID.incrementAndGet();
        this.genome = new SimpleGenome(properties.get(VISION_RADIUS_PROPERTY));
        this.state = new SimpleState();
    }

    @Override
    public Genome getGenome() {
        return this.genome;
    }

    @Override
    public Response respond(
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location, Organism> neighbors
    ) {
        Response response = this.genome.respond(this.state, empties, substances, neighbors);
        this.state = (SimpleState) response.newState();
        return response;
    }

}
