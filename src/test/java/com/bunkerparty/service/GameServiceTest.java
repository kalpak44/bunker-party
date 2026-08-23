package com.bunkerparty.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

  @Test
  void shouldDelegateGetAllRooms() {
    Collection<Room> rooms = List.of(new Room("1234"));
    when(roomManager.getAllRooms()).thenReturn(rooms);

    Collection<Room> result = gameService.getAllRooms();

    assertEquals(rooms, result);
    verify(roomManager).getAllRooms();
  }

  @Test
  void shouldIncludeEventIdxInBroadcastWhenSet() throws IOException {
    Room room = new Room("1234");
    room.setRound(1);
    room.setEventIdx(7);
    Session session = mock(Session.class);
    when(session.isOpen()).thenReturn(true);
    room.addPlayer(new Player("p1", "t1", "Alice", session, Map.of("profession", 1)));

    gameService.broadcastUpdate(room);

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(sender).send(eq(session), captor.capture());
    assertEquals(7, captor.getValue().get("eventIdx").getAsInt());
  }

  @Test
  void shouldIncludeRevealedRoundHistoryInBroadcast() throws IOException {
    Room room = new Room("1234");
    room.setRound(1);
    room.setEventIdx(3);
    Session session = mock(Session.class);
    when(session.isOpen()).thenReturn(true);
    room.addPlayer(new Player("p1", "t1", "Alice", session, Map.of("profession", 1)));
    room.addRoundReveal("p1", "profession");

    gameService.broadcastUpdate(room);

    ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
    verify(sender).send(eq(session), captor.capture());
    JsonObject history = captor.getValue().getAsJsonObject("history");
    assertEquals(3, history.getAsJsonObject("1").get("eventIdx").getAsInt());
  }

  @Test
  void shouldCatchAndLogWhenBroadcastSendFails() throws IOException {
    Room room = new Room("1234");
    Session session = mock(Session.class);
    when(session.isOpen()).thenReturn(true);
    room.addPlayer(new Player("p1", "t1", "Alice", session, Map.of("profession", 1)));
    doThrow(new IOException("boom")).when(sender).send(eq(session), any(JsonObject.class));

    gameService.broadcastUpdate(room);
  }

  @Test
  void shouldCatchAndLogWhenSendToSessionFails() throws IOException {
    Session session = mock(Session.class);
    doThrow(new IOException("boom")).when(sender).send(eq(session), any(JsonObject.class));

    gameService.sendToSession(session, new JsonObject());
  }
}
