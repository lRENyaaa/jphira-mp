package top.rymc.phira.main.game.room.local;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.state.RoomGameState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LocalRoomPlayerManagementTest {

    @Mock
    private Player player;

    @Mock
    private Player secondPlayer;

    @Mock
    private Player thirdPlayer;

    @Mock
    private PlayerOperations playerOperations;

    @Mock
    private PlayerOperations secondPlayerOperations;

    @Mock
    private ChartInfo chartInfo;

    @Mock
    private Runnable onDestroy;

    private LocalRoom localRoom;
    private MockedStatic<Server> mockedServer;

    @BeforeEach
    void setUp() {
        lenient().when(player.getId()).thenReturn(1);
        lenient().when(player.getName()).thenReturn("Player1");
        lenient().when(secondPlayer.getId()).thenReturn(2);
        lenient().when(secondPlayer.getName()).thenReturn("Player2");
        lenient().when(thirdPlayer.getId()).thenReturn(3);
        lenient().when(thirdPlayer.getName()).thenReturn("Player3");

        mockedServer = mockStatic(Server.class);
    }

    @AfterEach
    void tearDown() {
        if (mockedServer != null) {
            mockedServer.close();
        }
    }

    private LocalRoom createRoomWithSettings(boolean autoDestroy, boolean host, int maxPlayer, boolean locked) {
        LocalRoom.RoomSetting setting = new LocalRoom.RoomSetting(
                autoDestroy,
                host,
                maxPlayer,
                locked,
                false,
                false,
                false
        );
        return new LocalRoom(
                onDestroy,
                "test-room-id",
                setting,
                RoomGameState.Type.SelectChart,
                chartInfo
        );
    }

    @Test
    @DisplayName("should add player to players set when joining as non-monitor")
    void shouldAddPlayerToPlayersSetWhenJoiningAsNonMonitor() {
        localRoom = createRoomWithSettings(false, false, 4, false);

        localRoom.join(player, false);

        assertThat(localRoom.containsPlayer(player)).isTrue();
        assertThat(localRoom.containsMonitor(player)).isFalse();
    }

    @Test
    @DisplayName("should add player to monitors set when joining as monitor")
    void shouldAddPlayerToMonitorsSetWhenJoiningAsMonitor() {
        localRoom = createRoomWithSettings(false, false, 4, false);

        localRoom.join(player, true);

        assertThat(localRoom.containsMonitor(player)).isTrue();
        assertThat(localRoom.containsPlayer(player)).isFalse();
    }

    @Test
    @DisplayName("join throws GameOperationException when room is full")
    void joinThrowsGameOperationExceptionWhenRoomIsFull() {
        localRoom = createRoomWithSettings(false, false, 2, false);
        localRoom.join(player, false);
        localRoom.join(secondPlayer, false);

        assertThatThrownBy(() -> localRoom.join(thirdPlayer, false))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("join throws GameOperationException when room is locked with existing players")
    void joinThrowsGameOperationExceptionWhenRoomIsLockedWithExistingPlayers() {
        localRoom = createRoomWithSettings(false, false, 4, true);
        localRoom.join(player, false);

        assertThatThrownBy(() -> localRoom.join(secondPlayer, false))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("should make first non-monitor player host when host setting is enabled")
    void shouldMakeFirstNonMonitorPlayerHostWhenHostSettingIsEnabled() {
        localRoom = createRoomWithSettings(false, true, 4, false);

        localRoom.join(player, false);

        assertThat(localRoom.isHost(player)).isTrue();
    }

    @Test
    @DisplayName("leave removes player from sets")
    void leaveRemovesPlayerFromSets() {
        localRoom = createRoomWithSettings(false, false, 4, false);
        localRoom.join(player, false);

        localRoom.leave(player);

        assertThat(localRoom.containsPlayer(player)).isFalse();
    }

    @Test
    @DisplayName("host transfers to next player when host leaves")
    void hostTransfersToNextPlayerWhenHostLeaves() {
        localRoom = createRoomWithSettings(false, true, 4, false);
        when(player.operations()).thenReturn(java.util.Optional.of(playerOperations));
        when(secondPlayer.operations()).thenReturn(java.util.Optional.of(secondPlayerOperations));

        localRoom.join(player, false);
        localRoom.join(secondPlayer, false);

        assertThat(localRoom.isHost(player)).isTrue();

        localRoom.leave(player);

        assertThat(localRoom.isHost(secondPlayer)).isTrue();
        verify(secondPlayerOperations).updateHostStatus(true);
    }

    @Test
    @DisplayName("should trigger destroy callback when last player leaves with autoDestroy enabled")
    void shouldTriggerDestroyCallbackWhenLastPlayerLeavesWithAutoDestroyEnabled() {
        localRoom = createRoomWithSettings(true, false, 4, false);

        localRoom.join(player, false);
        localRoom.leave(player);

        verify(onDestroy).run();
    }

    @Test
    @DisplayName("containsPlayer returns true for player in room")
    void containsPlayerReturnsTrueForPlayerInRoom() {
        localRoom = createRoomWithSettings(false, false, 4, false);
        localRoom.join(player, false);

        boolean result = localRoom.containsPlayer(player);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when checking if monitor in room contains monitor")
    void shouldReturnTrueWhenCheckingIfMonitorInRoomContainsMonitor() {
        localRoom = createRoomWithSettings(false, false, 4, false);
        localRoom.join(player, true);

        boolean result = localRoom.containsMonitor(player);

        assertThat(result).isTrue();
    }
}
