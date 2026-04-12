package top.rymc.phira.main.game.player.operations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.Playing;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LocalPlayerOperationsTest {

    @Mock
    private PlayerConnection connection;

    @Test
    @DisplayName("should execute updateHostStatus without exception")
    void shouldExecuteUpdateHostStatusWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.updateHostStatus(true));
    }

    @Test
    @DisplayName("should execute enterState without exception")
    void shouldExecuteEnterStateWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);
        Playing state = new Playing();

        assertThatNoException().isThrownBy(() -> operations.enterState(state));
    }

    @Test
    @DisplayName("should execute selectChart without exception")
    void shouldExecuteSelectChartWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.selectChart(100, "TestChart", 1));
    }

    @Test
    @DisplayName("should execute lockRoom without exception")
    void shouldExecuteLockRoomWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.lockRoom(true));
    }

    @Test
    @DisplayName("should execute cycleRoom without exception")
    void shouldExecuteCycleRoomWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.cycleRoom(true));
    }

    @Test
    @DisplayName("should execute memberJoined without exception")
    void shouldExecuteMemberJoinedWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.memberJoined(2, "Player2", false));
    }

    @Test
    @DisplayName("should execute memberLeft without exception")
    void shouldExecuteMemberLeftWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.memberLeft(2, "Player2"));
    }

    @Test
    @DisplayName("should execute memberReady without exception")
    void shouldExecuteMemberReadyWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.memberReady(2));
    }

    @Test
    @DisplayName("should execute memberCancelReady without exception")
    void shouldExecuteMemberCancelReadyWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.memberCancelReady(2));
    }

    @Test
    @DisplayName("should execute gameRequireStart without exception")
    void shouldExecuteGameRequireStartWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.gameRequireStart(1));
    }

    @Test
    @DisplayName("should execute gameStartPlaying without exception")
    void shouldExecuteGameStartPlayingWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(operations::gameStartPlaying);
    }

    @Test
    @DisplayName("should execute gameAbort without exception")
    void shouldExecuteGameAbortWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.gameAbort(1));
    }

    @Test
    @DisplayName("should execute receiveTouchStream without exception")
    void shouldExecuteReceiveTouchStreamWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);
        List<TouchFrame> frames = List.of(mock(TouchFrame.class));

        assertThatNoException().isThrownBy(() -> operations.receiveTouchStream(2, frames));
    }

    @Test
    @DisplayName("should execute receiveJudgeStream without exception")
    void shouldExecuteReceiveJudgeStreamWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);
        List<JudgeEvent> events = List.of(mock(JudgeEvent.class));

        assertThatNoException().isThrownBy(() -> operations.receiveJudgeStream(2, events));
    }

    @Test
    @DisplayName("should execute receiveChat without exception")
    void shouldExecuteReceiveChatWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.receiveChat(2, "Hello"));
    }

    @Test
    @DisplayName("should execute gameEnd without exception")
    void shouldExecuteGameEndWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(operations::gameEnd);
    }

    @Test
    @DisplayName("should execute gamePlayed without exception")
    void shouldExecuteGamePlayedWithoutException() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThatNoException().isThrownBy(() -> operations.gamePlayed(1, 1000000, 99.5f, true));
    }

    @Test
    @DisplayName("should create operations instance successfully")
    void shouldCreateOperationsInstanceSuccessfully() {
        LocalPlayerOperations operations = new LocalPlayerOperations(connection);

        assertThat(operations).isNotNull();
    }
}
