package io.eiaun.physics;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubstanceFactoryTest {

    @Test
    void canMake() {
        SubstanceFactory substanceFactory = new SubstanceFactory(
                List.of(new SubstanceSpec("id", Collections.emptyMap(), null, 1234)));
        Substance substance = substanceFactory.make();
        assertEquals("id", substance.getId());
    }

    @Test
    void canMakeById() {
        SubstanceFactory substanceFactory = new SubstanceFactory(
                List.of(new SubstanceSpec("id", Map.of("P", "V"), null, 1234)));
        Substance substance = substanceFactory.make("id");
        assertEquals("id", substance.getId());
        assertEquals(Map.of("P", "V"), substance.getProperties());
    }

    @Test
    void canRunMain() {
        SubstanceFactory.main(new String[] {});
    }


}