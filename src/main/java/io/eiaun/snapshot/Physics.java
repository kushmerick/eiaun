package io.eiaun.snapshot;

import io.eiaun.physics.Jakku;
import io.eiaun.physics.SubstanceSpec;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class Physics {

    private Map<String, SubstanceSpec> substanceSpecs;

    public static Physics from(Jakku jakku) {
        return new Physics(
                jakku.getSubstanceFactory().getSubstanceSpecs().stream()
                        .collect(Collectors.toMap(SubstanceSpec::getId, Function.identity())));
    }

}
