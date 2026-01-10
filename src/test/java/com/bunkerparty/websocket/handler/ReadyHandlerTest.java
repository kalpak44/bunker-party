package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReadyHandlerTest {

    private GameService gameService;
    private ReadyHandler handler;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        handler = new ReadyHandler(gameService);
    }

    @Test
    void shouldTrackVotesAndStartGameWhenAllReady() {
        Room room = new Room("1234");
        Player p1 = new Player("p1", "t1", "Alice", null, Map.of());
        Player p2 = new Player("p2", "t2", "Bob", null, Map.of());
        Player p3 = new Player("p3", "t3", "Charlie", null, Map.of());
        room.addPlayer(p1);
        room.addPlayer(p2);
        room.addPlayer(p3);
        
        when(gameService.getRoom("1234")).thenReturn(room);

        handler.handle(null, createReadyMsg("p1", "1234"));
        handler.handle(null, createReadyMsg("p2", "1234"));
        handler.handle(null, createReadyMsg("p3", "1234"));

        assertEquals(3, room.getStartVotes().size());
        assertEquals(Room.PHASE_REVEAL, room.getPhase());
        assertEquals(1, room.getRound());
        verify(gameService, times(3)).broadcastUpdate(room);
    }

    private JsonObject createReadyMsg(String pid, String rid) {
        JsonObject msg = new JsonObject();
        msg.addProperty("playerId", pid);
        msg.addProperty("roomId", rid);
        return msg;
    }
}
