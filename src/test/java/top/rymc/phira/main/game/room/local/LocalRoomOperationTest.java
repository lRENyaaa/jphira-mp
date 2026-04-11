package top.rymc.phira.main.game.room.local;

import org.junit.jupiter.api.AfterEach;
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
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.test.MockPhiraServer;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        MockitoAnnotations.openMocks(this);
        when(hostPlayer.getId()).thenReturn(1);
        when(hostPlayer.operations()).thenReturn(Optional.of(hostOperations));
        when(nonHostPlayer.getId()).thenReturn(2);
        when(nonHostPlayer.operations()).thenReturn(Optional.of(nonHostOperations));

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
    @DisplayName("isHost returns false for non-host player")
    void isHostReturnsFalseForNonHostPlayer() {
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
    @DisplayName("lockRoom by non-host throws GameOperationException")
    void lockRoomByNonHostThrowsGameOperationException() {
        localRoom.join(nonHostPlayer, false);

        assertThatThrownBy(() -> operation.lockRoom(nonHostPlayer))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.permission_denied");
    }

    @Test
    @DisplayName("cycleRoom by host toggles cycle setting")
    void cycleRoomByHostTogglesCycleSetting() {
        boolean initialCycle = localRoom.getSetting().isCycle();

        operation.cycleRoom(hostPlayer);

        assertThat(localRoom.getSetting().isCycle()).isEqualTo(!initialCycle);
        verify(hostOperations).cycleRoom(!initialCycle);
    }

    @Test
    @DisplayName("cycleRoom by non-host throws GameOperationException")
    void cycleRoomByNonHostThrowsGameOperationException() {
        localRoom.join(nonHostPlayer, false);

        assertThatThrownBy(() -> operation.cycleRoom(nonHostPlayer))
                .isInstanceOf(GameOperationException.class)
                .hasMessageContaining("error.permission_denied");
    }

    @Test
    @DisplayName("selectChart in SelectChart state by host fetches and broadcasts")
    void selectChartInSelectChartStateByHostFetchesAndBroadcasts() {
        Player anotherPlayer = mock(Player.class);
        when(anotherPlayer.getId()).thenReturn(3);
        when(anotherPlayer.operations()).thenReturn(Optional.of(anotherOperations));
        localRoom.join(anotherPlayer, false);

        operation.selectChart(hostPlayer, 100);

        verify(anotherOperations).selectChart(100, "TestChart", 1);
    }

    @Test
    @DisplayName("selectChart in wrong state throws GameOperationException")
    void selectChartInWrongStateThrowsGameOperationException() {
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
    @DisplayName("chat when enabled broadcasts to all")
    void chatWhenEnabledBroadcastsToAll() {
        Player anotherPlayer = mock(Player.class);
        when(anotherPlayer.getId()).thenReturn(3);
        when(anotherPlayer.operations()).thenReturn(Optional.of(anotherOperations));
        localRoom.join(anotherPlayer, false);

        String message = "Hello everyone";

        operation.chat(hostPlayer, message);

        verify(anotherOperations).receiveChat(1, message);
    }

    @Test
    @DisplayName("chat when disabled throws GameOperationException")
    void chatWhenDisabledThrowsGameOperationException() {
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
