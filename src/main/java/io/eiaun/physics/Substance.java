package io.eiaun.physics;

import lombok.Data;

import java.util.Map;

@Data
public class Substance {

    private final String id;
    private final Map<String, String> properties;
    private final String child;

}
