package io.eiaun.concepts.ecosystem;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface Ecosystem {

    CompletableFuture<Void> step(ExecutorService executor);

}
