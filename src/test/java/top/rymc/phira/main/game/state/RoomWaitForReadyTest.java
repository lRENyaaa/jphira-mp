package top.rymc.phira.main.game.state;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.TestPlayerFactory;
import top.rymc.phira.test.TestServerSetup;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoomWaitForReady")
class RoomWaitForReadyTest {

    private RoomWaitForReady state;
    private RoomGameState capturedNextState;
    private Set<Player> players;
    private Set<Player> monitors;

    @BeforeAll
    static void initServer() throws Exception {
        TestServerSetup.init();
    }

    @BeforeEach
    void setUp() {
        capturedNextState = null;
        Consumer<RoomGameState> stateUpdater = s -> capturedNextState = s;
        var chart = new ChartInfo();
        state = new RoomWaitForReady(stateUpdater, chart);
        players = ConcurrentHashMap.newKeySet();
        monitors = ConcurrentHashMap.newKeySet();
    }

    @Test
    @DisplayName("should transition to RoomPlaying when all players ready")
    void shouldTransitionToRoomPlayingWhenAllPlayersReady() {
        var player1 = TestPlayerFactory.createPlayer(1, "p1");
        var player2 = TestPlayerFactory.createPlayer(2, "p2");
        players.add(player1);
        players.add(player2);

        state.ready(player1, players, monitors);
        state.ready(player2, players, monitors);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }

    @Test
    @DisplayName("should not transition when not all players ready")
    void shouldNotTransitionWhenNotAllPlayersReady() {
        var player1 = TestPlayerFactory.createPlayer(1, "p1");
        var player2 = TestPlayerFactory.createPlayer(2, "p2");
        players.add(player1);
        players.add(player2);

        state.ready(player1, players, monitors);

        assertThat(capturedNextState).isNull();
    }

    @Test
    @DisplayName("should remove player from ready set on leave")
    void shouldRemovePlayerFromReadySetOnLeave() {
        var player1 = TestPlayerFactory.createPlayer(1, "player1");
        var player2 = TestPlayerFactory.createPlayer(2, "player2");
        players.add(player1);
        players.add(player2);
        state.ready(player1, players, monitors);

        state.handleLeave(player1);
        state.ready(player2, players, monitors);

        assertThat(capturedNextState).isNull();
    }

    @Test
    @DisplayName("should allow player to cancel ready")
    void shouldAllowPlayerToCancelReady() {
        var player1 = TestPlayerFactory.createPlayer(1, "player1");
        var player2 = TestPlayerFactory.createPlayer(2, "player2");
        players.add(player1);
        players.add(player2);
        state.ready(player1, players, monitors);
        state.ready(player2, players, monitors);
        capturedNextState = null;

        state.cancelReady(player1, players, monitors);
        state.ready(player1, players, monitors);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }

    @Test
    @DisplayName("should throw when require start in wait for ready state")
    void shouldThrowWhenRequireStartInWaitForReadyState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.requireStart(player, players, monitors))
            .isInstanceOf(GameOperationException.class)
            .hasMessageContaining("不能");
    }

    @Test
    @DisplayName("should throw when abort in wait for ready state")
    void shouldThrowWhenAbortInWaitForReadyState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.abort(player, players, monitors))
            .isInstanceOf(GameOperationException.class)
            .hasMessageContaining("不能");
    }

    @Test
    @DisplayName("should throw when played in wait for ready state")
    void shouldThrowWhenPlayedInWaitForReadyState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.played(player, 123, players, monitors))
            .isInstanceOf(GameOperationException.class)
            .hasMessageContaining("不能");
    }

    @Test
    @DisplayName("should handle join without error")
    void shouldHandleJoinWithoutError() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        state.handleJoin(player);
    }

    @Test
    @DisplayName("should include initiator in ready set")
    void shouldIncludeInitiatorInReadySet() {
        var initiator = TestPlayerFactory.createPlayer(1, "initiator");
        var chart = new ChartInfo();
        var stateWithInitiator = new RoomWaitForReady(s -> capturedNextState = s, chart, initiator);
        players.add(initiator);

        stateWithInitiator.ready(initiator, players, monitors);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }

    @Test
    @DisplayName("should ignore offline players when checking ready status")
    void shouldIgnoreOfflinePlayersWhenCheckingReadyStatus() {
        var onlinePlayer = TestPlayerFactory.createPlayer(1, "online");
        var offlinePlayer = TestPlayerFactory.createOfflinePlayer(2, "offline");
        players.add(onlinePlayer);
        players.add(offlinePlayer);

        state.ready(onlinePlayer, players, monitors);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }
}
