package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.bunkerparty.websocket.helpers.JsonUtils;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class NewGameHandler extends BaseMessageHandler {

    @Inject
    public NewGameHandler(GameService gameService) {
        super(gameService);
    }

    /**
     * Handles the "new_game" message to create a new room.
     */
    @Override
    public void handle(Session session, JsonObject msg) {
        String name = getString(msg, "name").trim();
        if (!validateName(session, name)) {
            return;
        }

        Room room = gameService.createRoom();
        String playerId = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();
        Player creator = new Player(playerId, token, name, session, Map.of());
        room.addPlayer(creator);

        sendOpenRoom(session, room, creator);

        gameService.broadcastUpdate(room);
    }
}
