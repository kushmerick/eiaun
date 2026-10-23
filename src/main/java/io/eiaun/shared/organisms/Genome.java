package io.eiaun.shared.organisms;

import io.eiaun.shared.physics.*;
import lombok.Setter;
import org.apache.fory.json.annotation.JsonIgnore;

import java.util.Map;
import java.util.Set;

public abstract class Genome {

    @JsonIgnore
    @Setter protected SubstanceFactory substanceFactory;

    public abstract double getVisionRadius();

    public abstract Changes respond(
            Jakku jakku,
            State state,
            Set<Location> empty,
            Map<Location, Substance> substances,
            Map<Location, Organism> neighbors);

}
