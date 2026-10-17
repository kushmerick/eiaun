package io.eiaun.shared.util;

import com.vaadin.copilot.shaded.commons.lang3.function.FailableSupplier;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableCallable;

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

    public static AutoCloseable get(
            String message,
            FailableCallable<AutoCloseable, IOException> runnable
    ) {
        log.trace(message);
        try {
            return runnable.call();
        } catch (IOException failure) {
            log.warn("Failure while {}", message, failure);
            return () -> {};
        }
    }

}
