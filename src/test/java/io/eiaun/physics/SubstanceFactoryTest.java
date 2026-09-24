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
    void substancesAreSingletons() {
        SubstanceFactory substanceFactory = new SubstanceFactory(
                List.of(new SubstanceSpec("id", Map.of("P", "V"), null, 1234)));
        Substance substance1 = substanceFactory.make("id");
        Substance substance2 = substanceFactory.make("id");
        assertSame(substance1, substance2);
    }

    @Test
    void canRunMain() {
        SubstanceFactory.main(new String[] {});
    }


}