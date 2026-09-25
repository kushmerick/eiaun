package io.eiaun.shared.physics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SubstanceSpec {

    public final String id;
    public final Map<String, String> properties;
    public final String child;
    public double abundance;

    public Substance make() {
        return new Substance(this.id, this.properties, this.child);
    }

}
