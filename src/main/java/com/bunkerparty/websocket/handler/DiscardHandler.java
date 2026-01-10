package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscardHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiscardHandler.class);
    private final GameService gameService;

    @Inject
    public DiscardHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws Exception {
        String roomId = msg.has("roomId") ? msg.get("roomId").getAsString() : "";
        String playerId = msg.has("playerId") ? msg.get("playerId").getAsString() : "";
        String cardKey = msg.has("cardKey") ? msg.get("cardKey").getAsString() : "";

        Room room = gameService.getRoom(roomId);
        if (room == null) return;

        Player player = room.getPlayer(playerId);
        if (player == null) return;

        if (!Room.PHASE_REVEAL.equals(room.getPhase())) {
            logger.warn("Discard attempt in wrong phase: {} for room {}", room.getPhase(), roomId);
            return;
        }

        if (player.hasUsedKey(cardKey)) {
            logger.warn("Player {} already used card {} in room {}", player.getName(), cardKey, roomId);
            return;
        }

        if (room.getRoundReveals().containsKey(playerId)) {
            logger.warn("Player {} already discarded a card this round in room {}", player.getName(), roomId);
            return;
        }

        player.revealCard(cardKey);
        room.addRoundReveal(playerId, cardKey);

        if (room.allActivePlayersRevealed()) {
            room.setPhase(Room.PHASE_CONFIRM);
        }

        gameService.broadcastUpdate(room);
    }
}
