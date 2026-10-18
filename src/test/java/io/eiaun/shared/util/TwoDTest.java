package io.eiaun.shared.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class TwoDTest {

    @Test
    void removingLastSubElementClearsSecondLevel() {
        TwoD<Integer> twod = new TwoD<>();
        twod.set(10, 20, 30);
        assertEquals(Map.of(10, Map.of(20, 30)), twod.getThings());
        twod.remove(10, 20);
        assertEquals(Collections.emptyMap(), twod.getThings());
    }

    @Test
    void removingNonLastSubElementDoesNotClearSecondLevel() {
        TwoD<Integer> twod = new TwoD<>();
        twod.set(10, 20, 30);
        twod.set(10, 50, 60);
        assertEquals(
                Map.of(10, Map.of(20, 30, 50, 60)),
                twod.getThings());
        twod.remove(10, 20);
        assertEquals(
                Map.of(10, Map.of(50, 60)),
                twod.getThings());
    }

    @Test
    void canRemoveNonexistentElement() {
        TwoD<Integer> twod = new TwoD<>();
        twod.remove(10, 20);
        assertEquals(Collections.emptyMap(), twod.getThings());
    }

    @Test
    void settingNullMeansRemove() {
        TwoD<Integer> twod = new TwoD<>();
        twod.set(10, 20, 30);
        assertTrue(twod.contains(10, 20));
        twod.set(10, 20, null);
        assertFalse(twod.contains(10, 20));
    }

    @Test
    void settingComputeEquality() {
        TwoD<Integer> twod1 = new TwoD<>();
        assertFalse(twod1.equals(null, Function.identity()));
        TwoD<Integer> twod2 = new TwoD<>();
        assertTrue(twod1.equals(twod2, Function.identity()));
        twod1.set(10, 20, 30);
        assertFalse(twod1.equals(twod2, Function.identity()));
        twod2.set(50, 60, 70);
        assertFalse(twod1.equals(twod2, Function.identity()));
        twod2.remove(50, 60);
        twod2.set(10, 20, 40);
        assertFalse(twod1.equals(twod2, Function.identity()));
        twod2.set(10, 20, 30);
        assertTrue(twod1.equals(twod2, Function.identity()));
    }

}