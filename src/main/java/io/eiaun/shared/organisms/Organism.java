package io.eiaun.shared.organisms;

import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Substance;
import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

abstract public class Organism {

    private static final AtomicLong ID = new AtomicLong();

    @Getter private final long id;

    protected Organism() {
        this.id = ID.getAndIncrement();
    }

    abstract public void setState(State state);

    abstract public State getState();

    abstract public Genome getGenome();

    public Response respond(
            Jakku jakku,
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location,Organism> neighbors
    ) {
        Response response = getGenome().respond(jakku, getState(), empties, substances, neighbors);
        setState(response.newState());
        return response;
    }

}
