package com.bunkerparty.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class Room {

    public static final String PHASE_LOBBY = "lobby";
    public static final String PHASE_REVEAL = "reveal";
    public static final String PHASE_CONFIRM = "confirm";
    public static final String PHASE_GAME_OVER = "game_over";

    private final String roomId;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, String> pidByName = new ConcurrentHashMap<>();

    private String phase = PHASE_LOBBY;
    private int round = 0;
    private Integer eventIdx = null;

    private final Map<Integer, Map<String, String>> revealedByRound = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> eventByRound = new ConcurrentHashMap<>();
    private final Set<String> startVotes = new CopyOnWriteArraySet<>();

    private final Map<String, String> roundReveals = new ConcurrentHashMap<>();
    private final Set<String> roundConfirms = new CopyOnWriteArraySet<>();

    /**
     * Creates a new room with a given ID.
     */
    public Room(String roomId) {
        this.roomId = roomId;
    }

    /**
     * Returns the room's unique ID.
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Returns a map of players in the room, keyed by player ID.
     */
    public Map<String, Player> getPlayers() {
        return players;
    }

    /**
     * Returns a map of player IDs keyed by their lowercase names.
     */
    public Map<String, String> getPidByName() {
        return pidByName;
    }

    /**
     * Adds a player to the room.
     */
    public void addPlayer(Player player) {
        players.put(player.getId(), player);
        pidByName.put(player.getName().toLowerCase(), player.getId());
    }

    /**
     * Returns a player by their ID.
     */
    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    /**
     * Returns the current game phase.
     */
    public String getPhase() {
        return phase;
    }

    /**
     * Updates the game phase.
     */
    public void setPhase(String phase) {
        this.phase = phase;
    }

    /**
     * Returns the current game round number.
     */
    public int getRound() {
        return round;
    }

    /**
     * Sets the current game round number.
     */
    public void setRound(int round) {
        this.round = round;
    }

    /**
     * Increments the game round number.
     */
    public void incrementRound() {
        this.round++;
    }

    /**
     * Returns the current bunker event index.
     */
    public Integer getEventIdx() {
        return eventIdx;
    }

    /**
     * Sets the current bunker event index and records it for the current round.
     */
    public void setEventIdx(Integer eventIdx) {
        if (eventIdx != null && round > 0) {
            eventByRound.put(round, eventIdx);
        }
        this.eventIdx = eventIdx;
    }

    /**
     * Returns a map of revealed cards per round.
     */
    public Map<Integer, Map<String, String>> getRevealedByRound() {
        return revealedByRound;
    }

    /**
     * Returns a map of bunker event indices per round.
     */
    public Map<Integer, Integer> getEventByRound() {
        return eventByRound;
    }

    /**
     * Returns a set of player IDs who have voted to start the game.
     */
    public Set<String> getStartVotes() {
        return startVotes;
    }

    /**
     * Adds a start vote from a player.
     */
    public void addStartVote(String playerId) {
        startVotes.add(playerId);
    }

    /**
     * Returns a map of cards revealed in the current round.
     */
    public Map<String, String> getRoundReveals() {
        return roundReveals;
    }

    /**
     * Records a card reveal for the current round.
     */
    public void addRoundReveal(String playerId, String key) {
        roundReveals.put(playerId, key);
        revealedByRound.computeIfAbsent(round, r -> new ConcurrentHashMap<>()).put(playerId, key);
    }

    /**
     * Clears recorded reveals for the current round.
     */
    public void clearRoundReveals() {
        roundReveals.clear();
    }

    /**
     * Returns a set of player IDs who have confirmed the end of the current round.
     */
    public Set<String> getRoundConfirms() {
        return roundConfirms;
    }

    /**
     * Records a round end confirmation from a player.
     */
    public void addRoundConfirm(String playerId) {
        roundConfirms.add(playerId);
    }

    /**
     * Clears round end confirmations.
     */
    public void clearRoundConfirms() {
        roundConfirms.clear();
    }

    /**
     * Returns a list of IDs of players who are currently online.
     */
    public List<String> getActivePlayers() {
        return players.values().stream()
                .filter(Player::isOnline)
                .map(Player::getId)
                .toList();
    }

    /**
     * Returns true if all online players have revealed a card in the current round.
     */
    public boolean allActivePlayersRevealed() {
        List<String> activeIds = getActivePlayers();
        if (activeIds.isEmpty()) return false;
        for (String id : activeIds) {
            if (!roundReveals.containsKey(id)) return false;
        }
        return true;
    }

    /**
     * Returns true if all online players have confirmed the end of the current round.
     */
    public boolean allActivePlayersConfirmed() {
        List<String> activeIds = getActivePlayers();
        if (activeIds.isEmpty()) return false;
        for (String id : activeIds) {
            if (!roundConfirms.contains(id)) return false;
        }
        return true;
    }

    /**
     * Returns true if all players have revealed all their character cards.
     */
    public boolean allPlayersUsedAllCards(int totalKeys) {
        if (players.isEmpty()) return true;
        for (Player p : players.values()) {
            if (!p.hasRevealedAllCards(totalKeys)) {
                return false;
            }
        }
        return true;
    }
}
