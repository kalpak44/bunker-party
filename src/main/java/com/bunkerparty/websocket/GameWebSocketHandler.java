package com.bunkerparty.websocket;

import com.bunkerparty.manager.RoomManager;
import com.bunkerparty.model.Player;
import com.bunkerparty.model.Room;
import com.bunkerparty.websocket.handler.JoinGameHandler;
import com.bunkerparty.websocket.handler.LeaveGameHandler;
import com.bunkerparty.websocket.handler.MessageHandler;
import com.bunkerparty.websocket.handler.NewGameHandler;
import com.bunkerparty.websocket.handler.ReadyHandler;
import com.bunkerparty.websocket.handler.DiscardHandler;
import com.bunkerparty.websocket.handler.ConfirmHandler;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.gson.JsonArray;
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
    private final RoomManager roomManager;
    private final WebSocketJsonSender sender;

    @Inject
    public GameWebSocketHandler(
            NewGameHandler newGameHandler,
            JoinGameHandler joinGameHandler,
            LeaveGameHandler leaveGameHandler,
            ReadyHandler readyHandler,
            DiscardHandler discardHandler,
            ConfirmHandler confirmHandler,
            RoomManager roomManager,
            WebSocketJsonSender sender
    ) {
        handlers.put("new_game", newGameHandler);
        handlers.put("join_game", joinGameHandler);
        handlers.put("leave_game", leaveGameHandler);
        handlers.put("ready", readyHandler);
        handlers.put("discard", discardHandler);
        handlers.put("confirm", confirmHandler);
        this.roomManager = roomManager;
        this.sender = sender;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connected: {}", session.getRemoteAddress());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) throws Exception {
        logger.info("Closed: {}, {}, {}", session.getRemoteAddress(), statusCode, reason);
        for (Room room : roomManager.getAllRooms()) {
            boolean found = false;
            for (Player player : room.getPlayers().values()) {
                if (session.equals(player.getSession())) {
                    player.setOnline(false);
                    found = true;
                    break;
                }
            }
            if (found) {
                notifyPlayersUpdate(room);
            }
        }
    }

    private void notifyPlayersUpdate(Room room) throws Exception {
        JsonArray players = new JsonArray();
        for (Player p : room.getPlayers().values()) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("id", p.getId());
            pObj.addProperty("name", p.getName());
            pObj.addProperty("online", p.isOnline());
            
            JsonObject revealed = new JsonObject();
            p.getRevealedIndices().forEach(revealed::addProperty);
            pObj.add("revealed", revealed);
            
            players.add(pObj);
        }

        JsonObject notify = new JsonObject();
        notify.addProperty("type", "player_joined"); // reusing same type for simplicity in PoC
        notify.addProperty("phase", room.getPhase());
        notify.addProperty("round", room.getRound());
        if (room.getEventIdx() != null) {
            notify.addProperty("eventIdx", room.getEventIdx());
        }

        JsonObject history = new JsonObject();
        room.getRevealedByRound().forEach((r, reveals) -> {
            JsonObject rObj = new JsonObject();
            rObj.addProperty("eventIdx", room.getEventByRound().get(r));
            JsonObject revealsObj = new JsonObject();
            reveals.forEach(revealsObj::addProperty);
            rObj.add("reveals", revealsObj);
            history.add(String.valueOf(r), rObj);
        });
        notify.add("history", history);

        JsonObject roundReveals = new JsonObject();
        room.getRoundReveals().forEach(roundReveals::addProperty);
        notify.add("roundReveals", roundReveals);

        JsonArray roundConfirms = new JsonArray();
        room.getRoundConfirms().forEach(roundConfirms::add);
        notify.add("roundConfirms", roundConfirms);

        notify.add("players", players);

        for (Player p : room.getPlayers().values()) {
            if (p.getSession() != null && p.getSession().isOpen()) {
                JsonObject personalNotify = notify.deepCopy();
                JsonObject myCards = new JsonObject();
                p.getCharacterIndices().forEach(myCards::addProperty);
                personalNotify.add("myCards", myCards);
                sender.send(p.getSession(), personalNotify);
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
            sender.send(session, pong);
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
