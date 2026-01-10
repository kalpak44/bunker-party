package com.bunkerparty.websocket.handler;

import com.bunkerparty.manager.RoomManager;
import com.bunkerparty.model.LogEntry;
import com.bunkerparty.model.Player;
import com.bunkerparty.model.Room;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

public class ConfirmHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmHandler.class);
    private final RoomManager roomManager;
    private final WebSocketJsonSender sender;
    private static final int BUNKER_COUNT = 30;
    private static final int TOTAL_CARD_TYPES = 7;

    @Inject
    public ConfirmHandler(RoomManager roomManager, WebSocketJsonSender sender) {
        this.roomManager = roomManager;
        this.sender = sender;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws Exception {
        String roomId = msg.has("roomId") ? msg.get("roomId").getAsString() : "";
        String playerId = msg.has("playerId") ? msg.get("playerId").getAsString() : "";

        Room room = roomManager.getRoom(roomId);
        if (room == null) return;

        Player player = room.getPlayer(playerId);
        if (player == null) return;

        if (!Room.PHASE_CONFIRM.equals(room.getPhase())) {
            logger.warn("Confirm attempt in wrong phase: {} for room {}", room.getPhase(), roomId);
            return;
        }

        room.addRoundConfirm(playerId);
        room.addLog(new LogEntry("confirm", Map.of("name", player.getName())));

        if (room.allActivePlayersConfirmed()) {
            if (room.allPlayersUsedAllCards(TOTAL_CARD_TYPES)) {
                room.setPhase(Room.PHASE_GAME_OVER);
                room.addLog(new LogEntry("game_over", Map.of()));
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
                room.addLog(new LogEntry("next_round", Map.of("round", room.getRound())));
            }
        }

        notifyUpdate(room);
    }

    private void notifyUpdate(Room room) throws IOException {
        JsonObject update = new JsonObject();
        update.addProperty("type", "game_update");
        update.addProperty("phase", room.getPhase());
        update.addProperty("round", room.getRound());
        update.addProperty("roomId", room.getRoomId());

        if (room.getEventIdx() != null) {
            update.addProperty("eventIdx", room.getEventIdx());
        }

        JsonObject history = new JsonObject();
        room.getRevealedByRound().forEach((r, reveals) -> {
            JsonObject rObj = new JsonObject();
            rObj.addProperty("eventIdx", room.getEventByRound().get(r));
            JsonObject revealsObj = new JsonObject();
            reveals.forEach(revealsObj::addProperty);
            rObj.add("reveals", revealsObj);
            history.add(String.valueOf(r), rObj);
        });
        update.add("history", history);

        JsonArray playersArray = new JsonArray();
        for (Player p : room.getPlayers().values()) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("id", p.getId());
            pObj.addProperty("name", p.getName());
            pObj.addProperty("online", p.isOnline());
            pObj.addProperty("ready", room.getStartVotes().contains(p.getId()));

            JsonObject revealed = new JsonObject();
            p.getRevealedIndices().forEach(revealed::addProperty);
            pObj.add("revealed", revealed);

            playersArray.add(pObj);
        }
        update.add("players", playersArray);

        JsonArray roundConfirms = new JsonArray();
        room.getRoundConfirms().forEach(roundConfirms::add);
        update.add("roundConfirms", roundConfirms);

        for (Player p : room.getPlayers().values()) {
            if (p.getSession() != null && p.getSession().isOpen()) {
                JsonObject personalUpdate = update.deepCopy();
                JsonObject myCards = new JsonObject();
                p.getCharacterIndices().forEach(myCards::addProperty);
                personalUpdate.add("myCards", myCards);
                sender.send(p.getSession(), personalUpdate);
            }
        }
    }
}
