package io.eiaun.util;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TwoD<Thing> {

    private final Map<Integer, Map<Integer, Thing>> things;

    public TwoD() {
        things = new HashMap<>();
    }

    public void set(int i, int j, Thing thing) {
        if (thing == null) {
            remove(i, j);
        } else {
            this.things.computeIfAbsent(i, _ -> new HashMap<>()).put(j, thing);
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

    public Set<Integer> firstIndices() {
        return this.things.keySet();
    }

    public Set<Integer> secondIndices(int i) {
        return this.things.getOrDefault(i, Collections.emptyMap()).keySet();
    }

}