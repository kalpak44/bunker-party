package com.bunkerparty.websocket.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;

public class WebSocketJsonSender {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    /**
     * Sends a JsonObject to a WebSocket session.
     */
    public void send(Session session, JsonObject json) throws IOException {
        if (!isOpen(session)) {
            return;
        }
        session.getRemote().sendString(GSON.toJson(json));
    }

    private boolean isOpen(Session session) {
        return session != null && session.isOpen();
    }
}
