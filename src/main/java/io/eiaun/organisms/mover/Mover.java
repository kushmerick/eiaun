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
 * Every mover prefers substances with randomly selected properties.
 * The organism moves toward the closest substance that doesn't
 * already have a co-located organism. It starts with some energy;
 * each move consumes some energy; its energy is replenished when it
 * eats one of it's preferred substances; an organism dies if it uses up
 * all its energy.
 */
@Data
public class Mover implements Organism {

    private static final AtomicLong ID = new AtomicLong();
    private static final Random RANDOM = new Random();
    public static final String VISION_RADIUS_PROPERTY = "vision_radius";
    public static final String DESIRABLE_PROPERTIES_PROPERTY = "desirable_properties";
    public static final String PEAK_ENERGY_PROPERTY = "peak_energy";
    public static final String MOVE_ENERGY_PROPERTY = "move_energy";

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
        int desirablePropertyCount = properties.get(DESIRABLE_PROPERTIES_PROPERTY).intValue();
        if (desirablePropertyCount > allSubstancePropertiesKeys.length) {
            throw new RuntimeException(String.format("%s %s is larger than the number of properties %s",
                    DESIRABLE_PROPERTIES_PROPERTY, desirablePropertyCount,  allSubstancePropertiesKeys.length));
        }
        while (desirableSubstanceProperties.size() < desirablePropertyCount) {
            String property = allSubstancePropertiesKeys[RANDOM.nextInt(allSubstancePropertiesKeys.length)];
            String[] values = allSubstanceProperties.get(property).toArray(String[]::new);
            String value = values[RANDOM.nextInt(values.length)];
            desirableSubstanceProperties.put(property, value);
        }
        double peakEnergy = properties.get(PEAK_ENERGY_PROPERTY);
        this.genome = new MoverGenome(
                this,
                desirableSubstanceProperties,
                properties.get(VISION_RADIUS_PROPERTY),
                peakEnergy,
                properties.get(MOVE_ENERGY_PROPERTY));
        this.state = new MoverState(peakEnergy);
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
