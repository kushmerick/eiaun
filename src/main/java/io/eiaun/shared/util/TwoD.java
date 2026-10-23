package io.eiaun.shared.util;


import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class TwoD<Thing> {

    // simplify experiments with different backing stores
    private final Supplier<Map<Integer,Map<Integer, Thing>>> level1 = ConcurrentHashMap::new;
    private final Function<Integer, Map<Integer, Thing>> level2 = _ -> new ConcurrentHashMap<>();

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

    @SafeVarargs
    public final String anyDiscrepancy(TwoD<Thing> that, Function<Thing,?>... getters) {
        // TODO: make this 2-pass approach more efficient
        String discrepancy = anyDiscrepancy2(that, getters);
        if (discrepancy != null) return discrepancy;
        return that.anyDiscrepancy2(this, getters);
    }

    @SafeVarargs
    private String anyDiscrepancy2(TwoD<Thing> that, Function<Thing,?>... getters) {
        for (int i : this.firstIndices()) {
            for (int j : this.secondIndices(i)) {
                Thing mine = this.get(i, j);
                Thing yours = that.get(i, j);
                for (Function<Thing, ?> getter : getters) {
                    Object m = mine == null ? null : getter.apply(mine);
                    Object y = yours == null ? null : getter.apply(yours);
                    if (!Objects.equals(m, y)) {
                        return String.format("(%s,%s): %s/%s <> %s/%s", i, j, mine, m, yours, y);
                    }
                }
            }
        }
        return null;
    }


}