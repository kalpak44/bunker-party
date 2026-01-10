package com.bunkerparty.websocket;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.bunkerparty.websocket.handler.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class GameWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    private final GameService gameService;

    @Inject
    public GameWebSocketHandler(
            NewGameHandler newGameHandler,
            JoinGameHandler joinGameHandler,
            LeaveGameHandler leaveGameHandler,
            ReadyHandler readyHandler,
            DiscardHandler discardHandler,
            ConfirmHandler confirmHandler,
            GameService gameService
    ) {
        handlers.put("new_game", newGameHandler);
        handlers.put("join_game", joinGameHandler);
        handlers.put("leave_game", leaveGameHandler);
        handlers.put("ready", readyHandler);
        handlers.put("discard", discardHandler);
        handlers.put("confirm", confirmHandler);
        this.gameService = gameService;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connected: {}", session.getRemoteAddress());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) throws Exception {
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

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
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
