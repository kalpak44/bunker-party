package com.bunkerparty.websocket;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.bunkerparty.websocket.handler.ConfirmHandler;
import com.bunkerparty.websocket.handler.DiscardHandler;
import com.bunkerparty.websocket.handler.JoinGameHandler;
import com.bunkerparty.websocket.handler.LeaveGameHandler;
import com.bunkerparty.websocket.handler.NewGameHandler;
import com.bunkerparty.websocket.handler.ReadyHandler;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.Map;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameWebSocketHandlerTest {

  private GameWebSocketHandler webSocketHandler;
  private GameService gameService;
  private NewGameHandler newGameHandler;

  @BeforeEach
  void setUp() {
    gameService = mock(GameService.class);
    newGameHandler = mock(NewGameHandler.class);
    JoinGameHandler joinGameHandler = mock(JoinGameHandler.class);
    LeaveGameHandler leaveGameHandler = mock(LeaveGameHandler.class);
    ReadyHandler readyHandler = mock(ReadyHandler.class);
    DiscardHandler discardHandler = mock(DiscardHandler.class);
    ConfirmHandler confirmHandler = mock(ConfirmHandler.class);

    webSocketHandler =
        new GameWebSocketHandler(
            newGameHandler,
            joinGameHandler,
            leaveGameHandler,
            readyHandler,
            discardHandler,
            confirmHandler,
            gameService);
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

    verify(gameService)
        .sendToSession(eq(session), argThat(json -> json.get("type").getAsString().equals("pong")));
  }

  @Test
  void shouldHandleDisconnect() {
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
