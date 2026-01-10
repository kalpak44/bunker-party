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

    @Inject
    public GameService(RoomManager roomManager, WebSocketJsonSender sender) {
        this.roomManager = roomManager;
        this.sender = sender;
    }

    public Room createRoom() {
        return roomManager.createRoom();
    }

    public Room getRoom(String roomId) {
        return roomManager.getRoom(roomId);
    }

    public Collection<Room> getAllRooms() {
        return roomManager.getAllRooms();
    }

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

        JsonObject history = new JsonObject();
        room.getRevealedByRound().forEach((r, reveals) -> {
            JsonObject rObj = new JsonObject();
            rObj.addProperty("eventIdx", room.getEventByRound().get(r));
            JsonObject revealsObj = new JsonObject();
            reveals.forEach(revealsObj::addProperty);
            rObj.add("reveals", revealsObj);
            history.add(String.valueOf(r), rObj);
        });
        update.add("history", history);

        JsonArray startVotes = new JsonArray();
        room.getStartVotes().forEach(startVotes::add);
        update.add("startVotes", startVotes);

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
        update.add("players", playersArray);

        JsonObject roundReveals = new JsonObject();
        room.getRoundReveals().forEach(roundReveals::addProperty);
        update.add("roundReveals", roundReveals);

        JsonArray roundConfirms = new JsonArray();
        room.getRoundConfirms().forEach(roundConfirms::add);
        update.add("roundConfirms", roundConfirms);

        return update;
    }
}
