package io.eiaun.shared.util;


import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class TwoD<Thing> {

    // simplify experiments with different backing stores
    private final Supplier<Map<Integer,Map<Integer, Thing>>> level1 = HashMap::new;
    private final Function<Integer, Map<Integer, Thing>> level2 = _ -> new HashMap<>();

    @Getter(AccessLevel.PACKAGE) private final Map<Integer, Map<Integer, Thing>> things;

    public TwoD() {
        this.things = this.level1.get();
    }

    public void set(int i, int j, Thing thing) {
        if (thing == null) {
            remove(i, j);
        } else {
            this.things.computeIfAbsent(i, this.level2).put(j, thing);
        }
    }

    public Thing get(int i, int j) {
        return this.things.getOrDefault(i, Collections.emptyMap()).get(j);
    }

    public boolean contains(int i, int j) {
        return this.things.containsKey(i) && this.things.get(i).containsKey(j);
    }

    public void remove(int i, int j) {
        Map<Integer, Thing> second = this.things.get(i);
        if (second != null) {
            second.remove(j);
            if (second.isEmpty()) {
                this.things.remove(i);
            }
        }
    }

    public boolean isEmpty() {
        return this.things.isEmpty();
    }

    public void clear() {
        this.things.clear();
    }

    public Set<Integer> firstIndices() {
        return this.things.keySet();
    }

    public Set<Integer> secondIndices(int i) {
        return this.things.getOrDefault(i, Collections.emptyMap()).keySet();
    }

    public void replace(TwoD<Thing> that) {
        this.things.clear();
        this.things.putAll(that.things);
    }

    public <Id> boolean equals(TwoD<Thing> that, Function<Thing, Id> identifier) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.things.size() != that.things.size()) return false; // short-circuit
        for (int i: this.firstIndices()) {
            for (int j: this.secondIndices(i)) {
                Thing mine = this.get(i, j);
                Thing yours = that.get(i, j);
                if (yours == null || !Objects.equals(identifier.apply(mine), identifier.apply(yours))) {
                    return false;
                }
            }
        }
        return true;
    }

}