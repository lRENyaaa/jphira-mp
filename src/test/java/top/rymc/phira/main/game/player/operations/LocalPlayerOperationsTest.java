package top.rymc.phira.main.game.player.operations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.Playing;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

class LocalPlayerOperationsTest {

    @Test
    @DisplayName("updateHostStatus executes without exception")
    void updateHostStatusExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.updateHostStatus(true));
    }

    @Test
    @DisplayName("enterState executes without exception")
    void enterStateExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);
        Playing state = new Playing();

        assertThatNoException().isThrownBy(() -> operations.enterState(state));
    }

    @Test
    @DisplayName("selectChart executes without exception")
    void selectChartExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.selectChart(100, "TestChart", 1));
    }

    @Test
    @DisplayName("lockRoom executes without exception")
    void lockRoomExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.lockRoom(true));
    }

    @Test
    @DisplayName("cycleRoom executes without exception")
    void cycleRoomExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.cycleRoom(true));
    }

    @Test
    @DisplayName("memberJoined executes without exception")
    void memberJoinedExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.memberJoined(2, "Player2", false));
    }

    @Test
    @DisplayName("memberLeft executes without exception")
    void memberLeftExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.memberLeft(2, "Player2"));
    }

    @Test
    @DisplayName("memberReady executes without exception")
    void memberReadyExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.memberReady(2));
    }

    @Test
    @DisplayName("memberCancelReady executes without exception")
    void memberCancelReadyExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.memberCancelReady(2));
    }

    @Test
    @DisplayName("gameRequireStart executes without exception")
    void gameRequireStartExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.gameRequireStart(1));
    }

    @Test
    @DisplayName("gameStartPlaying executes without exception")
    void gameStartPlayingExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.gameStartPlaying());
    }

    @Test
    @DisplayName("gameAbort executes without exception")
    void gameAbortExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.gameAbort(1));
    }

    @Test
    @DisplayName("receiveTouchStream executes without exception")
    void receiveTouchStreamExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);
        List<TouchFrame> frames = List.of(mock(TouchFrame.class));

        assertThatNoException().isThrownBy(() -> operations.receiveTouchStream(2, frames));
    }

    @Test
    @DisplayName("receiveJudgeStream executes without exception")
    void receiveJudgeStreamExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);
        List<JudgeEvent> events = List.of(mock(JudgeEvent.class));

        assertThatNoException().isThrownBy(() -> operations.receiveJudgeStream(2, events));
    }

    @Test
    @DisplayName("receiveChat executes without exception")
    void receiveChatExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.receiveChat(2, "Hello"));
    }

    @Test
    @DisplayName("gameEnd executes without exception")
    void gameEndExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.gameEnd());
    }

    @Test
    @DisplayName("gamePlayed executes without exception")
    void gamePlayedExecutesWithoutException() {
        PlayerConnection connection = mock(PlayerConnection.class);
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.gamePlayed(1, 1000000, 99.5f, true));
    }

    @Test
    @DisplayName("operations instance is created successfully")
    void operationsInstanceIsCreatedSuccessfully() {
        PlayerConnection connection = mock(PlayerConnection.class);

        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThat(operations).isNotNull();
    }
}
