package top.rymc.phira.main.game.room.local;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.state.RoomGameState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private PlayerOperations thirdPlayerOperations;

    @Mock
    private ChartInfo chartInfo;

    @Mock
    private Runnable onDestroy;

    private LocalRoom localRoom;
    private MockedStatic<Server> mockedServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(player.getId()).thenReturn(1);
        when(player.getName()).thenReturn("Player1");
        when(secondPlayer.getId()).thenReturn(2);
        when(secondPlayer.getName()).thenReturn("Player2");
        when(thirdPlayer.getId()).thenReturn(3);
        when(thirdPlayer.getName()).thenReturn("Player3");

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
    @DisplayName("join adds player to players set when isMonitor is false")
    void joinAddsPlayerToPlayersSetWhenIsMonitorIsFalse() {
        localRoom = createRoomWithSettings(false, false, 4, false);

        localRoom.join(player, false);

        assertThat(localRoom.containsPlayer(player)).isTrue();
        assertThat(localRoom.containsMonitor(player)).isFalse();
    }

    @Test
    @DisplayName("join adds player to monitors set when isMonitor is true")
    void joinAddsPlayerToMonitorsSetWhenIsMonitorIsTrue() {
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
    @DisplayName("first non-monitor player becomes host when host setting enabled")
    void firstNonMonitorPlayerBecomesHostWhenHostSettingEnabled() {
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
    @DisplayName("destroy callback triggered when last player leaves with autoDestroy enabled")
    void destroyCallbackTriggeredWhenLastPlayerLeavesWithAutoDestroyEnabled() {
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
    @DisplayName("containsMonitor returns true for monitor in room")
    void containsMonitorReturnsTrueForMonitorInRoom() {
        localRoom = createRoomWithSettings(false, false, 4, false);
        localRoom.join(player, true);

        boolean result = localRoom.containsMonitor(player);

        assertThat(result).isTrue();
    }
}
