package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscardHandler extends BaseMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiscardHandler.class);

    @Inject
    public DiscardHandler(GameService gameService) {
        super(gameService);
    }

    /**
     * Handles the "discard" message to reveal a card.
     */
    @Override
    public void handle(Session session, JsonObject msg) {
        String cardKey = getString(msg, "cardKey");

        Room room = getRoom(msg);
        if (room == null) return;

        Player player = getPlayer(room, msg);
        if (player == null) return;

        if (!Room.PHASE_REVEAL.equals(room.getPhase())) {
            logger.warn("Discard attempt in wrong phase: {} for room {}", room.getPhase(), room.getRoomId());
            return;
        }

        if (player.hasUsedKey(cardKey)) {
            logger.warn("Player {} already used card {} in room {}", player.getName(), cardKey, room.getRoomId());
            return;
        }

        if (room.getRoundReveals().containsKey(player.getId())) {
            logger.warn("Player {} already discarded a card this round in room {}", player.getName(), room.getRoomId());
            return;
        }

        player.revealCard(cardKey);
        room.addRoundReveal(player.getId(), cardKey);

        if (room.allActivePlayersRevealed()) {
            room.setPhase(Room.PHASE_CONFIRM);
        }

        gameService.broadcastUpdate(room);
    }
}
