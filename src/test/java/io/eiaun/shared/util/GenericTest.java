package io.eiaun.shared.util;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericTest {

    // this simple hierarchy has some quirks that are intended to resemble the
    // classes we will be using in the production code

    private abstract static class SampleParent {
        @Getter private String id;
        private SampleParent() {
            this.id = "abc";
        }
        abstract void setX(long x);
    }

    private static class Sample extends SampleParent {
        @Getter private int x;
        @Getter private boolean y;
        public Sample() {
            super();
            this.x = 123;
            this.y = true;
        }
        public Sample(Boolean ignored) {
            this();
        }
        @Override public void setX(long x) {
            this.x = (int) x;
        }

    }

    private static final Map<String, Class<?>> KIND_CLASSES = Map.of(
            "S", Sample.class
    );

    private static final Map<Class<?>, String> CLASS_KINDS =
            KIND_CLASSES.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    @Test
    void canEncode() {
        Sample sample = new Sample(Boolean.TRUE);
        Generic generic = Generic.encode(sample, CLASS_KINDS);
        String encoded = JSON.toJson(generic);
        assertEquals(
                """
                        {"k":"S","j":"{\\"id\\":\\"abc\\",\\"x\\":123,\\"y\\":true}"}""",
                encoded);
    }

    @Test
    void canDecode() {
        Generic generic = new Generic(
                "S",
                """
                        {"k":"S","j":"{\\"id\\":\\"abc\\",\\"x\\":123,\\"y\\":true}"}""");
        Sample sample = generic.decode(KIND_CLASSES);
        assertEquals("abc", sample.getId());
        assertEquals(123, sample.getX());
        assertTrue(sample.isY());
    }


}