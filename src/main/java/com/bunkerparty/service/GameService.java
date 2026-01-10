package com.bunkerparty.service;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

@Singleton
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final RoomManager roomManager;
    private final WebSocketJsonSender sender;

    /**
     * Creates a new game service.
     */
    @Inject
    public GameService(RoomManager roomManager, WebSocketJsonSender sender) {
        this.roomManager = roomManager;
        this.sender = sender;
    }

    /**
     * Creates a new game room.
     */
    public Room createRoom() {
        return roomManager.createRoom();
    }

    /**
     * Returns a room by its ID.
     */
    public Room getRoom(String roomId) {
        return roomManager.getRoom(roomId);
    }

    /**
     * Returns all active rooms.
     */
    public Collection<Room> getAllRooms() {
        return roomManager.getAllRooms();
    }

    /**
     * Broadcasts a game state update to all players in the room.
     */
    public void broadcastUpdate(Room room) {
        JsonObject update = createGameUpdateMessage(room);

        for (Player p : room.getPlayers().values()) {
            if (p.getSession() != null && p.getSession().isOpen()) {
                JsonObject personalUpdate = update.deepCopy();
                JsonObject myCards = new JsonObject();
                p.getCharacterIndices().forEach(myCards::addProperty);
                personalUpdate.add("myCards", myCards);
                try {
                    sender.send(p.getSession(), personalUpdate);
                } catch (IOException e) {
                    logger.error("Failed to send update to player {} in room {}", p.getName(), room.getRoomId(), e);
                }
            }
        }
    }

    /**
     * Sends a JSON message to a specific WebSocket session.
     */
    public void sendToSession(Session session, JsonObject message) {
        try {
            sender.send(session, message);
        } catch (IOException e) {
            logger.error("Failed to send message to session {}", session.getRemoteAddress(), e);
        }
    }

    private JsonObject createGameUpdateMessage(Room room) {
        JsonObject update = new JsonObject();
        update.addProperty("type", "game_update");
        update.addProperty("phase", room.getPhase());
        update.addProperty("round", room.getRound());
        update.addProperty("roomId", room.getRoomId());

        if (room.getEventIdx() != null) {
            update.addProperty("eventIdx", room.getEventIdx());
        }

        update.add("history", createHistoryObject(room));
        update.add("startVotes", createStartVotesArray(room));
        update.add("players", createPlayersArray(room));
        update.add("roundReveals", createRoundRevealsObject(room));
        update.add("roundConfirms", createRoundConfirmsArray(room));

        return update;
    }

    private JsonObject createHistoryObject(Room room) {
        JsonObject history = new JsonObject();
        room.getRevealedByRound().forEach((r, reveals) -> {
            JsonObject rObj = new JsonObject();
            rObj.addProperty("eventIdx", room.getEventByRound().get(r));
            JsonObject revealsObj = new JsonObject();
            reveals.forEach(revealsObj::addProperty);
            rObj.add("reveals", revealsObj);
            history.add(String.valueOf(r), rObj);
        });
        return history;
    }

    private JsonArray createStartVotesArray(Room room) {
        JsonArray startVotes = new JsonArray();
        room.getStartVotes().forEach(startVotes::add);
        return startVotes;
    }

    private JsonArray createPlayersArray(Room room) {
        JsonArray playersArray = new JsonArray();
        for (Player p : room.getPlayers().values()) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("id", p.getId());
            pObj.addProperty("name", p.getName());
            pObj.addProperty("online", p.isOnline());
            pObj.addProperty("ready", room.getStartVotes().contains(p.getId()));

            JsonObject revealed = new JsonObject();
            p.getRevealedIndices().forEach(revealed::addProperty);
            pObj.add("revealed", revealed);

            playersArray.add(pObj);
        }
        return playersArray;
    }

    private JsonObject createRoundRevealsObject(Room room) {
        JsonObject roundReveals = new JsonObject();
        room.getRoundReveals().forEach(roundReveals::addProperty);
        return roundReveals;
    }

    private JsonArray createRoundConfirmsArray(Room room) {
        JsonArray roundConfirms = new JsonArray();
        room.getRoundConfirms().forEach(roundConfirms::add);
        return roundConfirms;
    }
}
