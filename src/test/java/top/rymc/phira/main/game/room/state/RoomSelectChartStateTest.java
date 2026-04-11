package top.rymc.phira.main.game.room.state;

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
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.protocol.data.state.SelectChart;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
        MockitoAnnotations.openMocks(this);
        capturedState = new AtomicReference<>();
        stateUpdater = capturedState::set;

        when(room.getPlayerManager()).thenReturn(playerManager);
        when(player.getId()).thenReturn(1);
        when(player.operations()).thenReturn(Optional.of(playerOperations));
        when(chartInfo.getId()).thenReturn(100);

        roomSelectChart = new RoomSelectChart(room, stateUpdater);
    }

    @Test
    @DisplayName("requireStart with single player transitions to Playing state")
    void requireStartWithSinglePlayerTransitionsToPlaying() {
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Collections.emptySet());

        try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
            roomSelectChart.requireStart(player);

            assertThat(capturedState.get()).isInstanceOf(RoomPlaying.class);
        }
    }

    @Test
    @DisplayName("requireStart with multiple players transitions to WaitForReady state")
    void requireStartWithMultiplePlayersTransitionsToWaitForReady() {
        Player anotherPlayer = mock(Player.class);
        when(anotherPlayer.getId()).thenReturn(2);
        when(anotherPlayer.operations()).thenReturn(Optional.of(mock(PlayerOperations.class)));

        when(playerManager.getPlayers()).thenReturn(Set.of(player, anotherPlayer));
        when(playerManager.getMonitors()).thenReturn(Collections.emptySet());

        try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
            roomSelectChart.requireStart(player);

            assertThat(capturedState.get()).isInstanceOf(RoomWaitForReady.class);
        }
    }

    @Test
    @DisplayName("ready throws invalid state exception")
    void readyThrowsInvalidState() {
        assertThatThrownBy(() -> roomSelectChart.ready(player))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.invalid_state");
    }

    @Test
    @DisplayName("cancelReady throws invalid state exception")
    void cancelReadyThrowsInvalidState() {
        assertThatThrownBy(() -> roomSelectChart.cancelReady(player))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.invalid_state");
    }

    @Test
    @DisplayName("abort throws invalid state exception")
    void abortThrowsInvalidState() {
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
    @DisplayName("toProtocol returns SelectChart state with chart id when chart is set")
    void toProtocolReturnsSelectChartStateWithChartId() {
        RoomSelectChart chartWithState = new RoomSelectChart(room, stateUpdater, chartInfo);

        var protocol = chartWithState.toProtocol();

        assertThat(protocol).isInstanceOf(SelectChart.class);
    }
}
