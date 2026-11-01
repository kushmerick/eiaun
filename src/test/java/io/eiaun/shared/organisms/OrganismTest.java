package io.eiaun.shared.organisms;

import io.eiaun.shared.fakes.FakeOrganism;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganismTest {

    @Test
    public void testToString() {
        Organism organism = FakeOrganism.make(null);
        assertEquals(
                "<Org:" + organism.getId() + ">",
                organism.toString());
    }

}