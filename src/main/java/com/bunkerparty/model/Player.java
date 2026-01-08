package com.bunkerparty.model;

import org.eclipse.jetty.websocket.api.Session;

import java.util.*;

public class Player {
    private final String id;
    private final String token;
    private String name;
    private Session session;
    private Map<String, Integer> characterIndices;
    private Map<String, Integer> revealedIndices;
    private Set<String> usedKeys;
    private boolean online;
    private long lastSeen;

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

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
        if (session != null) {
            this.online = true;
            this.lastSeen = System.currentTimeMillis();
        }
    }

    public Map<String, Integer> getCharacterIndices() {
        return new HashMap<>(characterIndices);
    }

    public void setCharacterIndices(Map<String, Integer> characterIndices) {
        this.characterIndices = new HashMap<>(characterIndices);
    }

    public Map<String, Integer> getRevealedIndices() {
        return new HashMap<>(revealedIndices);
    }

    public void revealCard(String key) {
        if (characterIndices.containsKey(key)) {
            revealedIndices.put(key, characterIndices.get(key));
            usedKeys.add(key);
        }
    }

    public Set<String> getUsedKeys() {
        return new HashSet<>(usedKeys);
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
        if (!online) {
            this.lastSeen = System.currentTimeMillis();
        }
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    public boolean hasUsedKey(String key) {
        return usedKeys.contains(key);
    }

    public boolean hasRevealedAllCards(int totalKeys) {
        return usedKeys.size() >= totalKeys;
    }
}
