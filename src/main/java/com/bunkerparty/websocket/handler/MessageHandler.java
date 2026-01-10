package com.bunkerparty.websocket.handler;

import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;

public interface MessageHandler {
    /**
     * Handles an incoming WebSocket message.
     */
    void handle(Session session, JsonObject message) throws Exception;
}
