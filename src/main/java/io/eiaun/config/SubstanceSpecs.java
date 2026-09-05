package io.eiaun.config;

import io.eiaun.physics.SubstanceSpec;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "eiaun.jakku")
@Data
public class SubstanceSpecs {

    private final List<SubstanceSpec> substances;

}
