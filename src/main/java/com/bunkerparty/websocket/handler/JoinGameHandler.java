package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;
import java.util.UUID;

import static com.bunkerparty.websocket.helpers.JsonUtils.error;


public class JoinGameHandler implements MessageHandler {

    private final GameService gameService;

    @Inject
    public JoinGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws Exception {
        String roomId = msg.has("roomId") && !msg.get("roomId").isJsonNull() ? msg.get("roomId").getAsString() : "";
        String name = msg.has("name") && !msg.get("name").isJsonNull() ? msg.get("name").getAsString().trim() : "";
        String token = msg.has("token") && !msg.get("token").isJsonNull() ? msg.get("token").getAsString() : null;
        Room room = gameService.getRoom(roomId);

        if (room == null) {
            gameService.sendToSession(session, error("room_not_found", ""));
            return;
        }

        if (name.isEmpty()) {
            gameService.sendToSession(session, error("name_required", "Name is required"));
            return;
        }
        if (name.length() > 10) {
            gameService.sendToSession(session, error("name_too_long", "Name is too long"));
            return;
        }

        String playerId = room.getPidByName().get(name.toLowerCase());
        Player player;

        if (playerId != null) {
            // Re-join
            player = room.getPlayer(playerId);
            if (token != null && token.equals(player.getToken())) {
                player.setSession(session);
                player.setOnline(true);
            } else {
                gameService.sendToSession(session, error("invalid_token", "Invalid token for user " + name));
                return;
            }
        } else {
            // New join
            if (room.getPhase() != null && !room.getPhase().equals(Room.PHASE_LOBBY)) {
                gameService.sendToSession(session, error("game_started", "Game already started — cannot join"));
                return;
            }
            if (room.getPlayers().size() >= 6) {
                gameService.sendToSession(session, error("room_full", "Room is full (max 6 players)"));
                return;
            }
            playerId = UUID.randomUUID().toString();
            token = UUID.randomUUID().toString();
            player = new Player(playerId, token, name, session, Map.of());
            room.addPlayer(player);
        }

        JsonObject openRoom = new JsonObject();
        openRoom.addProperty("type", "open_room");
        openRoom.addProperty("room_id", room.getRoomId());
        openRoom.addProperty("player_id", player.getId());
        openRoom.addProperty("token", player.getToken());
        gameService.sendToSession(session, openRoom);

        gameService.broadcastUpdate(room);
    }
}
