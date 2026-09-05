package io.eiaun.control;

import io.eiaun.concepts.ecosystem.Ecosystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class Control {

    private final AtomicLong taskCounter = new AtomicLong();
    private final ExecutorService executor;
    private final Ecosystem ecosystem;
    private final Semaphore concurrencyLimiter;
    private final int taskCount;

    @Autowired
    public Control(
            ExecutorService executor,
            Semaphore concurrencyLimiter,
            Ecosystem ecosystem,
            int taskCount
    ) {
        this.executor = executor;
        this.concurrencyLimiter = concurrencyLimiter;
        this.ecosystem = ecosystem;
        this.taskCount = taskCount;
    }

    public void start() {
        // TODO: The intent is to schedule `eiaun.control.task_count` tasks in parallel; at any given time at most N actually
        // TODO: run (where N = `eiaun.control.processor_fraction` X the number of CPUs). This is implemented using a
        // TODO: CompletableFuture that recursively calls `scheduleNext` to start its replacement.  This apparently
        // TODO: works fine, but it seems like a bad smell (for example, the CFs are discarded with nothing actually
        // TODO: awaiting their completion - is this a leak?). Is there a simpler/cleaner implementation?
        log.info("App started — launching {} virtual-thread tasks forever...", this.taskCount);
        for (int i = 0; i < this.taskCount; i++) {
            scheduleNext();
        }
    }

    private void scheduleNext() {
        CompletableFuture
            .runAsync(this::acquireSlot, this.executor)
            .thenRun(() -> step(this.taskCounter.incrementAndGet()))
            .thenRun(this::releaseSlot)
            .thenRun(this::scheduleNext);
    }

    private void acquireSlot() {
        try {
            this.concurrencyLimiter.acquire();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseSlot() {
        this.concurrencyLimiter.release();
    }

    private void step(long id) {
        log.info("Starting task {}", id);
        this.ecosystem.step();
        log.info("Finished task {}", id);
    }

    public void stop() {
        log.info("Stopping");
        this.executor.shutdownNow(); // TODO: Why does "shutdown" not work?
    }


}
