package com.bunkerparty.websocket.handler;

import com.bunkerparty.domain.Player;
import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;

public class LeaveGameHandler extends BaseMessageHandler {

  @Inject
  public LeaveGameHandler(GameService gameService) {
    super(gameService);
  }

  /** Handles the "leave_game" message to leave a room. */
  @Override
  public void handle(Session session, JsonObject msg) {
    Room room = getRoom(msg);
    if (room == null) return;

    boolean found = false;
    for (Player player : room.getPlayers().values()) {
      if (session.equals(player.getSession())) {
        player.setOnline(false);
        room.getPlayers().remove(player.getId());
        room.getPidByName().remove(player.getName().toLowerCase());
        found = true;
        break;
      }
    }

    if (found) {
      gameService.broadcastUpdate(room);
    }
  }
}
