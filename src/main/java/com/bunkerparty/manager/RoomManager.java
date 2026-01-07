package com.bunkerparty.manager;

import com.bunkerparty.model.Room;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createRoom() {
        String id = UUID.randomUUID().toString();
        Room room = new Room(id);
        rooms.put(id, room);
        return room;
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }
}
