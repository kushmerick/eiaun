package io.eiaun.shared.util;

import java.util.Map;

public record Generic(
        String k,
        String j
) {

    public static Generic encode(
            Object thing,
            Map<Class<?>, String> classKinds
    ) {
        return new Generic(
                classKinds.get(thing.getClass()),
                JSON.toJson(thing));
    }

    @SuppressWarnings("unchecked")
    public <T> T decode(
            Map<String, Class<?>> kindClasses
    ) {
        return (T) JSON.fromJson(this.j, kindClasses.get(this.k));
    }

}
