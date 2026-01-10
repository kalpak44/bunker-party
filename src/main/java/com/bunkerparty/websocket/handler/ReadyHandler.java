package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReadyHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReadyHandler.class);
    private final GameService gameService;

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
    public ReadyHandler(GameService gameService) {
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

        room.addStartVote(playerId);

        logger.info("Player {} is ready in room {}", player.getName(), roomId);

        checkStart(room);
        gameService.broadcastUpdate(room);
    }

    private void checkStart(Room room) {
        int playerCount = room.getPlayers().size();
        if (playerCount >= 3 && playerCount <= 6 && room.getStartVotes().size() == playerCount) {
            room.setRound(1);
            distributeCards(room);
            room.setPhase(Room.PHASE_REVEAL);
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
}
