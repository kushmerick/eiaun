package io.eiaun.shared.organisms.mover;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.physics.Jakku;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Every mover prefers substances with randomly selected properties.
 * The organism moves toward the closest substance that doesn't
 * already have a co-located organism. It starts with some energy;
 * each move consumes some energy; its energy is replenished when it
 * eats one of it's preferred substances; an organism dies if it uses up
 * all its energy.
 */
public class Mover extends Organism {

    private static final Random RANDOM = new Random();
    public static final String VISION_RADIUS_PROPERTY = "vision_radius";
    public static final String DESIRABLE_PROPERTIES_PROPERTY = "desirable_properties";
    public static final String PEAK_ENERGY_PROPERTY = "peak_energy";
    public static final String MOVE_ENERGY_PROPERTY = "move_energy";
    public static final String REST_ENERGY_PROPERTY = "rest_energy";

    @Getter private MoverState state;
    @Getter private final MoverGenome genome;

    public Mover(
            Jakku jakku,
            Map<String, Double> properties
    ) {
        super();
        double peakEnergy = properties.get(PEAK_ENERGY_PROPERTY);
        this.state = new MoverState(peakEnergy);
        this.genome = new MoverGenome(
                makeDesirableSubstanceProperties(jakku, properties),
                properties.get(VISION_RADIUS_PROPERTY),
                peakEnergy,
                properties.get(MOVE_ENERGY_PROPERTY),
                properties.get(REST_ENERGY_PROPERTY));
    }

    private static Map<String, String> makeDesirableSubstanceProperties(
            Jakku jakku,
            Map<String, Double> properties
    ) {
        Map<String, String> desirableSubstanceProperties = new HashMap<>();
        Map<String, Set<String>> allSubstanceProperties = jakku.getAllSubstanceProperties();
        String[] allSubstancePropertiesKeys = allSubstanceProperties.keySet().toArray(String[]::new);
        int desirablePropertyCount = properties.get(DESIRABLE_PROPERTIES_PROPERTY).intValue();
        if (desirablePropertyCount > allSubstancePropertiesKeys.length) {
            throw new RuntimeException(String.format("%s %s is larger than the number of properties %s",
                    DESIRABLE_PROPERTIES_PROPERTY, desirablePropertyCount, allSubstancePropertiesKeys.length));
        }
        while (desirableSubstanceProperties.size() < desirablePropertyCount) {
            String property = allSubstancePropertiesKeys[RANDOM.nextInt(allSubstancePropertiesKeys.length)];
            String[] values = allSubstanceProperties.get(property).toArray(String[]::new);
            String value = values[RANDOM.nextInt(values.length)];
            desirableSubstanceProperties.put(property, value);
        }
        return desirableSubstanceProperties;
    }

    @Override
    public void setState(State state) {
        this.state = (MoverState) state;
    }

}
