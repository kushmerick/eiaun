package io.eiaun.snapshot;

import io.eiaun.physics.Jakku;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface SnapshotRecorder {

    CompletableFuture<String> record(Jakku jakku, Executor executor);

}
