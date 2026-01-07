package com.bunkerparty.model;

import java.util.*;

public class Room {
    public static final String PHASE_LOBBY = "lobby";
    public static final String PHASE_REVEAL = "reveal";
    public static final String PHASE_CONFIRM = "confirm";
    public static final String PHASE_VOTE = "vote";
    public static final String PHASE_GAME_OVER = "game_over";

    private final String roomId;
    private final Map<String, Player> players;
    private final Map<String, String> pidByName;
    private final List<LogEntry> logs;

    private String phase;
    private int round;
    private Integer eventIdx;
    private Set<String> startVotes;
    private Map<String, String> roundReveals;
    private Set<String> roundConfirms;
    private Map<String, String> roundVotes;
    private Set<String> eliminated;
    private Map<Integer, Integer> elimPlan;
    private Set<String> revoteTargets;
    private Integer revoteQuota;

    public Room(String roomId) {
        this.roomId = roomId;
        this.players = new HashMap<>();
        this.pidByName = new HashMap<>();
        this.logs = new ArrayList<>();

        this.phase = PHASE_LOBBY;
        this.round = 0;
        this.eventIdx = null;
        this.startVotes = new HashSet<>();
        this.roundReveals = new HashMap<>();
        this.roundConfirms = new HashSet<>();
        this.roundVotes = new HashMap<>();
        this.eliminated = new HashSet<>();
        this.elimPlan = null;
        this.revoteTargets = null;
        this.revoteQuota = null;
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
        this.eventIdx = eventIdx;
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

    public Map<String, String> getRoundVotes() {
        return roundVotes;
    }

    public void addRoundVote(String voterId, String targetId) {
        roundVotes.put(voterId, targetId);
    }

    public void clearRoundVotes() {
        roundVotes.clear();
    }

    public Set<String> getEliminated() {
        return eliminated;
    }

    public void eliminatePlayer(String playerId) {
        eliminated.add(playerId);
    }

    public boolean isEliminated(String playerId) {
        return eliminated.contains(playerId);
    }

    public Map<Integer, Integer> getElimPlan() {
        return elimPlan;
    }

    public void setElimPlan(Map<Integer, Integer> elimPlan) {
        this.elimPlan = elimPlan;
    }

    public Set<String> getRevoteTargets() {
        return revoteTargets;
    }

    public void setRevoteTargets(Set<String> revoteTargets) {
        this.revoteTargets = revoteTargets;
    }

    public Integer getRevoteQuota() {
        return revoteQuota;
    }

    public void setRevoteQuota(Integer revoteQuota) {
        this.revoteQuota = revoteQuota;
    }

    public void clearRevoteContext() {
        this.revoteTargets = null;
        this.revoteQuota = null;
    }

    public List<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public void addLog(LogEntry entry) {
        logs.add(entry);
    }

    public List<String> getActivePlayers() {
        List<String> active = new ArrayList<>();
        for (String pid : players.keySet()) {
            if (!eliminated.contains(pid)) {
                active.add(pid);
            }
        }
        return active;
    }

    public int getActivePlayerCount() {
        return getActivePlayers().size();
    }

    public boolean allActivePlayersRevealed() {
        List<String> active = getActivePlayers();
        if (active.isEmpty()) return false;

        for (String pid : active) {
            if (!roundReveals.containsKey(pid)) {
                return false;
            }
        }
        return true;
    }

    public boolean allActivePlayersConfirmed() {
        List<String> active = getActivePlayers();
        if (active.isEmpty()) return false;

        for (String pid : active) {
            if (!roundConfirms.contains(pid)) {
                return false;
            }
        }
        return true;
    }

    public boolean allActivePlayersVoted() {
        List<String> active = getActivePlayers();
        if (active.isEmpty()) return false;

        for (String pid : active) {
            if (!roundVotes.containsKey(pid)) {
                return false;
            }
        }
        return true;
    }

    public boolean allPlayersUsedAllCards(int totalKeys) {
        if (players.isEmpty()) return false;

        for (Player player : players.values()) {
            if (!player.hasRevealedAllCards(totalKeys)) {
                return false;
            }
        }
        return true;
    }

    public int getCurrentRoundQuota() {
        if (elimPlan == null) return 0;
        return elimPlan.getOrDefault(round, 0);
    }
}
