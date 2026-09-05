package io.eiaun.physics;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubstanceFactoryTest {

    @Test
    void canMake() {
        SubstanceFactory substanceFactory = new SubstanceFactory(
                List.of(new SubstanceSpec("id", 1234, Collections.emptyMap())));
        Substance substance = substanceFactory.make();
        assertEquals("id", substance.getId());
    }

    @Test
    void canRunMain() {
        SubstanceFactory.main(new String[] {});
    }


}