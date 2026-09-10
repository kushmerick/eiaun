package io.eiaun.control;

import io.eiaun.physics.Jakku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class Control {

    private final AtomicLong taskCounter = new AtomicLong();
    private final ExecutorService executor;
    private final Jakku jakku;
    private final int maxConcurrency;
    private final int durationSeconds;
    private boolean running = false;
    private CountDownLatch runningForever = null;

    @Autowired
    public Control(
            ExecutorService executor,
            Jakku jakku,
            int maxConcurrency,
            int durationSeconds
    ) {
        this.executor = executor;
        this.jakku = jakku;
        this.maxConcurrency = maxConcurrency;
        this.durationSeconds = durationSeconds;
    }

    public void start() throws InterruptedException {
        log.info("Starting");
        jakku.initialize();
        this.running = true;
        for (int i = 0; i < this.maxConcurrency; i++) {
            this.executor.submit(this::taskLoop);
        }
        log.info("Started");
        // wait a given number of seconds, then exit the simulation (or run the
        // simulation forever if a negative duration is specified)
        if (this.durationSeconds < 0) {
            log.info("Running forever");
            this.runningForever = new CountDownLatch(1);
            this.runningForever.await();
        } else {
            log.info("Sleeping for {} seconds", this.durationSeconds);
            Thread.sleep(Duration.ofSeconds(durationSeconds));
            log.info("Stopping");
            stop();
            log.info("Stopped");
        }
    }

    private void taskLoop() {
        while (this.running) {
            long id = this.taskCounter.incrementAndGet();
            try {
                CompletableFuture
                        .runAsync(() -> log.trace("Task {} started", id), this.executor)
                        .thenCompose(_ -> this.jakku.step(this.executor))
                        .thenAccept(extinction -> {
                            if (extinction) {
                                log.info("Extinction");
                                stop();
                            }
                        })
                        .thenRunAsync(() -> log.trace("Task {} finished", id), this.executor)
                        .get();

            } catch (InterruptedException interrupted) {
                log.info("Task {} interrupted", id);
                return;
            } catch (ExecutionException failure) {
                if (failure.getCause() instanceof RejectedExecutionException) {
                    log.info("Task {} stopped", id);
                } else {
                    log.warn("Task {} failed", id, failure);
                }
            }
        }
    }

    private void stop() {
        log.info("Shutting down");
        this.running = false;
        this.executor.shutdownNow();
        if (this.runningForever != null) {
            this.runningForever.countDown();
        }
    }

}
