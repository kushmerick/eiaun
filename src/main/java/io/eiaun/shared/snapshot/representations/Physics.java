package io.eiaun.shared.snapshot.representations;

import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.SubstanceSpec;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class Physics {

    Map<String, SubstanceSpec> substanceSpecs;

    public static Physics from(Jakku jakku) {
        return new Physics(jakku.getSubstanceFactory().getSubstanceSpecs());
    }

}
