package com.bunkerparty.service;

import com.bunkerparty.domain.Room;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final int ROOM_ID_BOUND = 10000;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Random random;

    @Inject
    public RoomManager(Random random) {
        this.random = random;
    }

    /**
     * Creates a new game room with a unique 4-digit ID.
     */
    public Room createRoom() {
        String id;
        do {
            id = String.format("%04d", random.nextInt(ROOM_ID_BOUND));
        } while (rooms.containsKey(id));
        Room room = new Room(id);
        rooms.put(id, room);
        return room;
    }

    /**
     * Returns a room by its ID.
     */
    public Room getRoom(String id) {
        return rooms.get(id);
    }

    /**
     * Returns all currently active rooms.
     */
    public Collection<Room> getAllRooms() {
        return rooms.values();
    }
}
