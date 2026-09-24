package io.eiaun.physics;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
public class Substance {

    @Getter private final String id;
    @Getter private final Map<String, String> properties;
    @Getter private final String child;

}
