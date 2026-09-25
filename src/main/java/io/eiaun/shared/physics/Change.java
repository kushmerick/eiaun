package io.eiaun.shared.physics;

import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.Objects;

@ToString // just for logging
@Value
public class Change<Thing> {

    public static <Thing> Change<Thing> create(
            @NonNull Thing thing,
            @NonNull Location location
    ) {
        return new Change<>(null, thing, null, location);
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
        return new Change<>(thing, null, location, null);
    }

    public static <Thing> Change<Thing> move(
            @NonNull Thing thing,
            @NonNull Location from,
            @NonNull Location to
    ) {
        if (Objects.equals(from, to)) {
            throw new IllegalArgumentException("`from` and `to` can't be equal: " + from);
        }
        return new Change<>(thing, thing, from, to);
    }

    public static <Thing> Change<Thing> replace(
            @NonNull Thing original,
            @NonNull Thing replacement,
            @NonNull Location location
    ) {
        if (Objects.equals(original, replacement)) {
            throw new IllegalArgumentException("`original` and `replacement` can't be equal: " + original);
        }
        return new Change<>(original, replacement, location, location);
    }

    //                    populated for...
    Thing original;    // destroy, move, replace
    Thing replacement; // create, move, replace
    Location from;     // destroy, move, replace
    Location to;       // create, move, replace

    public boolean isCreate() {
        return this.from == null && this.to != null &&
                this.original == null && this.replacement != null;
    }

    public boolean isDestroy() {
        return this.from != null && this.to == null &&
                this.original != null && this.replacement == null;
    }

    public boolean isMove() {
        return this.to != null && this.from != null && this.to != this.from &&
                this.original != null && this.replacement != null && this.original == this.replacement;
    }

    public boolean isReplace() {
        return this.to != null && this.from != null && this.to == this.from &&
                this.original != null && this.replacement != null && this.original != this.replacement;
    }

}
