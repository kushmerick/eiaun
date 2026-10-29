package io.eiaun.shared.physics;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
public class Substance {

    @Getter private final String id;
    @Getter private final Map<String, String> properties;
    @Getter private final String metabolite;

    // just for logging
    @Override public String toString() {
        return "<Sub:" + this.id + ">";
    }

}
