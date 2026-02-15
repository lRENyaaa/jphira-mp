package top.rymc.phira.main.game.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.ReflectionUtil;
import top.rymc.phira.main.game.TestPlayerFactory;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomManager;
import top.rymc.phira.test.TestServerSetup;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoomWaitForReady")
class RoomWaitForReadyTest {

    private RoomWaitForReady state;
    private RoomGameState capturedNextState;
    private Room room;

    @BeforeAll
    static void initServer() throws Exception {
        TestServerSetup.init();
    }

    @BeforeEach
    void setUp() {
        capturedNextState = null;
        Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
        room = RoomManager.createRoom("test-room", new Room.RoomSetting());
        Consumer<RoomGameState> stateUpdater = s -> capturedNextState = s;
        var chart = new ChartInfo();
        state = new RoomWaitForReady(room, stateUpdater, chart);
    }

    @AfterEach
    void tearDown() {
        Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
    }

    @Test
    @DisplayName("should transition to RoomPlaying when all players ready")
    void shouldTransitionToRoomPlayingWhenAllPlayersReady() {
        var player1 = TestPlayerFactory.createPlayer(1, "p1");
        var player2 = TestPlayerFactory.createPlayer(2, "p2");
        room.join(player1, false);
        room.join(player2, false);

        state.ready(player1);
        state.ready(player2);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }

    @Test
    @DisplayName("should not transition when not all players ready")
    void shouldNotTransitionWhenNotAllPlayersReady() {
        var player1 = TestPlayerFactory.createPlayer(1, "p1");
        var player2 = TestPlayerFactory.createPlayer(2, "p2");
        room.join(player1, false);
        room.join(player2, false);

        state.ready(player1);

        assertThat(capturedNextState).isNull();
    }

    @Test
    @DisplayName("should remove player from ready set on leave")
    void shouldRemovePlayerFromReadySetOnLeave() {
        var player1 = TestPlayerFactory.createPlayer(1, "player1");
        var player2 = TestPlayerFactory.createPlayer(2, "player2");
        room.join(player1, false);
        room.join(player2, false);
        state.ready(player1);

        state.handleLeave(player1);
        state.ready(player2);

        assertThat(capturedNextState).isNull();
    }

    @Test
    @DisplayName("should allow player to cancel ready")
    void shouldAllowPlayerToCancelReady() {
        var player1 = TestPlayerFactory.createPlayer(1, "player1");
        var player2 = TestPlayerFactory.createPlayer(2, "player2");
        room.join(player1, false);
        room.join(player2, false);
        state.ready(player1);
        state.ready(player2);
        capturedNextState = null;

        state.cancelReady(player1);
        state.ready(player1);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }

    @Test
    @DisplayName("should throw when require start in wait for ready state")
    void shouldThrowWhenRequireStartInWaitForReadyState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.requireStart(player))
            .isInstanceOf(GameOperationException.class)
            .hasMessage("error.invalid_state");
    }

    @Test
    @DisplayName("should throw when abort in wait for ready state")
    void shouldThrowWhenAbortInWaitForReadyState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.abort(player))
            .isInstanceOf(GameOperationException.class)
            .hasMessage("error.invalid_state");
    }

    @Test
    @DisplayName("should throw when played in wait for ready state")
    void shouldThrowWhenPlayedInWaitForReadyState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.played(player, 123))
            .isInstanceOf(GameOperationException.class)
            .hasMessage("error.invalid_state");
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
        room.join(initiator, false);
        var chart = new ChartInfo();
        var stateWithInitiator = new RoomWaitForReady(room, s -> capturedNextState = s, chart, initiator);

        stateWithInitiator.ready(initiator);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }

    @Test
    @DisplayName("should ignore offline players when checking ready status")
    void shouldIgnoreOfflinePlayersWhenCheckingReadyStatus() {
        var onlinePlayer = TestPlayerFactory.createPlayer(1, "online");
        var offlinePlayer = TestPlayerFactory.createOfflinePlayer(2, "offline");
        room.join(onlinePlayer, false);
        room.join(offlinePlayer, false);

        state.ready(onlinePlayer);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }
}
