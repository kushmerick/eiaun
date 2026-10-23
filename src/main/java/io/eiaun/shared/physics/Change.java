package io.eiaun.shared.physics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@ToString // just for logging
@Data
@AllArgsConstructor
public class Change<Id,Thing> {

    public enum Verb {
        CREATE,
        MOVE,
        DESTROY,
    }

    public static <Id,Thing> Change<Id,Thing> create(
            @NonNull Thing baby,
            @NonNull Location location
    ) {
        return new Change<>(Verb.CREATE, null, null, null, location, baby);
    }

    public static <Id,Thing> Change<Id,Thing> destroy(
            @NonNull Id thing
    ) {
        return destroy(thing, Location.ORIGIN);
    }

    public static <Id,Thing> Change<Id,Thing> destroy(
            @NonNull Id thing,
            @NonNull Location location
    ) {
        return new Change<>(Verb.DESTROY, thing, null, location, null, null);
    }

    public static <Id,Thing> Change<Id,Thing> move(
            @NonNull Id thing,
            @NonNull Location from,
            @NonNull Location to
    ) {
        if (Objects.equals(from, to)) {
            throw new IllegalArgumentException("`from` and `to` can't be equal: " + from);
        }
        return new Change<>(Verb.MOVE, thing, thing, from, to, null);
    }

    Verb verb;      // expected fields:
    Id original;    // DESTROY, MOVE, REPLACE
    Id replacement; // MOVE, REPLACE
    Location from;  // DESTROY, MOVE, REPLACE
    Location to;    // CREATE, MOVE, REPLACE
    Thing baby;     // CREATE

    public boolean isCreate() {
        return this.verb == Verb.CREATE;
    }

    public boolean isMove() {
        return this.verb == Verb.MOVE;
    }

    public boolean isDestroy() {
        return this.verb == Verb.DESTROY;
    }

    public Change<Id,Thing> offset(Location offset, int grid) {
        Function<Location, Location> offsetter = l -> l.add(offset, grid);
        return new Change<>(
                getVerb(),
                getOriginal(),
                getReplacement(),
                Optional.ofNullable(getFrom()).map(offsetter).orElse(null),
                Optional.ofNullable(getTo()).map(offsetter).orElse(null),
                getBaby());
    }

    public <Thang> Change<Id,Thang> translate(Function<Thing,Thang> translator) {
        return new Change<>(
                this.verb,
                this.original,
                this.replacement ,
                this.from,
                this.to,
                this.baby == null ? null : translator.apply(this.baby));
    }

}
