package io.eiaun.shared.util;

import org.apache.fory.json.ForyJson;
import org.apache.fory.reflect.TypeRef;

public class JSON {

    private static final ForyJson FORY = ForyJson.builder()
            .withFieldMode(true) // disable discovery from `getFoo` and so forth
            .writeNullFields(false)
            .build();

    public static String toJson(Object thing) {
        return FORY.toJson(thing);
    }

    public static <Thing> Thing fromJson(String json, Class<Thing> clazz) {
        return FORY.fromJson(json, clazz);
    }

    public static <Thing> Thing fromJson(String json, TypeRef<Thing> type) {
        return FORY.fromJson(json, type);
    }

}
