package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.bunkerparty.websocket.helpers.JsonUtils;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;

public abstract class BaseMessageHandler implements MessageHandler {

    protected final GameService gameService;
    private static final int MAX_NAME_LENGTH = 10;

    /**
     * Creates a new base message handler.
     */
    protected BaseMessageHandler(GameService gameService) {
        this.gameService = gameService;
    }

    protected String getString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    protected Room getRoom(JsonObject json) {
        return gameService.getRoom(getString(json, "roomId"));
    }

    protected Player getPlayer(Room room, JsonObject json) {
        return room != null ? room.getPlayer(getString(json, "playerId")) : null;
    }

    protected void sendOpenRoom(Session session, Room room, Player player) {
        JsonObject openRoom = new JsonObject();
        openRoom.addProperty("type", "open_room");
        openRoom.addProperty("room_id", room.getRoomId());
        openRoom.addProperty("player_id", player.getId());
        openRoom.addProperty("token", player.getToken());
        gameService.sendToSession(session, openRoom);
    }

    protected boolean validateName(Session session, String name) {
        if (name == null || name.trim().isEmpty()) {
            gameService.sendToSession(session, JsonUtils.error("name_required", "Name is required"));
            return false;
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            gameService.sendToSession(session, JsonUtils.error("name_too_long", "Name is too long"));
            return false;
        }
        return true;
    }
}
