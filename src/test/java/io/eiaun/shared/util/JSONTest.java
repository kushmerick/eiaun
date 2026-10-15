package io.eiaun.shared.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JSONTest {

    @Getter @Setter
    private static class Parent {
        public int x;
        public String y;
    }

    @Getter @Setter
    private static class Child extends Parent {
        private boolean z;
    }

    @Test
    void toJSON() {
        Child child = new Child();
        child.setX(123);
        child.setY("abc");
        child.setZ(true);
        String json = JSON.toJson(child);
        assertEquals(
                "{\"x\":123,\"y\":\"abc\",\"z\":true}",
                json);
    }

    @Test
    void fromJSON() {
        Child child = JSON.fromJson(
                "{\"x\":123,\"y\":\"abc\",\"z\":true}",
                Child.class);
        assertEquals(123, child.getX());
        assertEquals("abc", child.getY());
        assertTrue(child.isZ());
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ParentWithCtors {
        private int x;
        private String y;
    }

    @Getter
    private static class ChildWithCtor extends ParentWithCtors {
        private boolean z;
        public ChildWithCtor(int x, String y, boolean z) {
            super(x, y);
            this.z = z;
        }
    }

    @Test
    void toJSONWithCtor() {
        ChildWithCtor child = new ChildWithCtor(123, "abc", true);
        String json = JSON.toJson(child);
        assertEquals(
                "{\"x\":123,\"y\":\"abc\",\"z\":true}",
                json);
    }

    @Test
    void fromJSONWithCtor() {
        ChildWithCtor child = JSON.fromJson(
                "{\"x\":123,\"y\":\"abc\",\"z\":true}",
                ChildWithCtor.class);
        assertEquals(123, child.getX());
        assertEquals("abc", child.getY());
        assertTrue(child.isZ());
    }

}