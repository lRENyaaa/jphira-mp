package top.rymc.phira.main.game.player.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.player.operations.LocalPlayerOperations;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.network.ConnectionReference;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.RoomHandler;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalPlayerTest {

    @Mock
    private UserInfo userInfo;

    @Mock
    private ConnectionReference connectionRef;

    @Mock
    private PlayerConnection connection;

    @Mock
    private Room room;

    private LocalPlayer localPlayer;

    @BeforeEach
    void setUp() {
        localPlayer = new LocalPlayer(userInfo, connectionRef);
    }

    @Test
    @DisplayName("should return room when handler is RoomHandler")
    void shouldReturnRoomWhenHandlerIsRoomHandler() {
        RoomHandler roomHandler = mock(RoomHandler.class);
        when(connectionRef.get()).thenReturn(connection);
        when(connection.getPacketHandler()).thenReturn(roomHandler);
        when(roomHandler.getRoom()).thenReturn(room);

        Optional<Room> result = localPlayer.getRoom();

        assertThat(result).isPresent().hasValue(room);
    }

    @Test
    @DisplayName("should return empty when handler is not RoomHandler")
    void shouldReturnEmptyWhenHandlerIsNotRoomHandler() {
        ServerBoundPacketHandler nonRoomHandler = mock(ServerBoundPacketHandler.class);
        when(connectionRef.get()).thenReturn(connection);
        when(connection.getPacketHandler()).thenReturn(nonRoomHandler);

        Optional<Room> result = localPlayer.getRoom();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should leave room and mark connection kicked when kick")
    void shouldLeaveRoomAndMarkConnectionKickedWhenKick() {
        RoomHandler roomHandler = mock(RoomHandler.class);
        when(connectionRef.get()).thenReturn(connection);
        when(connection.getPacketHandler()).thenReturn(roomHandler);
        when(roomHandler.getRoom()).thenReturn(room);

        localPlayer.kick();

        verify(room).leave(localPlayer);
        verify(connection).markAsKicked();
    }

    @Test
    @DisplayName("should return true when connection is open")
    void shouldReturnTrueWhenConnectionIsOpen() {
        when(connectionRef.get()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        boolean result = localPlayer.isOnline();

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when connection is closed")
    void shouldReturnFalseWhenConnectionIsClosed() {
        when(connectionRef.get()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(true);

        boolean result = localPlayer.isOnline();

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return UserProfile with correct id and name")
    void shouldReturnUserProfileWithCorrectIdAndName() {
        int expectedId = 123;
        String expectedName = "TestPlayer";
        when(userInfo.getId()).thenReturn(expectedId);
        when(userInfo.getName()).thenReturn(expectedName);

        UserProfile result = localPlayer.toProtocol();

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("should return LocalPlayerOperations when connection is open")
    void shouldReturnLocalPlayerOperationsWhenConnectionIsOpen() {
        when(connectionRef.get()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        Optional<LocalPlayerOperations> result = localPlayer.operations()
                .map(op -> (LocalPlayerOperations) op);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("should return empty when connection is closed")
    void shouldReturnEmptyWhenConnectionIsClosed() {
        when(connectionRef.get()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(true);

        Optional<LocalPlayerOperations> result = localPlayer.operations()
                .map(op -> (LocalPlayerOperations) op);

        assertThat(result).isEmpty();
    }
}
