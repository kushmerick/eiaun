package io.eiaun.config;

import io.eiaun.physics.SubstanceSpec;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "eiaun.jakku.substances")
@Data
public class SubstanceSpecs {

    private final List<SubstanceSpec> specs;

    public Map<String, Map<String, String>> getProperties() {
        return this.specs.stream().collect(Collectors.toMap(SubstanceSpec::getId, SubstanceSpec::getProperties));
    }

}
