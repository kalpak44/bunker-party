package com.bunkerparty.websocket.handler;

import com.bunkerparty.manager.RoomManager;
import com.bunkerparty.model.Player;
import com.bunkerparty.model.Room;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaveGameHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(LeaveGameHandler.class);
    private final RoomManager roomManager;
    private final WebSocketJsonSender sender;

    @Inject
    public LeaveGameHandler(RoomManager roomManager, WebSocketJsonSender sender) {
        this.roomManager = roomManager;
        this.sender = sender;
    }

    @Override
    public void handle(Session session, JsonObject msg) throws Exception {
        String roomId = msg.has("roomId") && !msg.get("roomId").isJsonNull() ? msg.get("roomId").getAsString() : null;
        if (roomId == null) return;

        Room room = roomManager.getRoom(roomId);
        if (room == null) return;

        boolean found = false;
        for (Player player : room.getPlayers().values()) {
            if (session.equals(player.getSession())) {
                player.setOnline(false);
                // We don't remove the player from the room entirely to allow re-join if they didn't cleanup session,
                // but here they ARE cleaning up session, so maybe we SHOULD remove them?
                // Actually, the issue says "cleanup session", so they are "logging out".
                // If they are logging out, we should probably remove them from the room so their name becomes available.
                room.getPlayers().remove(player.getId());
                room.getPidByName().remove(player.getName().toLowerCase());
                found = true;
                break;
            }
        }

        if (found) {
            notifyPlayersUpdate(room);
        }
    }

    private void notifyPlayersUpdate(Room room) throws Exception {
        JsonArray players = new JsonArray();
        for (Player p : room.getPlayers().values()) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("name", p.getName());
            pObj.addProperty("online", p.isOnline());
            players.add(pObj);
        }

        JsonObject notify = new JsonObject();
        notify.addProperty("type", "player_joined"); // reusing same type for simplicity
        notify.add("players", players);

        for (Player p : room.getPlayers().values()) {
            if (p.getSession() != null && p.getSession().isOpen()) {
                sender.send(p.getSession(), notify);
            }
        }
    }
}
