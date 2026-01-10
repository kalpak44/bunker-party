package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.bunkerparty.websocket.helpers.JsonUtils;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class NewGameHandler implements MessageHandler {

    private final GameService gameService;

    @Inject
    public NewGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws IOException {
        String name = msg.has("name") && !msg.get("name").isJsonNull() ? msg.get("name").getAsString().trim() : "";
        if (name.isEmpty()) {
            gameService.sendToSession(session, JsonUtils.error("name_required", "Name is required"));
            return;
        }
        if (name.length() > 10) {
            gameService.sendToSession(session, JsonUtils.error("name_too_long", "Name is too long"));
            return;
        }

        Room room = gameService.createRoom();
        String playerId = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();
        Player creator = new Player(playerId, token, name, session, Map.of());
        room.addPlayer(creator);

        JsonObject openRoom = new JsonObject();
        openRoom.addProperty("type", "open_room");
        openRoom.addProperty("room_id", room.getRoomId());
        openRoom.addProperty("player_id", playerId);
        openRoom.addProperty("token", token);
        gameService.sendToSession(session, openRoom);

        gameService.broadcastUpdate(room);
    }
}
