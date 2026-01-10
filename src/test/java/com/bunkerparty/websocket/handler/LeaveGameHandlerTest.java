package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class LeaveGameHandlerTest {

    private GameService gameService;
    private LeaveGameHandler handler;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        handler = new LeaveGameHandler(gameService);
    }

    @Test
    void shouldRemovePlayerFromRoomOnLeave() {
        Session session = mock(Session.class);
        Room room = new Room("1234");
        Player player = new Player("p1", "t1", "Alice", session, Map.of());
        room.addPlayer(player);
        JsonObject msg = new JsonObject();
        msg.addProperty("roomId", "1234");
        when(gameService.getRoom("1234")).thenReturn(room);

        handler.handle(session, msg);

        assertFalse(room.getPlayers().containsKey("p1"));
        assertFalse(player.isOnline());
        verify(gameService).broadcastUpdate(room);
    }
}
