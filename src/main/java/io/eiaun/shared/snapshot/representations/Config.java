package io.eiaun.shared.snapshot.representations;

import io.eiaun.shared.physics.Jakku;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
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
