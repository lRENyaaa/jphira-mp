package top.rymc.phira.main.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoomManager")
class RoomManagerTest {

    @BeforeEach
    void setUp() {
        java.util.Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
    }

    @AfterEach
    void tearDown() {
        java.util.Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
    }

    @Test
    @DisplayName("should create room with setting")
    void shouldCreateRoomWithSetting() {
        var setting = new Room.RoomSetting(true, true, 8, false, false, false, true);

        Room room = RoomManager.createRoom("test-room-setting", setting);

        assertThat(room).isNotNull();
        assertThat(RoomManager.findRoom("test-room-setting")).isEqualTo(room);
        assertThat(room.getSetting()).isEqualTo(setting);
    }

    @Test
    @DisplayName("should create room with host player")
    void shouldCreateRoomWithHostPlayer() {
        var host = TestPlayerFactory.createPlayer(1, "host");

        Room room = RoomManager.createRoom("test-room-host", host);

        assertThat(room).isNotNull();
        assertThat(RoomManager.findRoom("test-room-host")).isEqualTo(room);
        assertThat(room.isHost(host)).isTrue();
        assertThat(room.containsPlayer(host)).isTrue();
    }

    @Test
    @DisplayName("should throw when creating duplicate room")
    void shouldThrowWhenCreatingDuplicateRoom() {
        RoomManager.createRoom("duplicate-room", new Room.RoomSetting());

        assertThatThrownBy(() ->
            RoomManager.createRoom("duplicate-room", new Room.RoomSetting())
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("should find existing room")
    void shouldFindExistingRoom() {
        Room created = RoomManager.createRoom("findable-room", new Room.RoomSetting());

        Room found = RoomManager.findRoom("findable-room");

        assertThat(found).isEqualTo(created);
    }

    @Test
    @DisplayName("should return null when room not found")
    void shouldReturnNullWhenRoomNotFound() {
        Room found = RoomManager.findRoom("non-existent-room");

        assertThat(found).isNull();
    }

    @Test
    @DisplayName("should get all rooms")
    void shouldGetAllRooms() {
        Room room1 = RoomManager.createRoom("room-1", new Room.RoomSetting());
        Room room2 = RoomManager.createRoom("room-2", new Room.RoomSetting());

        var allRooms = RoomManager.getAllRooms();

        assertThat(allRooms).hasSize(2);
        assertThat(allRooms).contains(room1, room2);
    }

    @Test
    @DisplayName("should return empty list when no rooms")
    void shouldReturnEmptyListWhenNoRooms() {
        var allRooms = RoomManager.getAllRooms();

        assertThat(allRooms).isEmpty();
    }

    @Test
    @DisplayName("should return defensive copy of rooms list")
    void shouldReturnDefensiveCopyOfRoomsList() {
        RoomManager.createRoom("room-for-copy-test", new Room.RoomSetting());

        var firstCall = RoomManager.getAllRooms();
        var secondCall = RoomManager.getAllRooms();

        assertThat(firstCall).isNotSameAs(secondCall);
        assertThat(firstCall).containsExactlyElementsOf(secondCall);
    }
}
