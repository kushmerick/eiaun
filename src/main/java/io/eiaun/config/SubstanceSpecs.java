package io.eiaun.config;

import io.eiaun.physics.SubstanceSpec;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "eiaun.jakku.substances")
@Value
public class SubstanceSpecs {

    List<SubstanceSpec> specs;

}
