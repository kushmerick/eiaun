package io.eiaun.shared.snapshot;

import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.SubstanceSpec;
import lombok.Value;

import java.util.Map;

@Value
public class Physics {

    Map<String, SubstanceSpec> substanceSpecs;

    public static Physics from(Jakku jakku) {
        return new Physics(jakku.getSubstanceFactory().getSubstanceSpecs());
    }

}
