package io.eiaun.viz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VIZTest {

    @Test
    void canRun() {
        try (ConfigurableApplicationContext ctx = SpringApplication.run(
                VIZ.class,
                "--spring.profiles.active=viz,test",
                "--server.port=0")
        ) {
            assertTrue(ctx.isRunning());
        }
    }

}