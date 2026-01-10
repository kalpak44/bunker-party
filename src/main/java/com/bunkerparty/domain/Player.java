package com.bunkerparty.domain;

import org.eclipse.jetty.websocket.api.Session;

import java.util.*;

public class Player {
    private final String id;
    private final String token;
    private String name;
    private Session session;
    private Map<String, Integer> characterIndices;
    private final Map<String, Integer> revealedIndices;
    private final Set<String> usedKeys;
    private boolean online;
    private long lastSeen;

    /**
     * Creates a new player with an ID, token, name, and session.
     */
    public Player(String id, String token, String name, Session session,
                  Map<String, Integer> characterIndices) {
        this.id = id;
        this.token = token;
        this.name = name;
        this.session = session;
        this.characterIndices = new HashMap<>(characterIndices);
        this.revealedIndices = new HashMap<>();
        this.usedKeys = new HashSet<>();
        this.online = true;
        this.lastSeen = System.currentTimeMillis();
    }

    /**
     * Returns the player's unique ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the player's session token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the player's display name.
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the player's display name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the player's current WebSocket session.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Updates the player's session and marks them as online.
     */
    public void setSession(Session session) {
        this.session = session;
        if (session != null) {
            this.online = true;
            this.lastSeen = System.currentTimeMillis();
        }
    }

    /**
     * Returns a copy of the player's character card indices.
     */
    public Map<String, Integer> getCharacterIndices() {
        return new HashMap<>(characterIndices);
    }

    /**
     * Sets the player's character card indices.
     */
    public void setCharacterIndices(Map<String, Integer> characterIndices) {
        this.characterIndices = new HashMap<>(characterIndices);
    }

    /**
     * Returns a copy of the cards revealed by the player.
     */
    public Map<String, Integer> getRevealedIndices() {
        return new HashMap<>(revealedIndices);
    }

    /**
     * Reveals a specific card by its key.
     */
    public void revealCard(String key) {
        if (characterIndices.containsKey(key)) {
            revealedIndices.put(key, characterIndices.get(key));
            usedKeys.add(key);
        }
    }

    /**
     * Returns a set of card keys already used by the player.
     */
    public Set<String> getUsedKeys() {
        return new HashSet<>(usedKeys);
    }

    /**
     * Returns true if the player is currently connected.
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Updates the player's online status.
     */
    public void setOnline(boolean online) {
        this.online = online;
        if (!online) {
            this.lastSeen = System.currentTimeMillis();
        }
    }

    /**
     * Returns the timestamp when the player was last seen.
     */
    public long getLastSeen() {
        return lastSeen;
    }

    /**
     * Updates the last seen timestamp to the current time.
     */
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    /**
     * Returns true if the player has already used the specified card key.
     */
    public boolean hasUsedKey(String key) {
        return usedKeys.contains(key);
    }

    /**
     * Returns true if the player has revealed all their cards.
     */
    public boolean hasRevealedAllCards(int totalKeys) {
        return usedKeys.size() >= totalKeys;
    }
}
