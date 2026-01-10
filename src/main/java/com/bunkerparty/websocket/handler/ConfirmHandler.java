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

public class ConfirmHandler extends BaseMessageHandler {
    private final Random random;

    private static final Logger logger = LoggerFactory.getLogger(ConfirmHandler.class);
    private static final int BUNKER_COUNT = 30;
    private static final int TOTAL_CARD_TYPES = 7;
    private static final int MAX_EVENT_ATTEMPTS = 100;

    @Inject
    public ConfirmHandler(GameService gameService, Random random) {
        super(gameService);
        this.random = random;
    }

    /**
     * Handles the "confirm" message to confirm the end of a round.
     */
    @Override
    public void handle(Session session, JsonObject msg) {
        Room room = getRoom(msg);
        if (room == null) return;

        Player player = getPlayer(room, msg);
        if (player == null) return;

        if (!Room.PHASE_CONFIRM.equals(room.getPhase())) {
            logger.warn("Confirm attempt in wrong phase: {} for room {}", room.getPhase(), room.getRoomId());
            return;
        }

        room.addRoundConfirm(player.getId());

        if (room.allActivePlayersConfirmed()) {
            handleRoundTransition(room);
        }

        gameService.broadcastUpdate(room);
    }

    private void handleRoundTransition(Room room) {
        if (room.allPlayersUsedAllCards(TOTAL_CARD_TYPES)) {
            room.setPhase(Room.PHASE_GAME_OVER);
        } else {
            room.incrementRound();
            room.clearRoundReveals();
            room.clearRoundConfirms();

            room.setEventIdx(pickUniqueEventIndex(room));
            room.setPhase(Room.PHASE_REVEAL);
        }
    }

    private int pickUniqueEventIndex(Room room) {
        int newEventIdx;
        int attempts = 0;
        do {
            newEventIdx = random.nextInt(BUNKER_COUNT);
            attempts++;
        } while (room.getEventByRound().containsValue(newEventIdx) && attempts < MAX_EVENT_ATTEMPTS);
        return newEventIdx;
    }
}
