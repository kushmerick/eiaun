package io.eiaun.shared.physics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SubstanceSpec {

    public String id;
    public Map<String, String> properties;
    public String child;
    public double abundance;

    public Substance make() {
        return new Substance(this.id, this.properties, this.child);
    }

}
