package io.eiaun.config;

import io.eiaun.organisms.Organism;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.SubstanceFactory;
import io.eiaun.util.InfiniteFairIterator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Configuration
@Validated
@Slf4j
public class Config {

    @Bean
    public ExecutorService executorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public int maxConcurrency(
            @Value("${eiaun.control.processor_fraction:0.9}") @Positive double processorFraction
    ) {
        if (processorFraction > 0.95) {
            log.warn("Risky value {} for processor fraction", processorFraction);
        }
        return Math.max(1, (int) (Runtime.getRuntime().availableProcessors() * processorFraction));
    }

    @Bean
    public int durationSeconds(
            @Value("${eiaun.control.duration_secs:10}") @NotZero int durationSeconds
    ) {
        return durationSeconds;
    }

    @Bean
    public Function<Jakku, Organism> organismCreator(
            @Value("${eiaun.jakku.organisms.type:io.eiaun.implementations.simple.SimpleOrganism}") String className,
            OrganismProperties organismProperties
    ) {
        try {
            @SuppressWarnings("unchecked")
            Class<Organism> clazz = (Class<Organism>) Class.forName(className);
            Constructor<Organism> constructor = clazz.getConstructor(Jakku.class, Map.class);
            Map<String, Double> properties = organismProperties.getProperties().get(className);
            return (jakku) -> {
                try {
                    return constructor.newInstance(jakku, properties);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failure while constructing `" + className + "`", e);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failure while getting constructor for `" + className + "`", e);
        }
    }

    @Bean
    public SubstanceFactory substanceFactory(
            SubstanceSpecs substanceSpecs
    ) {
        return new SubstanceFactory(substanceSpecs.getSpecs());
    }

    @Bean
    public int grid(
            @Value("${eiaun.jakku.grid:1000}") @Positive int grid
    ) {
        log.info("Grid {}", grid);
        return grid;
    }

    @Bean
    public double organismDensity(
            @Value("${eiaun.jakku.organisms.density:0.1}") @Positive @Max(1) double density
    ) {
        log.info("Organism density {}", density);
        return density;
    }

    @Bean
    public double substanceDensity(
            @Value("${eiaun.jakku.substances.density:0.1}") @Min(0) @Max(1) double density
    ) {
        log.info("Substance density {}", density);
        return density;
    }

    @Bean
    public Jakku jakku(
            int grid,
            double organismDensity,
            Function<Jakku, Organism> organismCreator,
            double substanceDensity,
            SubstanceFactory substanceFactory
    ) {
        return new Jakku(
                grid,
                organismDensity,
                organismCreator,
                substanceDensity,
                substanceFactory,
                InfiniteFairIterator::of,
                log::info);
    }

}