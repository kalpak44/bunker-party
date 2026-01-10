package com.bunkerparty.websocket.helpers;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void shouldReturnStringOrDefault() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", " value ");
        obj.addProperty("empty", " ");

        assertEquals("value", JsonUtils.stringOrDefault(obj, "key", "def"));
        assertEquals("def", JsonUtils.stringOrDefault(obj, "empty", "def"));
        assertEquals("def", JsonUtils.stringOrDefault(obj, "missing", "def"));
    }

    @Test
    void shouldCreateErrorObject() {
        JsonObject error = JsonUtils.error("code1", "message1");

        assertEquals("error", error.get("type").getAsString());
        assertEquals("code1", error.get("code").getAsString());
        assertEquals("message1", error.get("message").getAsString());
    }
}
