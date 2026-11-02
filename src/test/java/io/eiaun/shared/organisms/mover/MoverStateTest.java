package io.eiaun.shared.organisms.mover;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MoverStateTest {

    @Test
    void energyDeterminesMoverStateEquality() {
        assertEquals(new MoverState(123), new MoverState(123));
        assertNotEquals(new MoverState(123), new MoverState(456));
    }

}