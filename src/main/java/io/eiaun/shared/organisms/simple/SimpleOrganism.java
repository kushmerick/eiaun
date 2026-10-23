package io.eiaun.shared.organisms.simple;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.physics.Jakku;
import lombok.Getter;
import lombok.Setter;
import org.apache.fory.json.annotation.JsonProperty;

import java.util.Map;

/**
 * An utterly trivial organism that doesn't do anything.
 */
public class SimpleOrganism extends Organism {

    public static final String VISION_RADIUS_PROPERTY = "vision_radius";

    @JsonProperty("s") @Getter private SimpleState state;
    @JsonProperty("g") @Getter @Setter private SimpleGenome genome;

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
