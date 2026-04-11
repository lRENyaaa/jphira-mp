package top.rymc.phira.main.game.room.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.WaitForReady;

import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomWaitForReadyStateTest {

    @Mock
    private LocalRoom room;

    @Mock
    private LocalRoom.PlayerManager playerManager;

    @Mock
    private Player player;

    @Mock
    private Player secondPlayer;

    @Mock
    private PlayerOperations playerOperations;

    @Mock
    private PlayerOperations secondPlayerOperations;

    @Mock
    private ChartInfo chartInfo;

    @SuppressWarnings("unchecked")
    private Consumer<RoomGameState> stateUpdater;

    private RoomWaitForReady roomWaitForReady;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stateUpdater = mock(Consumer.class);
        when(room.getPlayerManager()).thenReturn(playerManager);
        when(player.getId()).thenReturn(1);
        when(secondPlayer.getId()).thenReturn(2);
        when(player.isOnline()).thenReturn(true);
        when(secondPlayer.isOnline()).thenReturn(true);
    }

    @Test
    @DisplayName("ready adds player to ready set")
    void readyAddsPlayerToReadySet() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
            roomWaitForReady.ready(player);

            roomWaitForReady.ready(player);
        }
    }

    @Test
    @DisplayName("ready broadcasts member ready to all players")
    void readyBroadcastsMemberReady() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
            roomWaitForReady.ready(player);

            ArgumentCaptor<Consumer<PlayerOperations>> captor = ArgumentCaptor.forClass(Consumer.class);
            verify(playerManager).broadcast(captor.capture());
            captor.getValue().accept(playerOperations);
            verify(playerOperations).memberReady(player.getId());
        }
    }

    @Test
    @DisplayName("all players ready transitions state to playing")
    void allPlayersReadyTransitionsToPlaying() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
            roomWaitForReady.ready(player);

            ArgumentCaptor<RoomGameState> stateCaptor = ArgumentCaptor.forClass(RoomGameState.class);
            verify(stateUpdater).accept(stateCaptor.capture());
            assertThat(stateCaptor.getValue()).isInstanceOf(RoomPlaying.class);
        }
    }

    @Test
    @DisplayName("cancel ready removes player from ready set")
    void cancelReadyRemovesFromReadySet() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
            roomWaitForReady.ready(player);
            roomWaitForReady.cancelReady(player);

            roomWaitForReady.ready(secondPlayer);

            verify(stateUpdater, never()).accept(any(RoomPlaying.class));
        }
    }

    @Test
    @DisplayName("cancel ready broadcasts member cancel ready to all players")
    void cancelReadyBroadcastsMemberCancelReady() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
            roomWaitForReady.cancelReady(player);

            ArgumentCaptor<Consumer<PlayerOperations>> captor = ArgumentCaptor.forClass(Consumer.class);
            verify(playerManager).broadcast(captor.capture());
            captor.getValue().accept(playerOperations);
            verify(playerOperations).memberCancelReady(player.getId());
        }
    }

    @Test
    @DisplayName("handle leave removes player from ready set")
    void handleLeaveRemovesFromReadySet() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
            roomWaitForReady.ready(player);
            roomWaitForReady.handleLeave(player);
            roomWaitForReady.ready(secondPlayer);

            verify(stateUpdater, never()).accept(any(RoomPlaying.class));
        }
    }

    @Test
    @DisplayName("require start throws invalid state exception")
    void requireStartThrowsInvalidState() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        assertThatThrownBy(() -> roomWaitForReady.requireStart(player))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("abort throws invalid state exception")
    void abortThrowsInvalidState() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        assertThatThrownBy(() -> roomWaitForReady.abort(player))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("played throws invalid state exception")
    void playedThrowsInvalidState() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        assertThatThrownBy(() -> roomWaitForReady.played(player, 123))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("to protocol returns wait for ready state")
    void toProtocolReturnsWaitForReadyState() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        GameState result = roomWaitForReady.toProtocol();

        assertThat(result).isInstanceOf(WaitForReady.class);
    }
}
