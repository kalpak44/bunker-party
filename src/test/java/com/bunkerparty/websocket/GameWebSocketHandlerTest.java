package com.bunkerparty.websocket;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.bunkerparty.websocket.handler.*;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

class GameWebSocketHandlerTest {

    private GameWebSocketHandler webSocketHandler;
    private GameService gameService;
    private NewGameHandler newGameHandler;
    private JoinGameHandler joinGameHandler;
    private LeaveGameHandler leaveGameHandler;
    private ReadyHandler readyHandler;
    private DiscardHandler discardHandler;
    private ConfirmHandler confirmHandler;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        newGameHandler = mock(NewGameHandler.class);
        joinGameHandler = mock(JoinGameHandler.class);
        leaveGameHandler = mock(LeaveGameHandler.class);
        readyHandler = mock(ReadyHandler.class);
        discardHandler = mock(DiscardHandler.class);
        confirmHandler = mock(ConfirmHandler.class);

        webSocketHandler = new GameWebSocketHandler(
                newGameHandler, joinGameHandler, leaveGameHandler,
                readyHandler, discardHandler, confirmHandler, gameService
        );
    }

    @Test
    void shouldDispatchMessageToCorrectHandler() throws Exception {
        Session session = mock(Session.class);
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "new_game");

        webSocketHandler.onMessage(session, msg.toString());

        verify(newGameHandler).handle(eq(session), any(JsonObject.class));
    }

    @Test
    void shouldHandlePingMessage() throws Exception {
        Session session = mock(Session.class);
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "ping");

        webSocketHandler.onMessage(session, msg.toString());

        verify(gameService).sendToSession(eq(session), argThat(json -> json.get("type").getAsString().equals("pong")));
    }

    @Test
    void shouldHandleDisconnect() throws Exception {
        Session session = mock(Session.class);
        Room room = new Room("1234");
        Player player = new Player("p1", "t1", "Alice", session, Map.of());
        room.addPlayer(player);
        when(gameService.getAllRooms()).thenReturn(Collections.singletonList(room));

        webSocketHandler.onClose(session, 1000, "Normal closure");

        assert !player.isOnline();
        verify(gameService).broadcastUpdate(room);
    }
}
