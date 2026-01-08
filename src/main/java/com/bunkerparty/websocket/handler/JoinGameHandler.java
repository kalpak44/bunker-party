package com.bunkerparty.websocket.handler;

import com.bunkerparty.manager.RoomManager;
import com.bunkerparty.model.Player;
import com.bunkerparty.model.Room;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;
import java.util.UUID;

import static com.bunkerparty.websocket.helpers.JsonUtils.error;


public class JoinGameHandler implements MessageHandler {

    private final RoomManager roomManager;
    private final WebSocketJsonSender sender;

    @Inject
    public JoinGameHandler(RoomManager roomManager, WebSocketJsonSender sender) {
        this.roomManager = roomManager;
        this.sender = sender;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws Exception {
        String roomId = msg.has("roomId") && !msg.get("roomId").isJsonNull() ? msg.get("roomId").getAsString() : "";
        String name = msg.has("name") && !msg.get("name").isJsonNull() ? msg.get("name").getAsString().trim() : "";
        String token = msg.has("token") && !msg.get("token").isJsonNull() ? msg.get("token").getAsString() : null;
        Room room = roomManager.getRoom(roomId);

        if (room == null) {
            sender.send(session, error("room_not_found", ""));
            return;
        }

        if (room.getPhase() != null && !room.getPhase().equals(Room.PHASE_LOBBY)) {
            sender.send(session, error("game_started", "Game already started — cannot join"));
            return;
        }

        if (name.isEmpty()) {
            sender.send(session, error("name_required", "Name is required"));
            return;
        }
        if (name.length() > 10) {
            sender.send(session, error("name_too_long", "Name is too long"));
            return;
        }

        String playerId = room.getPidByName().get(name.toLowerCase());
        Player player;

        if (playerId != null) {
            // Re-join
            player = room.getPlayer(playerId);
            if (token != null && token.equals(player.getToken())) {
                player.setSession(session);
                player.setOnline(true);
            } else {
                sender.send(session, error("invalid_token", "Invalid token for user " + name));
                return;
            }
        } else {
            // New join
            if (room.getPlayers().size() >= 6) {
                sender.send(session, error("room_full", "Room is full (max 6 players)"));
                return;
            }
            token = UUID.randomUUID().toString();
            player = new Player(UUID.randomUUID().toString(), token, name, session, Map.of());
            room.addPlayer(player);
        }

        JsonObject res = new JsonObject();
        res.addProperty("type", "open_room");
        res.addProperty("room_id", room.getRoomId());
        res.addProperty("player_id", player.getId());
        res.addProperty("token", player.getToken());
        res.addProperty("phase", room.getPhase());
        res.addProperty("round", room.getRound());
        if (room.getEventIdx() != null) {
            res.addProperty("eventIdx", room.getEventIdx());
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
        res.add("history", history);

        JsonObject myCards = new JsonObject();
        player.getCharacterIndices().forEach(myCards::addProperty);
        res.add("myCards", myCards);

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
        res.add("players", players);

        sender.send(session, res);

        // Notify others
        JsonObject notify = new JsonObject();
        notify.addProperty("type", "player_joined");
        notify.addProperty("name", player.getName());
        notify.add("players", players);
        notify.addProperty("phase", room.getPhase());
        notify.addProperty("round", room.getRound());
        if (room.getEventIdx() != null) {
            notify.addProperty("eventIdx", room.getEventIdx());
        }

        notify.add("history", history);

        for (Player p : room.getPlayers().values()) {
            if (p.getSession() != null && p.getSession().isOpen() && p.getSession() != session) {
                sender.send(p.getSession(), notify);
            }
        }
    }
}
