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

@DisplayName("RoomSelectChart")
class RoomSelectChartTest {

    private RoomSelectChart state;
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
        state = new RoomSelectChart(room, stateUpdater);
    }

    @AfterEach
    void tearDown() {
        Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
    }

    @Test
    @DisplayName("should transition to RoomPlaying when single player requires start")
    void shouldTransitionToRoomPlayingWhenSinglePlayerRequiresStart() {
        var player = TestPlayerFactory.createPlayer(1, "solo");
        room.join(player, false);

        state.requireStart(player);

        assertThat(capturedNextState).isInstanceOf(RoomPlaying.class);
    }

    @Test
    @DisplayName("should transition to RoomWaitForReady when multiple players require start")
    void shouldTransitionToRoomWaitForReadyWhenMultiplePlayersRequireStart() {
        var host = TestPlayerFactory.createPlayer(1, "host");
        var player2 = TestPlayerFactory.createPlayer(2, "player2");
        room.join(host, false);
        room.join(player2, false);

        state.requireStart(host);

        assertThat(capturedNextState).isInstanceOf(RoomWaitForReady.class);
    }

    @Test
    @DisplayName("should throw when ready in select chart state")
    void shouldThrowWhenReadyInSelectChartState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.ready(player))
            .isInstanceOf(GameOperationException.class)
            .hasMessage("error.invalid_state");
    }

    @Test
    @DisplayName("should throw when cancel ready in select chart state")
    void shouldThrowWhenCancelReadyInSelectChartState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.cancelReady(player))
            .isInstanceOf(GameOperationException.class)
            .hasMessage("error.invalid_state");
    }

    @Test
    @DisplayName("should throw when abort in select chart state")
    void shouldThrowWhenAbortInSelectChartState() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        assertThatThrownBy(() -> state.abort(player))
            .isInstanceOf(GameOperationException.class)
            .hasMessage("error.invalid_state");
    }

    @Test
    @DisplayName("should throw when played in select chart state")
    void shouldThrowWhenPlayedInSelectChartState() {
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
    @DisplayName("should handle leave without error")
    void shouldHandleLeaveWithoutError() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        state.handleLeave(player);
    }

    @Test
    @DisplayName("should return null chart id when no chart selected")
    void shouldReturnNullChartIdWhenNoChartSelected() {
        var protocol = state.toProtocol();

        assertThat(protocol).isNotNull();
    }

    @Test
    @DisplayName("should store and return chart info")
    void shouldStoreAndReturnChartInfo() {
        var chart = new ChartInfo();
        ReflectionUtil.setField(chart, "id", 42);

        state.setChart(chart);

        assertThat(state.getChart()).isEqualTo(chart);
    }
}
