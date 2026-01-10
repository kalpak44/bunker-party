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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class JoinGameHandlerTest {

    private GameService gameService;
    private JoinGameHandler handler;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        handler = new JoinGameHandler(gameService);
    }

    @Test
    void shouldJoinNewPlayerSuccessfully() throws Exception {
        Session session = mock(Session.class);
        Room room = new Room("1234");
        JsonObject msg = new JsonObject();
        msg.addProperty("roomId", "1234");
        msg.addProperty("name", "Bob");
        when(gameService.getRoom("1234")).thenReturn(room);

        handler.handle(session, msg);

        assertEquals(1, room.getPlayers().size());
        verify(gameService).broadcastUpdate(room);
        verify(gameService).sendToSession(eq(session), argThat(json -> json.get("type").getAsString().equals("open_room")));
    }

    @Test
    void shouldRejoinExistingPlayerWithCorrectToken() throws Exception {
        Session session = mock(Session.class);
        Room room = new Room("1234");
        Player existing = new Player("p1", "token123", "Bob", null, Map.of());
        room.addPlayer(existing);
        existing.setOnline(false);

        JsonObject msg = new JsonObject();
        msg.addProperty("roomId", "1234");
        msg.addProperty("name", "Bob");
        msg.addProperty("token", "token123");
        when(gameService.getRoom("1234")).thenReturn(room);

        handler.handle(session, msg);

        assertTrue(existing.isOnline());
        assertEquals(session, existing.getSession());
        verify(gameService).broadcastUpdate(room);
    }

    @Test
    void shouldFailRejoinWithWrongToken() throws Exception {
        Session session = mock(Session.class);
        Room room = new Room("1234");
        Player existing = new Player("p1", "token123", "Bob", null, Map.of());
        room.addPlayer(existing);

        JsonObject msg = new JsonObject();
        msg.addProperty("roomId", "1234");
        msg.addProperty("name", "Bob");
        msg.addProperty("token", "wrong");
        when(gameService.getRoom("1234")).thenReturn(room);

        handler.handle(session, msg);

        verify(gameService).sendToSession(eq(session), argThat(json -> 
            json.get("type").getAsString().equals("error") && 
            json.get("code").getAsString().equals("invalid_token")));
    }

    @Test
    void shouldFailIfRoomNotFound() throws Exception {
        Session session = mock(Session.class);
        JsonObject msg = new JsonObject();
        msg.addProperty("roomId", "9999");
        when(gameService.getRoom("9999")).thenReturn(null);

        handler.handle(session, msg);

        verify(gameService).sendToSession(eq(session), argThat(json -> json.get("code").getAsString().equals("room_not_found")));
    }
}
