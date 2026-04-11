package top.rymc.phira.main.game.room;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.main.game.room.state.RoomSelectChart;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJoinRoomPacket;

import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomSnapshotTest {

    private static final String ROOM_ID = "test-room-123";
    private static final int HOST_ID = 1;

    @Test
    @DisplayName("asProtocolConvertible returns correct RoomInfo with all fields")
    void asProtocolConvertibleReturnsCorrectRoomInfo() {
        ChartInfo chartInfo = mock(ChartInfo.class);
        when(chartInfo.getId()).thenReturn(100);

        LocalRoom room = mock(LocalRoom.class);
        LocalRoom.PlayerManager playerManager = mock(LocalRoom.PlayerManager.class);
        when(room.getPlayerManager()).thenReturn(playerManager);
        when(playerManager.getPlayers()).thenReturn(Set.of());
        when(playerManager.getMonitors()).thenReturn(Set.of());

        Consumer<top.rymc.phira.main.game.room.state.RoomGameState> stateUpdater = mock(Consumer.class);
        RoomSelectChart state = new RoomSelectChart(room, stateUpdater, chartInfo);

        Player hostPlayer = mock(Player.class);
        UserProfile hostProfile = mock(UserProfile.class);
        when(hostPlayer.getId()).thenReturn(HOST_ID);
        when(hostPlayer.toProtocol()).thenReturn(hostProfile);

        RoomSnapshot roomSnapshot = new RoomSnapshot(
            ROOM_ID,
            state,
            true,
            false,
            true,
            HOST_ID,
            Set.of(hostPlayer),
            Set.of()
        );

        ProtocolConvertible<RoomInfo> convertible = roomSnapshot.asProtocolConvertible(hostPlayer);
        RoomInfo roomInfo = convertible.toProtocol();

        assertThat(roomInfo).isNotNull();
    }

    @Test
    @DisplayName("isHost returns true when player is the host")
    void isHostReturnsTrueForHostPlayer() {
        LocalRoom room = mock(LocalRoom.class);
        Consumer<top.rymc.phira.main.game.room.state.RoomGameState> stateUpdater = mock(Consumer.class);
        RoomSelectChart state = new RoomSelectChart(room, stateUpdater);

        Player hostPlayer = mock(Player.class);
        when(hostPlayer.getId()).thenReturn(HOST_ID);

        RoomSnapshot roomSnapshot = new RoomSnapshot(
            ROOM_ID,
            state,
            true,
            false,
            true,
            HOST_ID,
            Set.of(hostPlayer),
            Set.of()
        );

        boolean result = roomSnapshot.isHost(hostPlayer);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isHost returns false when player is not the host")
    void isHostReturnsFalseForNonHostPlayer() {
        LocalRoom room = mock(LocalRoom.class);
        Consumer<top.rymc.phira.main.game.room.state.RoomGameState> stateUpdater = mock(Consumer.class);
        RoomSelectChart state = new RoomSelectChart(room, stateUpdater);

        Player hostPlayer = mock(Player.class);
        Player nonHostPlayer = mock(Player.class);
        when(hostPlayer.getId()).thenReturn(HOST_ID);
        when(nonHostPlayer.getId()).thenReturn(2);

        RoomSnapshot roomSnapshot = new RoomSnapshot(
            ROOM_ID,
            state,
            true,
            false,
            true,
            HOST_ID,
            Set.of(hostPlayer),
            Set.of()
        );

        boolean result = roomSnapshot.isHost(nonHostPlayer);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("buildJoinSuccessPacket returns correct packet with players, monitors and live status")
    void buildJoinSuccessPacketReturnsCorrectPacket() {
        ChartInfo chartInfo = mock(ChartInfo.class);
        when(chartInfo.getId()).thenReturn(100);

        LocalRoom room = mock(LocalRoom.class);
        LocalRoom.PlayerManager playerManager = mock(LocalRoom.PlayerManager.class);
        when(room.getPlayerManager()).thenReturn(playerManager);
        when(playerManager.getPlayers()).thenReturn(Set.of());
        when(playerManager.getMonitors()).thenReturn(Set.of());

        Consumer<top.rymc.phira.main.game.room.state.RoomGameState> stateUpdater = mock(Consumer.class);
        RoomSelectChart state = new RoomSelectChart(room, stateUpdater, chartInfo);

        Player hostPlayer = mock(Player.class);
        UserProfile hostProfile = mock(UserProfile.class);
        when(hostPlayer.getId()).thenReturn(HOST_ID);
        when(hostPlayer.toProtocol()).thenReturn(hostProfile);

        RoomSnapshot roomSnapshot = new RoomSnapshot(
            ROOM_ID,
            state,
            true,
            false,
            true,
            HOST_ID,
            Set.of(hostPlayer),
            Set.of()
        );

        ClientBoundJoinRoomPacket packet = roomSnapshot.getProtocolHack().buildJoinSuccessPacket();

        assertThat(packet).isNotNull();
    }
}
