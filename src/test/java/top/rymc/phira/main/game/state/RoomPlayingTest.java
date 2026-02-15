package top.rymc.phira.main.game.state;

import org.junit.jupiter.api.AfterAll;
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
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.test.MockPhiraServer;
import top.rymc.phira.test.TestServerSetup;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoomPlaying")
class RoomPlayingTest {

    private static MockPhiraServer mockServer;

    private RoomPlaying state;
    private RoomGameState capturedNextState;
    private Room room;

    @BeforeAll
    static void setUpServer() throws Exception {
        mockServer = new MockPhiraServer();
        mockServer.start();
        PhiraFetcher.setHost(mockServer.getBaseUrl());
        TestServerSetup.init();
    }

    @AfterAll
    static void tearDownServer() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        capturedNextState = null;
        Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
        room = RoomManager.createRoom("test-room", new Room.RoomSetting());
        Consumer<RoomGameState> stateUpdater = s -> capturedNextState = s;
        var chart = new ChartInfo();
        state = new RoomPlaying(room, stateUpdater, chart);

        PhiraFetcher.getRecordCache().clear();
        mockServer.addRecord(1001, 1, 1, 1000000, 99.5f, true);
        mockServer.addRecord(1002, 2, 1, 950000, 98.0f, false);
    }

    @AfterEach
    void tearDown() {
        PhiraFetcher.getRecordCache().clear();
        Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
    }

    @Test
    @DisplayName("should transition to RoomSelectChart when all players done")
    void shouldTransitionToRoomSelectChartWhenAllPlayersDone() {
        var player1 = TestPlayerFactory.createPlayer(1, "p1");
        var player2 = TestPlayerFactory.createPlayer(2, "p2");
        room.join(player1, false);
        room.join(player2, false);

        state.played(player1, 1001);
        state.played(player2, 1002);

        assertThat(capturedNextState).isInstanceOf(RoomSelectChart.class);
    }

    @Test
    @DisplayName("should not transition when not all players done")
    void shouldNotTransitionWhenNotAllPlayersDone() {
        var player1 = TestPlayerFactory.createPlayer(1, "p1");
        var player2 = TestPlayerFactory.createPlayer(2, "p2");
        room.join(player1, false);
        room.join(player2, false);

        state.played(player1, 1001);

        assertThat(capturedNextState).isNull();
    }

    @Test
    @DisplayName("should transition to RoomSelectChart on abort")
    void shouldTransitionToRoomSelectChartOnAbort() {
        var player = TestPlayerFactory.createPlayer(1, "player");
        room.join(player, false);

        state.abort(player);

        assertThat(capturedNextState).isInstanceOf(RoomSelectChart.class);
    }

    @Test
    @DisplayName("should allow multiple aborts")
    void shouldAllowMultipleAborts() {
        var player1 = TestPlayerFactory.createPlayer(1, "p1");
        var player2 = TestPlayerFactory.createPlayer(2, "p2");
        room.join(player1, false);
        room.join(player2, false);

        state.abort(player1);
        state.abort(player2);

        assertThat(capturedNextState).isInstanceOf(RoomSelectChart.class);
    }

    @Test
    @DisplayName("should throw when require start in playing state")
    void shouldThrowWhenRequireStartInPlayingState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.requireStart(player))
            .isInstanceOf(GameOperationException.class)
            .hasMessage("error.invalid_state");
    }

    @Test
    @DisplayName("should throw when ready in playing state")
    void shouldThrowWhenReadyInPlayingState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.ready(player))
            .isInstanceOf(GameOperationException.class)
            .hasMessage("error.invalid_state");
    }

    @Test
    @DisplayName("should throw when cancel ready in playing state")
    void shouldThrowWhenCancelReadyInPlayingState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.cancelReady(player))
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
    @DisplayName("should handle leave without error")
    void shouldHandleLeaveWithoutError() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        state.handleLeave(player);
    }

    @Test
    @DisplayName("should ignore offline players when checking done status")
    void shouldIgnoreOfflinePlayersWhenCheckingDoneStatus() {
        var onlinePlayer = TestPlayerFactory.createPlayer(1, "online");
        var offlinePlayer = TestPlayerFactory.createOfflinePlayer(2, "offline");
        room.join(onlinePlayer, false);
        room.join(offlinePlayer, false);

        mockServer.addRecord(1001, 1, 1, 1000000, 99.5f, true);

        state.played(onlinePlayer, 1001);

        assertThat(capturedNextState).isInstanceOf(RoomSelectChart.class);
    }

    @Test
    @DisplayName("should ignore monitors when checking done status")
    void shouldIgnoreMonitorsWhenCheckingDoneStatus() {
        var player = TestPlayerFactory.createPlayer(1, "player");
        var monitor = TestPlayerFactory.createPlayer(2, "monitor");
        room.join(player, false);
        room.join(monitor, true);

        mockServer.addRecord(1001, 1, 1, 1000000, 99.5f, true);

        state.played(player, 1001);

        assertThat(capturedNextState).isInstanceOf(RoomSelectChart.class);
    }
}
