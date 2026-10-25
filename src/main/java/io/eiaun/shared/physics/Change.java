package io.eiaun.shared.physics;

import io.eiaun.shared.util.Generic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

import java.util.Objects;
import java.util.function.Function;

@ToString // just for logging
@Data
@AllArgsConstructor
public class Change<Id,Thing> {

    public enum Verb {
        CREATE,
        MOVE,
        DESTROY,
        UPDATE,
    }

    public static <Id,Thing> Change<Id,Thing> create(
            @NonNull Thing baby,
            @NonNull Location location
    ) {
        return new Change<>(Verb.CREATE, null, null, null, location, baby, null);
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
        return new Change<>(Verb.DESTROY, thing, null, location, null, null, null);
    }

    public static <Id,Thing> Change<Id,Thing> move(
            @NonNull Id thing,
            @NonNull Location from,
            @NonNull Location to
    ) {
        if (Objects.equals(from, to)) {
            throw new IllegalArgumentException("`from` and `to` can't be equal: " + from);
        }
        return new Change<>(Verb.MOVE, thing, thing, from, to, null, null);
    }

    public static <Id,Thing> Change<Id,Thing> update(
            @NonNull Location location,
            @NonNull Generic state
    ) {
        return new Change<>(Verb.UPDATE, null, null, null, location, null, state);
    }

    Verb verb;      // expected fields:
    Id original;    // DESTROY, MOVE
    Id replacement; // MOVE
    Location from;  // DESTROY, MOVE
    Location to;    // CREATE, MOVE, UPDATE
    Thing baby;     // CREATE
    Generic state;  // UPDATE

    public boolean isCreate() {
        return this.verb == Verb.CREATE;
    }

    public boolean isMove() {
        return this.verb == Verb.MOVE;
    }

    public boolean isUpdate() {
        return this.verb == Verb.UPDATE;
    }

    public boolean isDestroy() {
        return this.verb == Verb.DESTROY;
    }

    public Change<Id,Thing> offset(Location offset, int grid) {
        Function<Location, Location> offsetter = l -> l.add(offset, grid);
        return new Change<>(
                this.verb,
                this.original,
                this.replacement,
                this.from == null ? null : offsetter.apply(this.from),
                this.to == null ? null : offsetter.apply(this.to),
                this.baby,
                this.state);
    }

    public <Thang> Change<Id,Thang> translate(Function<Thing,Thang> translator) {
        return new Change<>(
                this.verb,
                this.original,
                this.replacement,
                this.from,
                this.to,
                this.baby == null ? null : translator.apply(this.baby),
                this.state);
    }

}
