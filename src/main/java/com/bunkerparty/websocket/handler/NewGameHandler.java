package com.bunkerparty.websocket.handler;

import com.bunkerparty.manager.RoomManager;
import com.bunkerparty.model.Player;
import com.bunkerparty.model.Room;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class NewGameHandler implements MessageHandler {

    private final RoomManager roomManager;
    private final WebSocketJsonSender sender;

    @Inject
    public NewGameHandler(RoomManager roomManager, WebSocketJsonSender sender) {
        this.roomManager = roomManager;
        this.sender = sender;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws IOException {
        String name = msg.get("name").getAsString();
        Room room = roomManager.createRoom();

        Player creator = new Player(UUID.randomUUID().toString(), name, session, Map.of());
        room.addPlayer(creator);

        JsonObject res = new JsonObject();
        res.addProperty("type", "open_room");
        res.addProperty("room_id", room.getRoomId());
        sender.send(session, res);
    }
}
