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

class DiscardHandlerTest {

    private GameService gameService;
    private DiscardHandler handler;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        handler = new DiscardHandler(gameService);
    }

    @Test
    void shouldRevealCardAndTransitionToConfirmPhase() {
        Room room = new Room("1234");
        room.setPhase(Room.PHASE_REVEAL);
        Player p1 = new Player("p1", "t1", "Alice", null, Map.of("prof", 1));
        room.addPlayer(p1);
        when(gameService.getRoom("1234")).thenReturn(room);

        JsonObject msg = new JsonObject();
        msg.addProperty("roomId", "1234");
        msg.addProperty("playerId", "p1");
        msg.addProperty("cardKey", "prof");

        handler.handle(null, msg);

        assertTrue(p1.hasUsedKey("prof"));
        assertEquals("prof", room.getRoundReveals().get("p1"));
        assertEquals(Room.PHASE_CONFIRM, room.getPhase());
        verify(gameService).broadcastUpdate(room);
    }
}
