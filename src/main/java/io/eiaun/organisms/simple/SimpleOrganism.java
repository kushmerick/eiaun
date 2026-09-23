package io.eiaun.organisms.simple;

import io.eiaun.organisms.Organism;
import io.eiaun.organisms.State;
import io.eiaun.physics.Jakku;
import lombok.Getter;

import java.util.Map;

/**
 * An utterly trivial organism that doesn't do anything.
 */
public class SimpleOrganism extends Organism {

    public static final String VISION_RADIUS_PROPERTY = "vision_radius";

    @Getter private SimpleState state;
    @Getter private final SimpleGenome genome;

    public SimpleOrganism(
            Jakku ignored,
            Map<String, Double> properties
    ) {
        super();
        this.state = new SimpleState();
        this.genome = new SimpleGenome(properties.get(VISION_RADIUS_PROPERTY));
    }

    @Override
    public void setState(State state) {
        this.state = (SimpleState) state;
    }

}
