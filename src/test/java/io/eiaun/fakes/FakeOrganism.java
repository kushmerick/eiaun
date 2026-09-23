package io.eiaun.fakes;

import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.organisms.State;
import io.eiaun.organisms.simple.SimpleGenome;
import io.eiaun.organisms.simple.SimpleOrganism;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;

import java.util.Map;
import java.util.Set;

public interface FakeOrganism {

    static Organism make(Jakku jakku) {
        return new SimpleOrganism(
                jakku,
                Map.of(SimpleOrganism.VISION_RADIUS_PROPERTY, 5d));
    }

    static Organism make(Jakku jakku, Response response) {
        return new SimpleOrganism(
                jakku,
                Map.of(SimpleOrganism.VISION_RADIUS_PROPERTY, 5d)
        ) {
            @Override
            public SimpleGenome getGenome() {
                return new SimpleGenome(5) {
                    @Override
                    public Response respond(
                            State state,
                            Set<Location> empty,
                            Map<Location, Substance> substances,
                            Map<Location, Organism> organisms
                    ) {
                        return response;
                    }
                };
            }
        };
    }

}
