package top.rymc.phira.main.network.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundCreateRoomPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJoinRoomPacket;
import top.rymc.phira.test.TestServerSetup;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class PlayHandlerTest {

    private PlayerConnection connection;
    private LocalPlayer player;
    private PlayHandler playHandler;
    private Map<String, Room> roomsBackup;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        TestServerSetup.init();

        connection = mock(PlayerConnection.class);
        player = mock(LocalPlayer.class);
        when(player.getConnection()).thenReturn(connection);
        when(connection.send(any())).thenReturn(Optional.empty());

        playHandler = PlayHandler.create(player);

        Field roomsField = RoomManager.class.getDeclaredField("ROOMS");
        roomsField.setAccessible(true);
        roomsBackup = new ConcurrentHashMap<>((Map<String, Room>) roomsField.get(null));
        ((Map<String, Room>) roomsField.get(null)).clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        Field roomsField = RoomManager.class.getDeclaredField("ROOMS");
        roomsField.setAccessible(true);
        ((Map<String, Room>) roomsField.get(null)).clear();
        ((Map<String, Room>) roomsField.get(null)).putAll(roomsBackup);
    }

    @Test
    @DisplayName("should send failed packet when handleCreateRoom fails")
    void shouldSendFailedPacketWhenHandleCreateRoomFails() {
        playHandler.handle((top.rymc.phira.protocol.packet.serverbound.ServerBoundCreateRoomPacket) null);

        verify(connection).send(any(ClientBoundCreateRoomPacket.class));
    }

    @Test
    @DisplayName("should send failed packet when handleJoinRoom with room not found")
    void shouldSendFailedPacketWhenHandleJoinRoomWithRoomNotFound() {
        playHandler.handle((top.rymc.phira.protocol.packet.serverbound.ServerBoundJoinRoomPacket) null);

        verify(connection).send(any(ClientBoundJoinRoomPacket.class));
    }

    @Test
    @DisplayName("should return player associated with handler when getPlayer")
    void shouldReturnPlayerAssociatedWithHandlerWhenGetPlayer() {
        assertThat(playHandler.getPlayer()).isSameAs(player);
    }

    @Test
    @DisplayName("should kick player when onUnhandledPacket")
    void shouldKickPlayerWhenOnUnhandledPacket() {
        playHandler.onUnhandledPacket(null);

        verify(player).kick();
    }
}
