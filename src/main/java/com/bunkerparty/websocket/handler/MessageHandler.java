package com.bunkerparty.websocket.handler;

import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;

public interface MessageHandler {
    void handle(Session session, JsonObject message) throws Exception;
}
