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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReadyHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReadyHandler.class);
    private final RoomManager roomManager;
    private final WebSocketJsonSender sender;

    private static final Map<String, Integer> CARD_COUNTS = Map.of(
            "profession", 19,
            "health", 10,
            "age", 10,
            "gender", 6,
            "hobby", 10,
            "phobia", 7,
            "item", 9
    );
    private static final int BUNKER_COUNT = 30;

    @Inject
    public ReadyHandler(RoomManager roomManager, WebSocketJsonSender sender) {
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

        room.addStartVote(playerId);
        room.addLog(new LogEntry("ready", Map.of("name", player.getName())));

        logger.info("Player {} is ready in room {}", player.getName(), roomId);

        checkStart(room);
        notifyUpdate(room);
    }

    private void checkStart(Room room) {
        int playerCount = room.getPlayers().size();
        if (playerCount >= 3 && playerCount <= 6 && room.getStartVotes().size() == playerCount) {
            distributeCards(room);
            room.setPhase(Room.PHASE_REVEAL);
            room.setRound(1);
            room.addLog(new LogEntry("game_started", Map.of()));
            logger.info("Game started in room {}", room.getRoomId());
        }
    }

    private void distributeCards(Room room) {
        List<Player> players = new ArrayList<>(room.getPlayers().values());
        Collections.shuffle(players);

        for (Map.Entry<String, Integer> entry : CARD_COUNTS.entrySet()) {
            String category = entry.getKey();
            int count = entry.getValue();
            List<Integer> indices = IntStream.range(0, count).boxed().collect(Collectors.toList());
            Collections.shuffle(indices);

            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                Map<String, Integer> pCards = p.getCharacterIndices();
                pCards.put(category, indices.get(i));
                p.setCharacterIndices(pCards);
            }
        }

        Random rand = new Random();
        room.setEventIdx(rand.nextInt(BUNKER_COUNT));
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

        JsonArray startVotes = new JsonArray();
        room.getStartVotes().forEach(startVotes::add);
        update.add("startVotes", startVotes);

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

        List<String> notReady = room.getPlayers().values().stream()
                .filter(p -> !room.getStartVotes().contains(p.getId()))
                .map(Player::getName)
                .collect(Collectors.toList());

        if (!notReady.isEmpty()) {
            logger.info("Still waiting for: {}", String.join(", ", notReady));
        }

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
