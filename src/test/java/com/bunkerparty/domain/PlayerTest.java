package com.bunkerparty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;

class PlayerTest {

  @Test
  void shouldCreatePlayerWithCorrectValues() {
    String id = "player-1";
    String token = "token-1";
    String name = "Alice";
    Session session = mock(Session.class);
    Map<String, Integer> characterIndices = Map.of("profession", 1);

    Player player = new Player(id, token, name, session, characterIndices);

    assertEquals(id, player.getId());
    assertEquals(token, player.getToken());
    assertEquals(name, player.getName());
    assertEquals(session, player.getSession());
    assertEquals(characterIndices, player.getCharacterIndices());
    assertTrue(player.isOnline());
  }

  @Test
  void shouldRevealCardCorrectly() {
    Player player = new Player("1", "t", "Alice", null, Map.of("profession", 5, "health", 2));

    player.revealCard("profession");

    assertTrue(player.hasUsedKey("profession"));
    assertFalse(player.hasUsedKey("health"));
    assertEquals(1, player.getRevealedIndices().size());
    assertEquals(5, player.getRevealedIndices().get("profession"));
  }

  @Test
  void shouldTrackOnlineStatusAndLastSeen() {
    Player player = new Player("1", "t", "Alice", null, Map.of());
    long before = System.currentTimeMillis();

    player.setOnline(false);

    long after = System.currentTimeMillis();
    assertFalse(player.isOnline());
    // Going offline re-stamps lastSeen with the current time. Bounding it by the window
    // around the call proves that without sleeping for the clock to tick over.
    assertTrue(player.getLastSeen() >= before);
    assertTrue(player.getLastSeen() <= after);
  }

  @Test
  void shouldCheckIfAllCardsRevealed() {
    Player player = new Player("1", "t", "Alice", null, Map.of("p", 1, "h", 2));

    player.revealCard("p");
    assertFalse(player.hasRevealedAllCards(2));

    player.revealCard("h");
    assertTrue(player.hasRevealedAllCards(2));
  }
}
