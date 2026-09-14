package io.eiaun.organisms;

import io.eiaun.physics.Change;
import io.eiaun.physics.Substance;

import java.util.List;

public record Response(
        State newState,
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
