package io.eiaun.organisms;

import io.eiaun.fakes.FakeOrganism;
import io.eiaun.organisms.simple.SimpleState;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OrganismTest {

    @Test
    void setsStateWhenResponding() {
        Response response = Response.of(
                new SimpleState(),
                Collections.emptyList(),
                Collections.emptyList());
        Organism organism = FakeOrganism.make(null, response);
        assertNotEquals(response.newState(), organism.getState());
        organism.respond(
                Collections.emptySet(),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertEquals(response.newState(), organism.getState());
    }

}