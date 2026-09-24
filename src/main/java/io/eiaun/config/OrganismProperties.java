package io.eiaun.config;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "eiaun.jakku.organisms")
@Value
public class OrganismProperties {

    // class -> property -> value
    Map<String, Map<String, Double>> properties;

}
