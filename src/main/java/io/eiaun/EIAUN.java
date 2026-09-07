package io.eiaun;

import io.eiaun.control.Control;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class EIAUN implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(EIAUN.class, args);
    }

    private final Control control;

    @Autowired
    public EIAUN(Control control) {
        this.control = control;
    }

    @Override
    public void run(String @NonNull [] ignored) throws InterruptedException {
        log.info("Starting");
        this.control.start();
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping");
        // nothing to do: Control manages its own lifecycle
    }

}
