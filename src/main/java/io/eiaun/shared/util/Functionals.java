package io.eiaun.shared.util;

import com.vaadin.copilot.shaded.commons.lang3.function.FailableSupplier;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableCallable;
import org.apache.commons.lang3.function.FailableRunnable;

import java.io.IOException;

@UtilityClass
@Slf4j
public class Functionals {

    public static <Thing> Thing get(
            FailableSupplier<Thing, Exception> getter
    ) {
        try {
            return getter.get();
        } catch (Exception failure) {
            // propagate exceptions, but checked exceptions maker lambdas etc messy
            throw new RuntimeException(failure);
        }
    }

    public static void run(
            FailableRunnable<Exception> runnable
    ) {
        try {
            runnable.run();
        } catch (Exception failure) {
            // propagate exceptions, but checked exceptions maker lambdas etc messy
            throw new RuntimeException(failure);
        }
    }

    public static AutoCloseable get(
            String message,
            FailableCallable<AutoCloseable, IOException> callable
    ) {
        log.trace(message);
        try {
            return callable.call();
        } catch (IOException failure) {
            log.warn("Failure while {}", message, failure);
            return () -> {};
        }
    }

}
