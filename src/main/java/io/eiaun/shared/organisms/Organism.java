package io.eiaun.shared.organisms;

import io.eiaun.shared.organisms.mover.Mover;
import io.eiaun.shared.organisms.simple.SimpleOrganism;
import io.eiaun.shared.physics.Changes;
import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

abstract public class Organism {

    public static final Map<String, Class<?>> KIND_ORGANISMS = Map.of(
            "M", Mover.class,
            "S", SimpleOrganism.class
    );

    public static final Map<Class<?>, String> ORGANISM_KINDS =
            KIND_ORGANISMS.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    public static final AtomicLong ID = new AtomicLong();

    @Getter @Setter private long id;

    protected Organism() {
        this.id = ID.getAndIncrement();
    }

    abstract public void setState(State state);

    abstract public State getState();

    abstract public Genome getGenome();

    public Changes respond(
            Jakku jakku,
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location,Organism> neighbors
    ) {
        return getGenome().respond(jakku, getState(), empties, substances, neighbors);
    }

    // just for logging
    @Override public String toString() {
        return "<Org:" + this.id + ">";
    }

}
