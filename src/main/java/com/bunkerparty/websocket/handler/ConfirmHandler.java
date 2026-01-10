package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class ConfirmHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmHandler.class);
    private final GameService gameService;
    private static final int BUNKER_COUNT = 30;
    private static final int TOTAL_CARD_TYPES = 7;

    @Inject
    public ConfirmHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws Exception {
        String roomId = msg.has("roomId") ? msg.get("roomId").getAsString() : "";
        String playerId = msg.has("playerId") ? msg.get("playerId").getAsString() : "";

        Room room = gameService.getRoom(roomId);
        if (room == null) return;

        Player player = room.getPlayer(playerId);
        if (player == null) return;

        if (!Room.PHASE_CONFIRM.equals(room.getPhase())) {
            logger.warn("Confirm attempt in wrong phase: {} for room {}", room.getPhase(), roomId);
            return;
        }

        room.addRoundConfirm(playerId);

        if (room.allActivePlayersConfirmed()) {
            if (room.allPlayersUsedAllCards(TOTAL_CARD_TYPES)) {
                room.setPhase(Room.PHASE_GAME_OVER);
            } else {
                room.incrementRound();
                room.clearRoundReveals();
                room.clearRoundConfirms();

                Random rand = new Random();
                int newEventIdx;
                int attempts = 0;
                do {
                    newEventIdx = rand.nextInt(BUNKER_COUNT);
                    attempts++;
                } while (room.getEventByRound().containsValue(newEventIdx) && attempts < 100);

                room.setEventIdx(newEventIdx);

                room.setPhase(Room.PHASE_REVEAL);
            }
        }

        gameService.broadcastUpdate(room);
    }
}
