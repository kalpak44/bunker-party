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

public class ReadyHandler extends BaseMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReadyHandler.class);
    private static final int MIN_PLAYERS = 3;
    private static final int MAX_PLAYERS = 6;
    private static final int INITIAL_ROUND = 1;

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
        super(gameService);
    }

    /**
     * Handles the "ready" message to vote to start the game.
     */
    @Override
    public void handle(Session session, JsonObject msg) {
        Room room = getRoom(msg);
        if (room == null) return;

        Player player = getPlayer(room, msg);
        if (player == null) return;

        room.addStartVote(player.getId());

        logger.info("Player {} is ready in room {}", player.getName(), room.getRoomId());

        checkStart(room);
        gameService.broadcastUpdate(room);
    }

    private void checkStart(Room room) {
        int playerCount = room.getPlayers().size();
        if (playerCount >= MIN_PLAYERS && playerCount <= MAX_PLAYERS && room.getStartVotes().size() == playerCount) {
            room.setRound(INITIAL_ROUND);
            distributeCards(room);
            room.setPhase(Room.PHASE_REVEAL);
            logger.info("Game started in room {}", room.getRoomId());
        }
    }

    private void distributeCards(Room room) {
        List<Player> players = new ArrayList<>(room.getPlayers().values());
        Collections.shuffle(players);

        for (Map.Entry<String, Integer> entry : CARD_COUNTS.entrySet()) {
            distributeCategoryCards(players, entry.getKey(), entry.getValue());
        }

        Random rand = new Random();
        room.setEventIdx(rand.nextInt(BUNKER_COUNT));
    }

    private void distributeCategoryCards(List<Player> players, String category, int count) {
        List<Integer> indices = IntStream.range(0, count).boxed().collect(Collectors.toList());
        Collections.shuffle(indices);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Map<String, Integer> pCards = p.getCharacterIndices();
            pCards.put(category, indices.get(i));
            p.setCharacterIndices(pCards);
        }
    }
}
