package io.eiaun.shared.fakes;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.Response;
import io.eiaun.shared.organisms.simple.SimpleGenome;
import io.eiaun.shared.organisms.simple.SimpleOrganism;
import io.eiaun.shared.physics.Jakku;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public interface FakeOrganism {

    static Organism make(Jakku jakku) {
        return new SimpleOrganism(
                jakku,
                Map.of(SimpleOrganism.VISION_RADIUS_PROPERTY, 5d));
    }

    static Organism make(Jakku jakku, Response response) {
        SimpleOrganism organism = (SimpleOrganism) make(jakku);
        organism.setGenome(mock(SimpleGenome.class));
        when(organism.getGenome().respond(any(), any(), any(), any(), any()))
                .thenReturn(response);
        return organism;
    }

}
