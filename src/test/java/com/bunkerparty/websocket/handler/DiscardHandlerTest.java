package com.bunkerparty.websocket.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import java.util.Map;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiscardHandlerTest {

  private GameService gameService;
  private DiscardHandler handler;

  @BeforeEach
  void setUp() {
    gameService = mock(GameService.class);
    handler = new DiscardHandler(gameService);
  }

  private static JsonObject discardMsg(String roomId, String playerId, String cardKey) {
    JsonObject msg = new JsonObject();
    msg.addProperty("roomId", roomId);
    msg.addProperty("playerId", playerId);
    msg.addProperty("cardKey", cardKey);
    return msg;
  }

  @Test
  void shouldReturn_whenRoomIsNull() {
    when(gameService.getRoom("nope")).thenReturn(null);

    handler.handle(mock(Session.class), discardMsg("nope", "p1", "prof"));

    verify(gameService, never()).broadcastUpdate(any());
  }

  @Test
  void shouldReturn_whenPlayerIsNull() {
    Room room = new Room("1234");
    room.setPhase(Room.PHASE_REVEAL);
    when(gameService.getRoom("1234")).thenReturn(room);

    // playerId doesn't exist in room
    handler.handle(mock(Session.class), discardMsg("1234", "missing", "prof"));

    verify(gameService, never()).broadcastUpdate(any());
    assertTrue(room.getRoundReveals().isEmpty(), "No reveal should be recorded");
  }

  @Test
  void shouldReturn_whenPhaseIsNotReveal() {
    Room room = new Room("1234");
    room.setPhase(Room.PHASE_CONFIRM); // wrong phase
    Player p1 = new Player("p1", "t1", "Alice", null, Map.of("prof", 1));
    room.addPlayer(p1);
    when(gameService.getRoom("1234")).thenReturn(room);

    handler.handle(null, discardMsg("1234", "p1", "prof"));

    verify(gameService, never()).broadcastUpdate(any());
    assertFalse(p1.hasUsedKey("prof"), "Card should not be revealed in wrong phase");
    assertTrue(room.getRoundReveals().isEmpty(), "No reveal should be recorded");
  }

  @Test
  void shouldReturn_whenPlayerAlreadyUsedCardKey() {
    Room room = new Room("1234");
    room.setPhase(Room.PHASE_REVEAL);
    Player p1 = new Player("p1", "t1", "Alice", null, Map.of("prof", 1));
    room.addPlayer(p1);
    when(gameService.getRoom("1234")).thenReturn(room);

    // Pre-condition: player already used this key
    p1.revealCard("prof");
    assertTrue(p1.hasUsedKey("prof"));

    handler.handle(null, discardMsg("1234", "p1", "prof"));

    verify(gameService, never()).broadcastUpdate(any());
    assertTrue(room.getRoundReveals().isEmpty(), "No reveal should be recorded");
  }

  @Test
  void shouldReturn_whenPlayerAlreadyDiscardedThisRound() {
    Room room = new Room("1234");
    room.setPhase(Room.PHASE_REVEAL);
    Player p1 = new Player("p1", "t1", "Alice", null, Map.of("prof", 1, "bio", 1));
    room.addPlayer(p1);
    when(gameService.getRoom("1234")).thenReturn(room);

    // Pre-condition: already revealed something this round
    room.addRoundReveal("p1", "bio");
    assertTrue(room.getRoundReveals().containsKey("p1"));

    handler.handle(null, discardMsg("1234", "p1", "prof"));

    verify(gameService, never()).broadcastUpdate(any());
    assertFalse(p1.hasUsedKey("prof"), "Second discard should be blocked");
    assertEquals("bio", room.getRoundReveals().get("p1"), "Round reveal should remain unchanged");
  }

  @Test
  void shouldRevealCard_andBroadcast_butNotTransition_whenNotAllPlayersRevealed() {
    Room room = spy(new Room("1234"));
    room.setPhase(Room.PHASE_REVEAL);
    Player p1 = new Player("p1", "t1", "Alice", null, Map.of("prof", 1));
    room.addPlayer(p1);

    // Force "not all active players revealed"
    doReturn(false).when(room).allActivePlayersRevealed();

    when(gameService.getRoom("1234")).thenReturn(room);

    handler.handle(null, discardMsg("1234", "p1", "prof"));

    assertTrue(p1.hasUsedKey("prof"));
    assertEquals("prof", room.getRoundReveals().get("p1"));
    assertEquals(Room.PHASE_REVEAL, room.getPhase(), "Phase should remain REVEAL");
    verify(gameService, times(1)).broadcastUpdate(room);
  }

  @Test
  void shouldRevealCard_andTransitionToConfirm_andBroadcast_whenAllPlayersRevealed() {
    Room room = spy(new Room("1234"));
    room.setPhase(Room.PHASE_REVEAL);
    Player p1 = new Player("p1", "t1", "Alice", null, Map.of("prof", 1));
    room.addPlayer(p1);

    // Force "all active players revealed"
    doReturn(true).when(room).allActivePlayersRevealed();

    when(gameService.getRoom("1234")).thenReturn(room);

    handler.handle(null, discardMsg("1234", "p1", "prof"));

    assertTrue(p1.hasUsedKey("prof"));
    assertEquals("prof", room.getRoundReveals().get("p1"));
    assertEquals(Room.PHASE_CONFIRM, room.getPhase(), "Phase should transition to CONFIRM");
    verify(gameService, times(1)).broadcastUpdate(room);
  }
}
