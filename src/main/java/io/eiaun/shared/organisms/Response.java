package io.eiaun.shared.organisms;

import io.eiaun.shared.physics.Change;
import io.eiaun.shared.physics.Substance;

import java.util.List;

public record Response(
        // an organism is responsible for updating its own state
        State newState,
        // Jakku is responsible for changing substances & organisms
        List<Change<Substance>> substanceChanges,
        List<Change<Organism>> organismChanges
) {

    public static Response of(
            State newState,
            List<Change<Substance>> substanceChanges,
            List<Change<Organism>> organismChanges
    ) {
        return new Response(newState, substanceChanges, organismChanges);
    }

}
