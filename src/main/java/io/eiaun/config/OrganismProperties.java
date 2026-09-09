package io.eiaun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "eiaun.jakku.organisms")
@Data
public class OrganismProperties {

    // class -> property -> value
    private final Map<String, Map<String, Double>> properties;

}
