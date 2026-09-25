package io.eiaun.organisms;

import io.eiaun.physics.Location;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Substance;
import lombok.Getter;
import lombok.Setter;

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
