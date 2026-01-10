package com.bunkerparty.service;

import com.bunkerparty.domain.Room;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class RoomManagerTest {

    @Test
    void shouldCreateRoomWith4DigitId() {
        RoomManager roomManager = new RoomManager();

        Room room = roomManager.createRoom();

        assertNotNull(room);
        assertEquals(4, room.getRoomId().length());
        assertTrue(room.getRoomId().matches("\\d{4}"));
    }

    @Test
    void shouldGetRoomById() {
        RoomManager roomManager = new RoomManager();
        Room createdRoom = roomManager.createRoom();

        Room retrievedRoom = roomManager.getRoom(createdRoom.getRoomId());

        assertEquals(createdRoom, retrievedRoom);
    }

    @Test
    void shouldGetAllRooms() {
        RoomManager roomManager = new RoomManager();
        roomManager.createRoom();
        roomManager.createRoom();

        Collection<Room> rooms = roomManager.getAllRooms();

        assertEquals(2, rooms.size());
    }
}
