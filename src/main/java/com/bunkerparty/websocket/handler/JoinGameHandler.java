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
        String roomId = msg.get("roomId").getAsString();
        String name = msg.get("name").getAsString();
        Room room = roomManager.getRoom(roomId);

        if (room == null) {
            sender.send(session, error("room_not_found", ""));
            return;
        }

        String playerId = room.getPidByName().get(name.toLowerCase());
        Player player;

        if (playerId != null) {
            // Re-join
            player = room.getPlayer(playerId);
            player.setSession(session);
            player.setOnline(true);
        } else {
            // New join
            if (room.getPlayers().size() >= 6) {
                sender.send(session, error("room_full", "Room is full (max 6 players)"));
                return;
            }
            player = new Player(UUID.randomUUID().toString(), name, session, Map.of());
            room.addPlayer(player);
        }

        JsonObject res = new JsonObject();
        res.addProperty("type", "open_room");
        res.addProperty("room_id", room.getRoomId());
        res.addProperty("player_id", player.getId());

        JsonArray players = new JsonArray();
        for (Player p : room.getPlayers().values()) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("name", p.getName());
            pObj.addProperty("online", p.isOnline());
            players.add(pObj);
        }
        res.add("players", players);

        sender.send(session, res);

        // Notify others
        JsonObject notify = new JsonObject();
        notify.addProperty("type", "player_joined");
        notify.addProperty("name", player.getName());
        notify.add("players", players);

        for (Player p : room.getPlayers().values()) {
            if (p.getSession() != null && p.getSession().isOpen() && p.getSession() != session) {
                sender.send(p.getSession(), notify);
            }
        }
    }
}
