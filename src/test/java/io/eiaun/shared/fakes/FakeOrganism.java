package io.eiaun.shared.fakes;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.Response;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.organisms.simple.SimpleGenome;
import io.eiaun.shared.organisms.simple.SimpleOrganism;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;

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
                            Jakku jakku,
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
