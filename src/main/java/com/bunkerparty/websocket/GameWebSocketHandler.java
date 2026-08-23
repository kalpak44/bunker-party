package com.bunkerparty.websocket;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.bunkerparty.websocket.handler.ConfirmHandler;
import com.bunkerparty.websocket.handler.DiscardHandler;
import com.bunkerparty.websocket.handler.JoinGameHandler;
import com.bunkerparty.websocket.handler.LeaveGameHandler;
import com.bunkerparty.websocket.handler.MessageHandler;
import com.bunkerparty.websocket.handler.NewGameHandler;
import com.bunkerparty.websocket.handler.ReadyHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class GameWebSocketHandler {

  private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
  private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
  private final GameService gameService;

  /** Creates a new WebSocket handler with all necessary message handlers injected. */
  @Inject
  public GameWebSocketHandler(
      NewGameHandler newGameHandler,
      JoinGameHandler joinGameHandler,
      LeaveGameHandler leaveGameHandler,
      ReadyHandler readyHandler,
      DiscardHandler discardHandler,
      ConfirmHandler confirmHandler,
      GameService gameService) {
    handlers.put("new_game", newGameHandler);
    handlers.put("join_game", joinGameHandler);
    handlers.put("leave_game", leaveGameHandler);
    handlers.put("ready", readyHandler);
    handlers.put("discard", discardHandler);
    handlers.put("confirm", confirmHandler);
    this.gameService = gameService;
  }

  /** Called when a new WebSocket connection is established. */
  @OnWebSocketConnect
  public void onConnect(Session session) {
    logger.info("Connected: {}", session.getRemoteAddress());
  }

  /** Called when a WebSocket connection is closed. */
  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
    logger.info("Closed: {}, {}, {}", session.getRemoteAddress(), statusCode, reason);
    for (Room room : gameService.getAllRooms()) {
      boolean found = false;
      for (Player player : room.getPlayers().values()) {
        if (session.equals(player.getSession())) {
          player.setOnline(false);
          found = true;
          break;
        }
      }
      if (found) {
        gameService.broadcastUpdate(room);
      }
    }
  }

  /** Called when a WebSocket message is received. */
  @OnWebSocketMessage
  public void onMessage(Session session, String message) {
    var json = JsonParser.parseString(message).getAsJsonObject();
    String type = json.get("type").getAsString();

    if ("ping".equals(type)) {
      JsonObject pong = new JsonObject();
      pong.addProperty("type", "pong");
      gameService.sendToSession(session, pong);
      return;
    }

    MessageHandler handler = handlers.get(type);

    if (handler != null) {
      handler.handle(session, json);
    } else {
      logger.warn("Unknown message type: {}", type);
    }
  }
}
