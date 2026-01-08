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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiscardHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiscardHandler.class);
    private final RoomManager roomManager;
    private final WebSocketJsonSender sender;

    @Inject
    public DiscardHandler(RoomManager roomManager, WebSocketJsonSender sender) {
        this.roomManager = roomManager;
        this.sender = sender;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws Exception {
        String roomId = msg.has("roomId") ? msg.get("roomId").getAsString() : "";
        String playerId = msg.has("playerId") ? msg.get("playerId").getAsString() : "";
        String cardKey = msg.has("cardKey") ? msg.get("cardKey").getAsString() : "";

        Room room = roomManager.getRoom(roomId);
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
        room.addLog(new LogEntry("discard", Map.of("name", player.getName(), "card", cardKey)));

        if (room.allActivePlayersRevealed()) {
            room.setPhase(Room.PHASE_CONFIRM);
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

        JsonObject roundReveals = new JsonObject();
        room.getRoundReveals().forEach(roundReveals::addProperty);
        update.add("roundReveals", roundReveals);

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
