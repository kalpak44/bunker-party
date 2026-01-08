package com.bunkerparty.websocket.handler;

import com.bunkerparty.manager.RoomManager;
import com.bunkerparty.model.Player;
import com.bunkerparty.model.Room;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.bunkerparty.websocket.helpers.JsonUtils;
import com.google.gson.JsonArray;
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
        String name = msg.has("name") && !msg.get("name").isJsonNull() ? msg.get("name").getAsString().trim() : "";
        if (name.isEmpty()) {
            sender.send(session, JsonUtils.error("name_required", "Name is required"));
            return;
        }
        if (name.length() > 10) {
            sender.send(session, JsonUtils.error("name_too_long", "Name is too long"));
            return;
        }

        Room room = roomManager.createRoom();

        String token = UUID.randomUUID().toString();
        Player creator = new Player(UUID.randomUUID().toString(), token, name, session, Map.of());
        room.addPlayer(creator);

        JsonObject res = new JsonObject();
        res.addProperty("type", "open_room");
        res.addProperty("room_id", room.getRoomId());
        res.addProperty("player_id", creator.getId());
        res.addProperty("token", token);
        res.addProperty("phase", room.getPhase());
        res.addProperty("round", room.getRound());

        JsonArray players = new JsonArray();
        JsonObject pObj = new JsonObject();
        pObj.addProperty("id", creator.getId());
        pObj.addProperty("name", creator.getName());
        pObj.addProperty("online", creator.isOnline());
        players.add(pObj);
        res.add("players", players);

        sender.send(session, res);
    }
}
