package io.eiaun.shared.util;

import org.apache.fory.json.ForyJson;

public class JSON {

    // Fory or Gson
    private static final boolean FORY_ENABLED = true;

    private static final ForyJson FORY = ForyJson.builder()
            .withFieldMode(true) // disable discovery from `getFoo` and so forth
            .writeNullFields(false)
            .build();
    private static final ForyJson GSON = ForyJson.builder().build();

    public static String toJson(Object thing) {
        return FORY_ENABLED
                ? FORY.toJson(thing)
                : GSON.toJson(thing);
    }

    /*
    public static <Thing> Thing fromJson(String json, Class<Thing> clazz) {
        return FORY_ENABLED
                ? FORY.fromJson(json, clazz)
                : GSON.fromJson(json, clazz);
    }
    */

}
