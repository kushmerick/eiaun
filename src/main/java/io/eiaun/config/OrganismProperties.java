package io.eiaun.config;

import io.eiaun.concepts.ecosystem.Organism;
import io.eiaun.physics.SubstanceSpec;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "eiaun.jakku.organisms")
@Data
public class OrganismProperties {

    private final Map<String, Map<String, Double>> properties;

}
