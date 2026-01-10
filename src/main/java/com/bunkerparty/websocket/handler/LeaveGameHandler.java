package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaveGameHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(LeaveGameHandler.class);
    private final GameService gameService;

    @Inject
    public LeaveGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws Exception {
        String roomId = msg.has("roomId") && !msg.get("roomId").isJsonNull() ? msg.get("roomId").getAsString() : null;
        if (roomId == null) return;

        Room room = gameService.getRoom(roomId);
        if (room == null) return;

        boolean found = false;
        for (Player player : room.getPlayers().values()) {
            if (session.equals(player.getSession())) {
                player.setOnline(false);
                room.getPlayers().remove(player.getId());
                room.getPidByName().remove(player.getName().toLowerCase());
                found = true;
                break;
            }
        }

        if (found) {
            gameService.broadcastUpdate(room);
        }
    }
}
