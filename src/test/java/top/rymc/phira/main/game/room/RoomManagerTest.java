package top.rymc.phira.main.game.room;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.game.exception.GameOperationException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomManagerTest {

    @BeforeEach
    void setUp() throws Exception {
        clearRooms();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearRooms();
    }

    private void clearRooms() throws Exception {
        Field roomsField = RoomManager.class.getDeclaredField("ROOMS");
        roomsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Room> rooms = (Map<String, Room>) roomsField.get(null);
        rooms.clear();
    }

    @Test
    @DisplayName("resolveRoom creates new room with new roomId")
    void resolveRoomCreatesNewRoomWithNewRoomId() {
        String roomId = "test-room-1";
        Room mockRoom = mock(Room.class);
        when(mockRoom.getRoomId()).thenReturn(roomId);
        Function<Runnable, Room> constructor = onClose -> mockRoom;

        Room result = RoomManager.resolveRoom(roomId, constructor);

        assertThat(result).isNotNull();
        assertThat(result.getRoomId()).isEqualTo(roomId);
    }

    @Test
    @DisplayName("resolveRoom throws GameOperationException when roomId already exists")
    void resolveRoomThrowsGameOperationExceptionWhenRoomIdAlreadyExists() {
        String roomId = "test-room-2";
        Room mockRoom = mock(Room.class);
        when(mockRoom.getRoomId()).thenReturn(roomId);
        Function<Runnable, Room> constructor = onClose -> mockRoom;
        RoomManager.resolveRoom(roomId, constructor);

        assertThatThrownBy(() -> RoomManager.resolveRoom(roomId, constructor))
                .isInstanceOf(GameOperationException.class)
                .hasMessage("error.room_already_exists");
    }

    @Test
    @DisplayName("findRoom returns room instance if exists")
    void findRoomReturnsRoomInstanceIfExists() {
        String roomId = "test-room-3";
        Room mockRoom = mock(Room.class);
        when(mockRoom.getRoomId()).thenReturn(roomId);
        Function<Runnable, Room> constructor = onClose -> mockRoom;
        RoomManager.resolveRoom(roomId, constructor);

        Room result = RoomManager.findRoom(roomId);

        assertThat(result).isNotNull();
        assertThat(result.getRoomId()).isEqualTo(roomId);
    }

    @Test
    @DisplayName("findRoom returns null if roomId not exists")
    void findRoomReturnsNullIfRoomIdNotExists() {
        String roomId = "non-existent-room";

        Room result = RoomManager.findRoom(roomId);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getAllRooms returns list of all registered rooms")
    void getAllRoomsReturnsListOfAllRegisteredRooms() {
        String roomId1 = "test-room-4";
        String roomId2 = "test-room-5";
        Room mockRoom1 = mock(Room.class);
        Room mockRoom2 = mock(Room.class);
        when(mockRoom1.getRoomId()).thenReturn(roomId1);
        when(mockRoom2.getRoomId()).thenReturn(roomId2);
        Function<Runnable, Room> constructor1 = onClose -> mockRoom1;
        Function<Runnable, Room> constructor2 = onClose -> mockRoom2;
        RoomManager.resolveRoom(roomId1, constructor1);
        RoomManager.resolveRoom(roomId2, constructor2);

        List<Room> result = RoomManager.getAllRooms();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Room::getRoomId).containsExactlyInAnyOrder(roomId1, roomId2);
    }

    @Test
    @DisplayName("getAllRooms returns empty list when no rooms registered")
    void getAllRoomsReturnsEmptyListWhenNoRoomsRegistered() {
        List<Room> result = RoomManager.getAllRooms();

        assertThat(result).isEmpty();
    }
}
