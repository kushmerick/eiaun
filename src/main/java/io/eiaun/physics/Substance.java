package io.eiaun.physics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class Substance {

    private final String id;
    private final Map<String, String> properties;

}
