package com.bunkerparty.websocket.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NewGameHandlerTest {

  private GameService gameService;
  private NewGameHandler handler;

  @BeforeEach
  void setUp() {
    gameService = mock(GameService.class);
    handler = new NewGameHandler(gameService);
  }

  @Test
  void shouldCreateNewGameSuccessfully() {
    Session session = mock(Session.class);
    JsonObject msg = new JsonObject();
    msg.addProperty("name", "Alice");
    Room room = new Room("1234");
    when(gameService.createRoom()).thenReturn(room);

    handler.handle(session, msg);

    verify(gameService).createRoom();
    assertEquals(1, room.getPlayers().size());
    assertEquals("Alice", room.getPlayers().values().iterator().next().getName());

    ArgumentCaptor<JsonObject> openRoomCaptor = ArgumentCaptor.forClass(JsonObject.class);
    verify(gameService).sendToSession(eq(session), openRoomCaptor.capture());
    assertEquals("open_room", openRoomCaptor.getValue().get("type").getAsString());

    verify(gameService).broadcastUpdate(room);
  }

  @Test
  void shouldFailIfNameIsMissing() {
    Session session = mock(Session.class);
    JsonObject msg = new JsonObject();
    msg.addProperty("name", "");

    handler.handle(session, msg);

    verify(gameService, never()).createRoom();
    verify(gameService).sendToSession(eq(session), any(JsonObject.class));
  }
}
