package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ConfirmHandlerTest {

    private GameService gameService;
    private ConfirmHandler handler;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        handler = new ConfirmHandler(gameService);
    }

    @Test
    void shouldConfirmAndTransitionToNextRound() {
        Room room = new Room("1234");
        room.setPhase(Room.PHASE_CONFIRM);
        room.setRound(1);
        Player p1 = new Player("p1", "t1", "Alice", null, Map.of("p", 1, "h", 2));
        room.addPlayer(p1);
        p1.revealCard("p"); // Player has more cards to reveal
        when(gameService.getRoom("1234")).thenReturn(room);

        JsonObject msg = new JsonObject();
        msg.addProperty("roomId", "1234");
        msg.addProperty("playerId", "p1");

        handler.handle(null, msg);

        assertEquals(2, room.getRound());
        assertEquals(Room.PHASE_REVEAL, room.getPhase());
        assertTrue(room.getRoundConfirms().isEmpty());
        verify(gameService).broadcastUpdate(room);
    }

    @Test
    void shouldGameOverWhenAllCardsUsed() {
        Room room = new Room("1234");
        room.setPhase(Room.PHASE_CONFIRM);
        room.setRound(1);
        Player p1 = new Player("p1", "t1", "Alice", null, Map.of("p", 1));
        room.addPlayer(p1);
        p1.revealCard("p"); // All cards (1) revealed
        when(gameService.getRoom("1234")).thenReturn(room);

        JsonObject msg = new JsonObject();
        msg.addProperty("roomId", "1234");
        msg.addProperty("playerId", "p1");

        // TOTAL_CARD_TYPES in ConfirmHandler is 7, but for this test we can see how allPlayersUsedAllCards behaves
        // Actually, ConfirmHandler has private static final int TOTAL_CARD_TYPES = 7;
        // To trigger game over, player must have used 7 cards or we can adjust expectation.
        // Let's mock player to return true for hasRevealedAllCards(7)
        
        // Wait, p1 is a real object. Let's give it 7 cards.
        Map<String, Integer> cards = Map.of(
                "profession", 1, "health", 2, "age", 3,
                "gender", 4, "hobby", 5, "phobia", 6, "item", 7
        );
        Player p7 = new Player("p1", "t1", "Alice", null, cards);
        cards.keySet().forEach(p7::revealCard);
        room.getPlayers().clear();
        room.getPidByName().clear();
        room.addPlayer(p7);
        p7.setOnline(true);

        handler.handle(null, msg);

        assertEquals(Room.PHASE_GAME_OVER, room.getPhase());
    }
}
