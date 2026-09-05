package io.eiaun.concepts.ecosystem;

import io.eiaun.physics.Substance;

import java.util.Map;

public record Response(
        State newState,
        Map<Location, Substance> substanceChanges,
        Map<Location, Organism> organismChanges
) {

    public static Response of(
            State newState,
            Map<Location, Substance> substanceChanges,
            Map<Location, Organism> organismChanges
    ) {
        return new Response(newState, substanceChanges, organismChanges);
    }

}
