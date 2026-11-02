package io.eiaun.shared.organisms.simple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleStateTest {

    @Test
    void allSimpleStatesAreEqual() {
        assertEquals(new SimpleState(), new SimpleState());
    }

}