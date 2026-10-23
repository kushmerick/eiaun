package io.eiaun.shared.physics;

import io.eiaun.shared.organisms.Organism;

import java.util.List;

public record Changes(
        List<Change<String,Substance>> substanceChanges,
        List<Change<Long, Organism>> organismChanges
) {

    public static Changes of(
            List<Change<String,Substance>> substanceChanges,
            List<Change<Long,Organism>> organismChanges
    ) {
        return new Changes(substanceChanges, organismChanges);
    }

}
