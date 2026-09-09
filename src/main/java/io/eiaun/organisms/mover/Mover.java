package io.eiaun.organisms.mover;

import io.eiaun.organisms.Genome;
import io.eiaun.organisms.Organism;
import io.eiaun.organisms.Response;
import io.eiaun.organisms.State;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Location;
import io.eiaun.physics.Substance;
import lombok.Data;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Every mover prefers substances with a randomly selected property.
 * The mover simply moves toward the closest substance that doesn't
 * already have a co-located organism.
 */
@Data
public class Mover implements Organism {

    private static final AtomicLong ID = new AtomicLong();
    private static final Random RANDOM = new Random();
    public static final String VISION_RADIUS_PROPERTY = "vision_radius";
    public static final String DESIRABLE_PROPERTIES_PROPERTY = "desirable_properties";

    private final long id;
    private final Genome genome;
    private State state;

    public Mover(
            Jakku jakku,
            Map<String, Double> properties
    ) {
        this.id = ID.incrementAndGet();
        Map<String,String> desirableSubstanceProperties = new HashMap<>();
        Map<String,Set<String>> allSubstanceProperties = jakku.getAllSubstanceProperties();
        String[] allSubstancePropertiesKeys = allSubstanceProperties.keySet().toArray(String[]::new);
        while (desirableSubstanceProperties.size() < properties.get(DESIRABLE_PROPERTIES_PROPERTY)) {
            String property = allSubstancePropertiesKeys[RANDOM.nextInt(allSubstancePropertiesKeys.length)];
            String[] values = allSubstanceProperties.get(property).toArray(String[]::new);
            String value = values[RANDOM.nextInt(values.length)];
            desirableSubstanceProperties.put(property, value);
            // TODO: add check for infinite loop
        }
        this.genome = new MoverGenome(
                this,
                desirableSubstanceProperties,
                properties.get(VISION_RADIUS_PROPERTY));
        this.state = new MoverState();
    }

    @Override
    public Response respond(
            Set<Location> empties,
            Map<Location, Substance> substances,
            Map<Location, Organism> neighbors
    ) {
        return this.genome.respond(this.state, empties, substances, neighbors);
    }

}
