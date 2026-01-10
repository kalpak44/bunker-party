package com.bunkerparty.service;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameServiceTest {

    private RoomManager roomManager;
    private WebSocketJsonSender sender;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        roomManager = mock(RoomManager.class);
        sender = mock(WebSocketJsonSender.class);
        gameService = new GameService(roomManager, sender);
    }

    @Test
    void shouldDelegateCreateRoom() {
        Room room = new Room("1234");
        when(roomManager.createRoom()).thenReturn(room);

        Room result = gameService.createRoom();

        assertEquals(room, result);
        verify(roomManager).createRoom();
    }

    @Test
    void shouldDelegateGetRoom() {
        Room room = new Room("1234");
        when(roomManager.getRoom("1234")).thenReturn(room);

        Room result = gameService.getRoom("1234");

        assertEquals(room, result);
        verify(roomManager).getRoom("1234");
    }

    @Test
    void shouldBroadcastUpdateToOnlinePlayers() throws IOException {
        Room room = new Room("1234");
        Session session = mock(Session.class);
        when(session.isOpen()).thenReturn(true);
        Player player = new Player("p1", "t1", "Alice", session, Map.of("profession", 1));
        room.addPlayer(player);

        gameService.broadcastUpdate(room);

        ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        verify(sender).send(eq(session), captor.capture());
        JsonObject sentJson = captor.getValue();
        assertEquals("game_update", sentJson.get("type").getAsString());
        assertEquals(1, sentJson.getAsJsonObject("myCards").get("profession").getAsInt());
    }

    @Test
    void shouldSendToSession() throws IOException {
        Session session = mock(Session.class);
        JsonObject msg = new JsonObject();
        msg.addProperty("test", "value");

        gameService.sendToSession(session, msg);

        verify(sender).send(session, msg);
    }
}
