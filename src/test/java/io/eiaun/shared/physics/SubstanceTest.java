package io.eiaun.shared.physics;

import io.eiaun.shared.fakes.FakeOrganism;
import io.eiaun.shared.fakes.FakeSubstance;
import io.eiaun.shared.organisms.Organism;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubstanceTest {

    @Test
    public void testToString() {
        Substance substance = new FakeSubstance();
        assertEquals(
                "<Sub:" + substance.getId() + ">",
                substance.toString());
    }

}