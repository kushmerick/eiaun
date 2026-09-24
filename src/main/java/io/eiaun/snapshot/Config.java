package io.eiaun.snapshot;

import io.eiaun.physics.Jakku;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Config {

    int grid;
    double organismDensity;
    double substanceDensity;

    public static Config from(Jakku jakku) {
        return new Config(
                jakku.getGrid(),
                jakku.getOrganismDensity(),
                jakku.getSubstanceDensity());
    }

}
