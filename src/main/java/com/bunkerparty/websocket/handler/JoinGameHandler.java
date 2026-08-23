package com.bunkerparty.websocket.handler;

import static com.bunkerparty.websocket.helpers.JsonUtils.error;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.eclipse.jetty.websocket.api.Session;

public class JoinGameHandler extends BaseMessageHandler {
  private static final int MAX_PLAYERS = 6;
  private static final String TOKEN = "token";

  @Inject
  public JoinGameHandler(GameService gameService) {
    super(gameService);
  }

  /** Handles the "join_game" message to join or rejoin a room. */
  @Override
  public void handle(Session session, JsonObject msg) {
    String roomId = getString(msg, "roomId");
    String name = getString(msg, "name").trim();
    String token =
        msg.has(TOKEN) && !msg.get(TOKEN).isJsonNull() ? msg.get(TOKEN).getAsString() : null;
    Room room = gameService.getRoom(roomId);

    if (room == null) {
      gameService.sendToSession(session, error("room_not_found", ""));
      return;
    }

    if (!validateName(session, name)) {
      return;
    }

    processPlayerJoin(session, room, name, token);
  }

  private void processPlayerJoin(Session session, Room room, String name, String token) {
    String playerId = room.getPidByName().get(name.toLowerCase());
    Player player;

    if (playerId != null) {
      player = handleRejoin(session, room, name, token, playerId);
    } else {
      player = handleNewJoin(session, room, name);
    }
    if (player == null) return;

    sendOpenRoom(session, room, player);
    gameService.broadcastUpdate(room);
  }

  private Player handleRejoin(
      Session session, Room room, String name, String token, String playerId) {
    Player player = room.getPlayer(playerId);
    if (token != null && token.equals(player.getToken())) {
      player.setSession(session);
      player.setOnline(true);
      return player;
    } else {
      gameService.sendToSession(session, error("invalid_token", "Invalid token for user " + name));
      return null;
    }
  }

  private Player handleNewJoin(Session session, Room room, String name) {
    if (room.getPhase() != null && !room.getPhase().equals(Room.PHASE_LOBBY)) {
      gameService.sendToSession(
          session, error("game_started", "Game already started — cannot join"));
      return null;
    }
    if (room.getPlayers().size() >= MAX_PLAYERS) {
      gameService.sendToSession(
          session, error("room_full", "Room is full (max " + MAX_PLAYERS + " players)"));
      return null;
    }
    String playerId = UUID.randomUUID().toString();
    String token = UUID.randomUUID().toString();
    Player player = new Player(playerId, token, name, session, Map.of());
    room.addPlayer(player);
    return player;
  }
}
