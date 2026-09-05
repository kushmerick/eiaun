package io.eiaun.config;

import io.eiaun.concepts.ecosystem.Ecosystem;
import io.eiaun.concepts.ecosystem.Organism;
import io.eiaun.physics.Jakku;
import io.eiaun.physics.Substance;
import io.eiaun.physics.SubstanceFactory;
import io.eiaun.util.InfiniteFairIterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Configuration
@Slf4j
public class Config {

    @Bean
    public ExecutorService executorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public Semaphore concurrencyLimiter(
            @Value("${eiaun.control.processor_fraction:0.9}") double processorFraction
    ) {
        int cpus = Math.max(1, (int) (Runtime.getRuntime().availableProcessors() * processorFraction));
        log.info("Using {} CPUs ({}%)", cpus, processorFraction * 100);
        return new Semaphore(cpus, true);
    }

    @Bean
    public int taskCount(
            @Value("${eiaun.control.task_count:20}") int taskCount
    ) {
        return taskCount;
    }

    @Bean
    public Supplier<Organism> organismCreator(
            @Value("${eiaun.jakku.organisms.type:io.eiaun.implementations.simple.SimpleOrganism}") String className,
            OrganismProperties organismProperties
    ) {
        try {
            @SuppressWarnings("unchecked")
            Class<Organism> clazz = (Class<Organism>) Class.forName(className);
            Constructor<Organism> constructor = clazz.getConstructor(Map.class);
            Map<String, Double> properties = organismProperties.getProperties().get(className);
            return () -> {
                try {
                    return constructor.newInstance(properties);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failure while constructing `" + className + "`", e);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failure while getting constructor for `" + className + "`", e);
        }
    }

    @Bean
    public Supplier<Substance> substanceCreator(
            SubstanceFactory substanceFactory
    ) {
        return substanceFactory::make;
    }

    @Bean
    public SubstanceFactory atomFactory(
            SubstanceSpecs substanceSpecs
    ) {
        return new SubstanceFactory(substanceSpecs.getSubstances());
    }

    @Bean
    public int grid(@Value("${eiaun.jakku.grid:1000}") int grid) {
        log.info("Grid {}", grid);
        return grid;
    }

    @Bean
    public static double organismInitialDensity(@Value("${eiaun.jakku.initial_density.organism:0.1}") double density) {
        log.info("Organism initial density {}", density);
        return density;
    }

    @Bean
    public static double substanceInitialDensity(@Value("${eiaun.jakku.initial_density.substance:0.2}") double density) {
        log.info("Substance initial density {}", density);
        return density;
    }

    @Bean
    public static Ecosystem ecosystem(
            int grid,
            double organismInitialDensity,
            Supplier<Organism> organismCreator,
            double substanceInitialDensity,
            Supplier<Substance> substanceCreator
    ) {
        return new Jakku(
                grid,
                organismInitialDensity,
                organismCreator,
                substanceInitialDensity,
                substanceCreator,
                InfiniteFairIterator::of,
                log::info);
    }

}