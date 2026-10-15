package io.eiaun.shared.physics;

import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.Objects;
import java.util.function.Function;

@ToString // just for logging
@Value
public class Change<Thing> {

    public enum Verb {
        CREATE,
        MOVE,
        DESTROY,
        REPLACE,
    }

    public static <Thing> Change<Thing> create(
            @NonNull Thing thing,
            @NonNull Location location
    ) {
        return new Change<>(Verb.CREATE, null, thing, null, location);
    }

    public static <Thing> Change<Thing> destroy(
            @NonNull Thing thing
    ) {
        return destroy(thing, Location.ORIGIN);
    }

    public static <Thing> Change<Thing> destroy(
            @NonNull Thing thing,
            @NonNull Location location
    ) {
        return new Change<>(Verb.DESTROY, thing, null, location, null);
    }

    public static <Thing> Change<Thing> move(
            @NonNull Thing thing,
            @NonNull Location from,
            @NonNull Location to
    ) {
        if (Objects.equals(from, to)) {
            throw new IllegalArgumentException("`from` and `to` can't be equal: " + from);
        }
        return new Change<>(Verb.MOVE, thing, thing, from, to);
    }

    public static <Thing> Change<Thing> replace(
            @NonNull Thing original,
            @NonNull Thing replacement,
            @NonNull Location location
    ) {
        if (Objects.equals(original, replacement)) {
            throw new IllegalArgumentException("`original` and `replacement` can't be equal: " + original);
        }
        return new Change<>(Verb.REPLACE, original, replacement, location, location);
    }

    Verb verb;         // expected fields:
    Thing original;    // DESTROY, MOVE, REPLACE
    Thing replacement; // CREATE, MOVE, REPLACE
    Location from;     // DESTROY, MOVE, REPLACE
    Location to;       // CREATE, MOVE, REPLACE

    public boolean isCreate() {
        return this.verb == Verb.CREATE;
    }

    public boolean isMove() {
        return this.verb == Verb.MOVE;
    }

    public boolean isDestroy() {
        return this.verb == Verb.DESTROY;
    }

    public boolean isReplace() {
        return this.verb == Verb.REPLACE;
    }

    public <Thang> Change<Thang> translate(Function<Thing,Thang> translator) {
        return new Change<>(
                this.verb,
                this.original == null ? null : translator.apply(this.original),
                this.replacement == null ? null : translator.apply(this.replacement),
                this.from,
                this.to);
    }

}
