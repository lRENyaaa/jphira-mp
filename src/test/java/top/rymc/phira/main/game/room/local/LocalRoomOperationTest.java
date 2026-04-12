package top.rymc.phira.main.game.room.local;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.test.MockPhiraServer;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LocalRoomOperationTest {

    @Mock
    private Player hostPlayer;

    @Mock
    private Player nonHostPlayer;

    @Mock
    private PlayerOperations hostOperations;

    @Mock
    private PlayerOperations nonHostOperations;

    @Mock
    private PlayerOperations anotherOperations;

    private LocalRoom localRoom;
    private LocalRoom.LocalOperation operation;
    private MockedStatic<Server> mockedServer;
    private MockPhiraServer mockPhiraServer;
    private String originalHost;

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(hostPlayer.getId()).thenReturn(1);
        lenient().when(hostPlayer.operations()).thenReturn(Optional.of(hostOperations));
        lenient().when(nonHostPlayer.getId()).thenReturn(2);
        lenient().when(nonHostPlayer.operations()).thenReturn(Optional.of(nonHostOperations));

        mockedServer = mockStatic(Server.class);

        mockPhiraServer = new MockPhiraServer();
        mockPhiraServer.start();
        mockPhiraServer.addChart(100, "TestChart", "TestCharter", "5");

        originalHost = "https://phira.5wyxi.com/";
        PhiraFetcher.setHost(mockPhiraServer.getBaseUrl());

        LocalRoom.RoomSetting setting = new LocalRoom.RoomSetting(
                false,
                true,
                4,
                false,
                false,
                false,
                true
        );

        localRoom = new LocalRoom(
                () -> {},
                "test-room",
                setting,
                RoomGameState.Type.SelectChart,
                null
        );

        localRoom.join(hostPlayer, false);
        operation = localRoom.getOperation();
    }

    @AfterEach
    void tearDown() {
        if (mockedServer != null) {
            mockedServer.close();
        }
        if (mockPhiraServer != null) {
            mockPhiraServer.stop();
        }
        PhiraFetcher.setHost(originalHost);
        PhiraFetcher.getChartCache().invalidateAll(java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("isHost returns true for host player")
    void isHostReturnsTrueForHostPlayer() {
        boolean result = localRoom.isHost(hostPlayer);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when checking if non-host player is host")
    void shouldReturnFalseWhenCheckingIfNonHostPlayerIsHost() {
        localRoom.join(nonHostPlayer, false);

        boolean result = localRoom.isHost(nonHostPlayer);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("lockRoom by host toggles locked setting")
    void lockRoomByHostTogglesLockedSetting() {
        boolean initialLocked = localRoom.getSetting().isLocked();

        operation.lockRoom(hostPlayer);

        assertThat(localRoom.getSetting().isLocked()).isEqualTo(!initialLocked);
        verify(hostOperations).lockRoom(!initialLocked);
    }

    @Test
    @DisplayName("should throw GameOperationException when non-host tries to lock room")
    void shouldThrowGameOperationExceptionWhenNonHostTriesToLockRoom() {
        localRoom.join(nonHostPlayer, false);

        assertThatThrownBy(() -> operation.lockRoom(nonHostPlayer))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.permission_denied");
    }

    @Test
    @DisplayName("should toggle cycle setting when host cycles room")
    void shouldToggleCycleSettingWhenHostCyclesRoom() {
        boolean initialCycle = localRoom.getSetting().isCycle();

        operation.cycleRoom(hostPlayer);

        assertThat(localRoom.getSetting().isCycle()).isEqualTo(!initialCycle);
        verify(hostOperations).cycleRoom(!initialCycle);
    }

    @Test
    @DisplayName("should throw GameOperationException when non-host tries to cycle room")
    void shouldThrowGameOperationExceptionWhenNonHostTriesToCycleRoom() {
        localRoom.join(nonHostPlayer, false);

        assertThatThrownBy(() -> operation.cycleRoom(nonHostPlayer))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.permission_denied");
    }

    @Test
    @DisplayName("should fetch and broadcast chart when host selects chart in SelectChart state")
    void shouldFetchAndBroadcastChartWhenHostSelectsChartInSelectChartState() {
        Player anotherPlayer = mock(Player.class);
        when(anotherPlayer.getId()).thenReturn(3);
        when(anotherPlayer.operations()).thenReturn(Optional.of(anotherOperations));
        localRoom.join(anotherPlayer, false);

        operation.selectChart(hostPlayer, 100);

        verify(anotherOperations).selectChart(100, "TestChart", 1);
    }

    @Test
    @DisplayName("should throw GameOperationException when selecting chart in wrong state")
    void shouldThrowGameOperationExceptionWhenSelectingChartInWrongState() {
        LocalRoom.RoomSetting setting = new LocalRoom.RoomSetting(
                false,
                true,
                4,
                false,
                false,
                false,
                true
        );

        LocalRoom playingRoom = new LocalRoom(
                () -> {},
                "test-room-2",
                setting,
                RoomGameState.Type.Playing,
                null
        );

        playingRoom.join(hostPlayer, false);

        assertThatThrownBy(() -> playingRoom.getOperation().selectChart(hostPlayer, 100))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.invalid_state");
    }

    @Test
    @DisplayName("should broadcast chat to all when chat is enabled")
    void shouldBroadcastChatToAllWhenChatIsEnabled() {
        Player anotherPlayer = mock(Player.class);
        when(anotherPlayer.getId()).thenReturn(3);
        when(anotherPlayer.operations()).thenReturn(Optional.of(anotherOperations));
        localRoom.join(anotherPlayer, false);

        String message = "Hello everyone";

        operation.chat(hostPlayer, message);

        verify(anotherOperations).receiveChat(1, message);
    }

    @Test
    @DisplayName("should throw GameOperationException when chat is disabled")
    void shouldThrowGameOperationExceptionWhenChatIsDisabled() {
        LocalRoom.RoomSetting setting = new LocalRoom.RoomSetting(
                false,
                true,
                4,
                false,
                false,
                false,
                false
        );

        LocalRoom noChatRoom = new LocalRoom(
                () -> {},
                "test-room-3",
                setting,
                RoomGameState.Type.SelectChart,
                null
        );

        noChatRoom.join(hostPlayer, false);

        assertThatThrownBy(() -> noChatRoom.getOperation().chat(hostPlayer, "Hello"))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.chat_not_enabled");
    }
}
