package io.eiaun.shared.organisms;

import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.physics.SubstanceFactory;
import lombok.Setter;
import org.apache.fory.json.annotation.JsonIgnore;

import java.util.Map;
import java.util.Set;

public abstract class Genome {

    @JsonIgnore
    @Setter protected SubstanceFactory substanceFactory;

    public abstract double getVisionRadius();

    public abstract Response respond(
            Jakku jakku,
            State state,
            Set<Location> empty,
            Map<Location, Substance> substances,
            Map<Location, Organism> neighbors);

}
