package top.rymc.phira.main.game.room.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.protocol.data.state.SelectChart;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RoomSelectChartStateTest {

    @Mock
    private LocalRoom room;

    @Mock
    private LocalRoom.PlayerManager playerManager;

    @Mock
    private Player player;

    @Mock
    private PlayerOperations playerOperations;

    @Mock
    private ChartInfo chartInfo;

    private AtomicReference<RoomGameState> capturedState;
    private Consumer<RoomGameState> stateUpdater;
    private RoomSelectChart roomSelectChart;

    @BeforeEach
    void setUp() {
        capturedState = new AtomicReference<>();
        stateUpdater = capturedState::set;

        lenient().when(room.getPlayerManager()).thenReturn(playerManager);
        lenient().when(player.getId()).thenReturn(1);
        lenient().when(player.operations()).thenReturn(Optional.of(playerOperations));
        lenient().when(chartInfo.getId()).thenReturn(100);

        roomSelectChart = new RoomSelectChart(room, stateUpdater);
    }

    @Test
    @DisplayName("should transition to Playing state when requireStart with single player")
    void shouldTransitionToPlayingStateWhenRequireStartWithSinglePlayer() {
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Collections.emptySet());

        try (MockedStatic<Server> ignored = mockStatic(Server.class)) {
            roomSelectChart.requireStart(player);

            assertThat(capturedState.get()).isInstanceOf(RoomPlaying.class);
        }
    }

    @Test
    @DisplayName("should transition to WaitForReady state when requireStart with multiple players")
    void shouldTransitionToWaitForReadyStateWhenRequireStartWithMultiplePlayers() {
        Player anotherPlayer = mock(Player.class);
        lenient().when(anotherPlayer.getId()).thenReturn(2);
        lenient().when(anotherPlayer.operations()).thenReturn(Optional.of(mock(PlayerOperations.class)));

        when(playerManager.getPlayers()).thenReturn(Set.of(player, anotherPlayer));
        when(playerManager.getMonitors()).thenReturn(Collections.emptySet());

        try (MockedStatic<Server> ignored = mockStatic(Server.class)) {
            roomSelectChart.requireStart(player);

            assertThat(capturedState.get()).isInstanceOf(RoomWaitForReady.class);
        }
    }

    @Test
    @DisplayName("should throw invalid state exception when ready")
    void shouldThrowInvalidStateExceptionWhenReady() {
        assertThatThrownBy(() -> roomSelectChart.ready(player))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.invalid_state");
    }

    @Test
    @DisplayName("should throw invalid state exception when cancelReady")
    void shouldThrowInvalidStateExceptionWhenCancelReady() {
        assertThatThrownBy(() -> roomSelectChart.cancelReady(player))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.invalid_state");
    }

    @Test
    @DisplayName("should throw invalid state exception when abort")
    void shouldThrowInvalidStateExceptionWhenAbort() {
        assertThatThrownBy(() -> roomSelectChart.abort(player))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.invalid_state");
    }

    @Test
    @DisplayName("played throws invalid state exception")
    void playedThrowsInvalidState() {
        assertThatThrownBy(() -> roomSelectChart.played(player, 1))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.invalid_state");
    }

    @Test
    @DisplayName("toProtocol returns SelectChart state with null chart id when chart is null")
    void toProtocolReturnsSelectChartState() {
        var protocol = roomSelectChart.toProtocol();

        assertThat(protocol).isInstanceOf(SelectChart.class);
    }

    @Test
    @DisplayName("should return SelectChart state with chart id when toProtocol with chart set")
    void shouldReturnSelectChartStateWithChartIdWhenToProtocolWithChartSet() {
        RoomSelectChart chartWithState = new RoomSelectChart(room, stateUpdater, chartInfo);

        var protocol = chartWithState.toProtocol();

        assertThat(protocol).isInstanceOf(SelectChart.class);
    }
}
