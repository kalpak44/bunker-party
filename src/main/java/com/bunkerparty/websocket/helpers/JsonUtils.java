package com.bunkerparty.websocket.helpers;

import com.google.gson.JsonObject;

public final class JsonUtils {

    private JsonUtils() {
        // utility class
    }

    /**
     * Returns a string value from a JsonObject, or a default value if not found or null.
     */
    public static String stringOrDefault(
            JsonObject obj,
            String key,
            String defaultValue
    ) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            String value = obj.get(key).getAsString().trim();
            return value.isBlank() ? defaultValue : value;
        }
        return defaultValue;
    }

    /**
     * Creates an error JsonObject with a given code and message.
     */
    public static JsonObject error(String code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("type", "error");
        error.addProperty("code", code);
        error.addProperty("message", message);
        return error;
    }
}

