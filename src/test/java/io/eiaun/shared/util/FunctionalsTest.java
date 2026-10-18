package io.eiaun.shared.util;

import com.vaadin.copilot.shaded.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FunctionalsTest {

    @Test
    public void getPoliteSupplierIsInvoked() {
        assertEquals(123, Functionals.get(() -> 123));
    }

    @Test
    public void getImpoliteSupplierIsChecked() {
        assertThrows(RuntimeException.class,
                () -> Functionals.get(() -> { throw new Exception(); }));
    }

    @Test
    public void getPoliteRunnableDoesNotThrow() {
        Functionals.run(() -> {});
    }

    @Test
    public void getImpoliteRunnableIsChecked() {
        assertThrows(RuntimeException.class,
                () -> Functionals.run(() -> { throw new Exception(); }));
    }

    @Test
    public void politeCloseableGetterIsInvoked() throws Exception {
        MutableBoolean called = new MutableBoolean(false);
        AutoCloseable closeable = Functionals.get("foo", () -> called::setTrue);
        closeable.close();
        assertTrue(called.get());
    }

    @Test
    public void impoliteCloseableGetterIsIgnored() throws Exception {
        AutoCloseable closeable = Functionals.get("foo", () -> { throw new IOException(); });
        closeable.close();
    }


}