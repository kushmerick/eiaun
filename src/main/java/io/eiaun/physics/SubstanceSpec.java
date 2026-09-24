package io.eiaun.physics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SubstanceSpec {

    public final String id;
    public double abundance;
    public final Map<String, String> properties;

    public Substance make() {
        return new Substance(id, properties);
    }

}
