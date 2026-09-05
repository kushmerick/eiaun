package io.eiaun;

import io.eiaun.control.Control;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.time.Duration;

@SpringBootApplication
@Slf4j
public class EIAUN implements CommandLineRunner {

    private static final String DURATION_SECS = "eiaun.control.duration_secs";
    private static final int DEFAULT_DURATION_SECS = 10;

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext ctx = SpringApplication.run(EIAUN.class, args);
        // wait a given number of seconds, then exit the simulation (or run the
        // simulation forever if a negative duration is specified)
        int durationSecs = ctx.getEnvironment().getProperty(DURATION_SECS, Integer.class, DEFAULT_DURATION_SECS);
        if (durationSecs < 0) {
            // we're using virtual threads, which are always treated as daemons, so
            // wait for the main thread to keep the simulation running forever.
            Thread.currentThread().join();
        } else {
            Thread.sleep(Duration.ofSeconds(durationSecs));
        }
    }

    private final Control control;

    @Autowired
    public EIAUN(Control control) {
        this.control = control;
    }

    @Override
    public void run(String @NonNull [] args) {
        log.info("Starting");
        this.control.start();
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping");
        this.control.stop();
    }

}
