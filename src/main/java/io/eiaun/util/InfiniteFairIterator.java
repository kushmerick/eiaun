package io.eiaun.util;


import java.util.*;

/**
 * Produce an infinite iterator of randomly selected elements from a given collection.  It is "fair"
 * in that the elements are selected from the concatenation of an infinite sequence of random
 * permutations of the original collection.
 */
public class InfiniteFairIterator<T> implements Iterator<T> {

    private final Collection<T> items;
    private Iterator<T> permutation = Collections.emptyIterator();

    public static <U> InfiniteFairIterator<U> of(Collection<U> items) {
        return new InfiniteFairIterator<>(items);
    }

    private InfiniteFairIterator(Collection<T> items) {
        this.items = items;
    }

    @Override
    public boolean hasNext() {
        return !this.items.isEmpty();
    }

    @Override
    public T next() {
        if (!this.permutation.hasNext()) {
            List<T> p = new ArrayList<>(this.items);
            Collections.shuffle(p);
            this.permutation = p.iterator();
        }
        return this.permutation.next();
    }

}
