package top.rymc.phira.main.game.room.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
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
    private ChartInfo chartInfo;

    private Consumer<RoomGameState> stateUpdater;

    private RoomWaitForReady roomWaitForReady;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stateUpdater = mock(Consumer.class);
        lenient().when(room.getPlayerManager()).thenReturn(playerManager);
        lenient().when(player.getId()).thenReturn(1);
        lenient().when(secondPlayer.getId()).thenReturn(2);
        lenient().when(player.isOnline()).thenReturn(true);
        lenient().when(secondPlayer.isOnline()).thenReturn(true);
    }

    @Test
    @DisplayName("ready adds player to ready set")
    void readyAddsPlayerToReadySet() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> ignored = mockStatic(Server.class)) {
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

        try (MockedStatic<Server> ignored = mockStatic(Server.class)) {
            roomWaitForReady.ready(player);

            ArgumentCaptor<Consumer<PlayerOperations>> captor = ArgumentCaptor.forClass(Consumer.class);
            verify(playerManager).broadcast(captor.capture());
            captor.getValue().accept(playerOperations);
            verify(playerOperations).memberReady(player.getId());
        }
    }

    @Test
    @DisplayName("should transition state to playing when all players ready")
    void shouldTransitionStateToPlayingWhenAllPlayersReady() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> ignored = mockStatic(Server.class)) {
            roomWaitForReady.ready(player);

            ArgumentCaptor<RoomGameState> stateCaptor = ArgumentCaptor.forClass(RoomGameState.class);
            verify(stateUpdater).accept(stateCaptor.capture());
            assertThat(stateCaptor.getValue()).isInstanceOf(RoomPlaying.class);
        }
    }

    @Test
    @DisplayName("should remove player from ready set when cancel ready")
    void shouldRemovePlayerFromReadySetWhenCancelReady() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> ignored = mockStatic(Server.class)) {
            roomWaitForReady.ready(player);
            roomWaitForReady.cancelReady(player);

            roomWaitForReady.ready(secondPlayer);

            verify(stateUpdater, never()).accept(any(RoomPlaying.class));
        }
    }

    @Test
    @DisplayName("should broadcast member cancel ready to all players when cancel ready")
    void shouldBroadcastMemberCancelReadyToAllPlayersWhenCancelReady() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        try (MockedStatic<Server> ignored = mockStatic(Server.class)) {
            roomWaitForReady.cancelReady(player);

            ArgumentCaptor<Consumer<PlayerOperations>> captor = ArgumentCaptor.forClass(Consumer.class);
            verify(playerManager).broadcast(captor.capture());
            captor.getValue().accept(playerOperations);
            verify(playerOperations).memberCancelReady(player.getId());
        }
    }

    @Test
    @DisplayName("should remove player from ready set when handle leave")
    void shouldRemovePlayerFromReadySetWhenHandleLeave() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());

        try (MockedStatic<Server> ignored = mockStatic(Server.class)) {
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
    @DisplayName("should throw invalid state exception when abort")
    void shouldThrowInvalidStateExceptionWhenAbort() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        assertThatThrownBy(() -> roomWaitForReady.abort(player))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("should throw invalid state exception when played")
    void shouldThrowInvalidStateExceptionWhenPlayed() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        assertThatThrownBy(() -> roomWaitForReady.played(player, 123))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("should return wait for ready state when to protocol")
    void shouldReturnWaitForReadyStateWhenToProtocol() {
        roomWaitForReady = new RoomWaitForReady(room, stateUpdater, chartInfo);

        GameState result = roomWaitForReady.toProtocol();

        assertThat(result).isInstanceOf(WaitForReady.class);
    }
}
