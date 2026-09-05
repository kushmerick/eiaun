package io.eiaun.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class InfiniteFairIteratorTest {

    private static final int MIN = 1;
    private static final int MAX = 10;
    private static final Set<Integer> ITEMS = IntStream.range(MIN, MAX + 1).boxed().collect(Collectors.toSet());

    @Test
    void isFair() {
        InfiniteFairIterator<Integer> seq = InfiniteFairIterator.of(ITEMS);
        for (int i = 0; i < 10; i++) {
            Set<Integer> emitted = new HashSet<>();
            for (int _ : ITEMS) {
                emitted.add(seq.next());
            }
            assertEquals(ITEMS, emitted);
        }
    }

    @Test
    void hasAtLeastOneMillionElements() {
        InfiniteFairIterator<Integer> seq = InfiniteFairIterator.of(ITEMS);
        for (int i = 0; i < 1_000_000; i++) {
            assertTrue(seq.hasNext());
            int item = seq.next();
            assertTrue(item >= MIN);
            assertTrue(item <= MAX);
        }
    }

}