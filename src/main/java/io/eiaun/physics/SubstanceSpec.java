package io.eiaun.physics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SubstanceSpec {

    public String id;
    public double abundance;
    public Map<String, String> properties;

    public Substance make() {
        return new Substance(id, properties);
    }

}
