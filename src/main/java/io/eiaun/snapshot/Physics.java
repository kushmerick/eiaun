package io.eiaun.snapshot;

import io.eiaun.physics.Jakku;
import io.eiaun.physics.SubstanceSpec;
import lombok.Value;

import java.util.Map;

@Value
public class Physics {

    Map<String, SubstanceSpec> substanceSpecs;

    public static Physics from(Jakku jakku) {
        return new Physics(jakku.getSubstanceFactory().getSubstanceSpecs());
    }

}
