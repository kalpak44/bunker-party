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

    public Room(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public Map<String, String> getPidByName() {
        return pidByName;
    }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
        pidByName.put(player.getName().toLowerCase(), player.getId());
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public void incrementRound() {
        this.round++;
    }

    public Integer getEventIdx() {
        return eventIdx;
    }

    public void setEventIdx(Integer eventIdx) {
        if (eventIdx != null && round > 0) {
            eventByRound.put(round, eventIdx);
        }
        this.eventIdx = eventIdx;
    }

    public Map<Integer, Map<String, String>> getRevealedByRound() {
        return revealedByRound;
    }

    public Map<Integer, Integer> getEventByRound() {
        return eventByRound;
    }

    public Set<String> getStartVotes() {
        return startVotes;
    }

    public void addStartVote(String playerId) {
        startVotes.add(playerId);
    }

    public Map<String, String> getRoundReveals() {
        return roundReveals;
    }

    public void addRoundReveal(String playerId, String key) {
        roundReveals.put(playerId, key);
        revealedByRound.computeIfAbsent(round, r -> new ConcurrentHashMap<>()).put(playerId, key);
    }

    public void clearRoundReveals() {
        roundReveals.clear();
    }

    public Set<String> getRoundConfirms() {
        return roundConfirms;
    }

    public void addRoundConfirm(String playerId) {
        roundConfirms.add(playerId);
    }

    public void clearRoundConfirms() {
        roundConfirms.clear();
    }

    public List<String> getActivePlayers() {
        return players.values().stream()
                .filter(Player::isOnline)
                .map(Player::getId)
                .toList();
    }

    public boolean allActivePlayersRevealed() {
        List<String> activeIds = getActivePlayers();
        if (activeIds.isEmpty()) return false;
        for (String id : activeIds) {
            if (!roundReveals.containsKey(id)) return false;
        }
        return true;
    }

    public boolean allActivePlayersConfirmed() {
        List<String> activeIds = getActivePlayers();
        if (activeIds.isEmpty()) return false;
        for (String id : activeIds) {
            if (!roundConfirms.contains(id)) return false;
        }
        return true;
    }

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
