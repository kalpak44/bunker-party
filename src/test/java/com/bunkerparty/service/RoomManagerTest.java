package com.bunkerparty.service;

import static org.junit.jupiter.api.Assertions.*;

import com.bunkerparty.domain.Room;
import java.util.Collection;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomManagerTest {

  private RoomManager roomManager;

  @BeforeEach
  void setUp() {
    roomManager = new RoomManager(new Random());
  }

  @Test
  void shouldCreateRoomWith4DigitId() {
    Room room = roomManager.createRoom();

    assertNotNull(room);
    assertEquals(4, room.getRoomId().length());
    assertTrue(room.getRoomId().matches("\\d{4}"));
  }

  @Test
  void shouldGetRoomById() {
    Room createdRoom = roomManager.createRoom();

    Room retrievedRoom = roomManager.getRoom(createdRoom.getRoomId());

    assertEquals(createdRoom, retrievedRoom);
  }

  @Test
  void shouldGetAllRooms() {
    roomManager.createRoom();
    roomManager.createRoom();

    Collection<Room> rooms = roomManager.getAllRooms();

    assertEquals(2, rooms.size());
  }
}
