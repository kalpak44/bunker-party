package com.bunkerparty.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    @Test
    void shouldInitializeWithCorrectRoomId() {
        String roomId = "1234";
        Room room = new Room(roomId);

        assertEquals(roomId, room.getRoomId());
        assertEquals(Room.PHASE_LOBBY, room.getPhase());
        assertEquals(0, room.getRound());
        assertTrue(room.getPlayers().isEmpty());
    }

    @Test
    void shouldAddPlayerAndTrackByName() {
        Room room = new Room("1234");
        Player player = new Player("p1", "t1", "Alice", null, Map.of());

        room.addPlayer(player);

        assertEquals(player, room.getPlayer("p1"));
        assertEquals("p1", room.getPidByName().get("alice"));
    }

    @Test
    void shouldTrackStartVotes() {
        Room room = new Room("1234");

        room.addStartVote("p1");

        assertTrue(room.getStartVotes().contains("p1"));
    }

    @Test
    void shouldHandleRoundTransitions() {
        Room room = new Room("1234");
        room.setRound(1);

        room.incrementRound();

        assertEquals(2, room.getRound());
    }

    @Test
    void shouldTrackRoundRevealsAndConfirms() {
        Room room = new Room("1234");
        room.setRound(1);

        room.addRoundReveal("p1", "profession");
        room.addRoundConfirm("p1");

        assertEquals("profession", room.getRoundReveals().get("p1"));
        assertEquals("profession", room.getRevealedByRound().get(1).get("p1"));
        assertTrue(room.getRoundConfirms().contains("p1"));

        room.clearRoundReveals();
        room.clearRoundConfirms();

        assertTrue(room.getRoundReveals().isEmpty());
        assertTrue(room.getRoundConfirms().isEmpty());
    }

    @Test
    void shouldCheckActivePlayersStatus() {
        Room room = new Room("1234");
        Player p1 = new Player("p1", "t1", "Alice", null, Map.of());
        Player p2 = new Player("p2", "t2", "Bob", null, Map.of());
        room.addPlayer(p1);
        room.addPlayer(p2);
        p2.setOnline(false);

        List<String> active = room.getActivePlayers();

        assertEquals(1, active.size());
        assertEquals("p1", active.get(0));
    }

    @Test
    void shouldCheckIfAllActivePlayersRevealed() {
        Room room = new Room("1234");
        Player p1 = new Player("p1", "t1", "Alice", null, Map.of());
        Player p2 = new Player("p2", "t2", "Bob", null, Map.of());
        room.addPlayer(p1);
        room.addPlayer(p2);
        
        room.addRoundReveal("p1", "card1");
        assertFalse(room.allActivePlayersRevealed());
        
        room.addRoundReveal("p2", "card2");
        assertTrue(room.allActivePlayersRevealed());
        
        p2.setOnline(false);
        room.clearRoundReveals();
        room.addRoundReveal("p1", "card1");
        assertTrue(room.allActivePlayersRevealed());
    }

    @Test
    void shouldCheckIfAllPlayersUsedAllCards() {
        Room room = new Room("1234");
        Player p1 = new Player("p1", "t1", "Alice", null, Map.of("c1", 1));
        room.addPlayer(p1);

        assertFalse(room.allPlayersUsedAllCards(1));

        p1.revealCard("c1");
        assertTrue(room.allPlayersUsedAllCards(1));
    }
}
