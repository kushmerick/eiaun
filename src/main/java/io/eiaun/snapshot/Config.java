package io.eiaun.snapshot;

import io.eiaun.physics.Jakku;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Config {

    private int grid;
    private double organismDensity;
    private double substanceDensity;

    public static Config from(Jakku jakku) {
        return new Config(
                jakku.getGrid(),
                jakku.getOrganismDensity(),
                jakku.getSubstanceDensity());
    }

}
