package top.rymc.phira.main.game.room.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.Playing;
import top.rymc.phira.test.MockPhiraServer;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class RoomPlayingStateTest {

    private LocalRoom room;
    private LocalRoom.PlayerManager playerManager;
    private LocalRoom.RoomSetting roomSetting;
    private Player player;
    private Player secondPlayer;
    private Player monitor;
    private PlayerOperations playerOperations;
    private PlayerOperations secondPlayerOperations;
    private PlayerOperations monitorOperations;
    private ChartInfo chartInfo;

    private Consumer<RoomGameState> stateUpdater;

    private RoomPlaying roomPlaying;
    private MockedStatic<Server> mockedServer;
    private MockPhiraServer mockPhiraServer;
    private String originalHost;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws IOException {
        room = mock(LocalRoom.class);
        playerManager = mock(LocalRoom.PlayerManager.class);
        roomSetting = mock(LocalRoom.RoomSetting.class);
        player = mock(Player.class);
        secondPlayer = mock(Player.class);
        monitor = mock(Player.class);
        playerOperations = mock(PlayerOperations.class);
        secondPlayerOperations = mock(PlayerOperations.class);
        monitorOperations = mock(PlayerOperations.class);
        chartInfo = mock(ChartInfo.class);
        stateUpdater = mock(Consumer.class);

        lenient().when(room.getPlayerManager()).thenReturn(playerManager);
        lenient().when(room.getSetting()).thenReturn(roomSetting);
        lenient().when(roomSetting.isCycle()).thenReturn(false);
        lenient().when(player.getId()).thenReturn(1);
        lenient().when(player.getName()).thenReturn("Player1");
        lenient().when(secondPlayer.getId()).thenReturn(2);
        lenient().when(secondPlayer.getName()).thenReturn("Player2");
        lenient().when(monitor.getId()).thenReturn(3);
        lenient().when(monitor.getName()).thenReturn("Monitor");
        lenient().when(player.isOnline()).thenReturn(true);
        lenient().when(secondPlayer.isOnline()).thenReturn(true);
        lenient().when(monitor.isOnline()).thenReturn(true);
        lenient().when(chartInfo.getId()).thenReturn(100);
        lenient().when(chartInfo.getName()).thenReturn("TestChart");

        mockedServer = mockStatic(Server.class);

        mockPhiraServer = new MockPhiraServer();
        mockPhiraServer.start();
        mockPhiraServer.addRecord(1000, 1, 100, 1000000, 100.0f, true);
        mockPhiraServer.addRecord(1001, 2, 100, 950000, 98.5f, false);

        originalHost = "https://phira.5wyxi.com/";
        PhiraFetcher.setHost(mockPhiraServer.getBaseUrl());
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
        PhiraFetcher.getRecordCache().clear();
    }

    @Test
    @DisplayName("should store frames when touch send")
    void shouldStoreFramesWhenTouchSend() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        List<TouchFrame> frames = List.of(mock(TouchFrame.class));

        roomPlaying.touchSend(player, frames);

        roomPlaying.touchSend(player, List.of());
    }

    @Test
    @DisplayName("should not broadcast touch send to monitors")
    void shouldNotBroadcastTouchSendToMonitors() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Set.of(monitor));
        when(monitor.operations()).thenReturn(java.util.Optional.of(monitorOperations));
        List<TouchFrame> frames = List.of(mock(TouchFrame.class));

        roomPlaying.touchSend(player, frames);

        verify(playerManager, never()).broadcastToMonitors(any());
    }

    @Test
    @DisplayName("should store events when judge send")
    void shouldStoreEventsWhenJudgeSend() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        List<JudgeEvent> events = List.of(mock(JudgeEvent.class));

        roomPlaying.judgeSend(player, events);

        roomPlaying.judgeSend(player, List.of());
    }

    @Test
    @DisplayName("should not broadcast judge send to monitors")
    void shouldNotBroadcastJudgeSendToMonitors() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Set.of(monitor));
        when(monitor.operations()).thenReturn(java.util.Optional.of(monitorOperations));
        List<JudgeEvent> events = List.of(mock(JudgeEvent.class));

        roomPlaying.judgeSend(player, events);

        verify(playerManager, never()).broadcastToMonitors(any());
    }

    @Test
    @DisplayName("should broadcast game abort when abort")
    void shouldBroadcastGameAbortWhenAbort() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());
        when(playerManager.getPlayersCopy()).thenReturn(Set.of(player, secondPlayer));
        when(player.operations()).thenReturn(java.util.Optional.of(playerOperations));
        when(secondPlayer.isOnline()).thenReturn(true);

        roomPlaying.abort(player);

        ArgumentCaptor<Consumer<PlayerOperations>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(playerManager).broadcast(captor.capture());
        captor.getValue().accept(playerOperations);
        verify(playerOperations).gameAbort(player.getId());
    }

    @Test
    @DisplayName("should transition to select chart when all done after abort")
    void shouldTransitionToSelectChartWhenAllDoneAfterAbort() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player));
        when(playerManager.getMonitors()).thenReturn(Set.of());
        when(playerManager.getPlayersCopy()).thenReturn(Set.of(player));
        when(player.operations()).thenReturn(java.util.Optional.of(playerOperations));
        when(player.isOnline()).thenReturn(true);

        roomPlaying.abort(player);

        ArgumentCaptor<RoomGameState> stateCaptor = ArgumentCaptor.forClass(RoomGameState.class);
        verify(stateUpdater).accept(stateCaptor.capture());
        assertThat(stateCaptor.getValue()).isInstanceOf(RoomSelectChart.class);
    }

    @Test
    @DisplayName("should fetch record and create phira record when played")
    void shouldFetchRecordAndCreatePhiraRecordWhenPlayed() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());
        when(playerManager.getPlayersCopy()).thenReturn(Set.of(player, secondPlayer));
        when(player.operations()).thenReturn(java.util.Optional.of(playerOperations));
        when(secondPlayer.isOnline()).thenReturn(true);

        roomPlaying.played(player, 1000);

        ArgumentCaptor<Consumer<PlayerOperations>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(playerManager).broadcast(captor.capture());
        captor.getValue().accept(playerOperations);
        verify(playerOperations).gamePlayed(1, 1000000, 100.0f, true);
    }

    @Test
    @DisplayName("played broadcasts game played")
    void playedBroadcastsGamePlayed() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());
        when(playerManager.getPlayersCopy()).thenReturn(Set.of(player, secondPlayer));
        when(player.operations()).thenReturn(java.util.Optional.of(playerOperations));
        when(secondPlayer.isOnline()).thenReturn(true);

        roomPlaying.played(player, 1000);

        ArgumentCaptor<Consumer<PlayerOperations>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(playerManager).broadcast(captor.capture());
        captor.getValue().accept(playerOperations);
        verify(playerOperations).gamePlayed(1, 1000000, 100.0f, true);
    }

    @Test
    @DisplayName("all players done transitions to select chart")
    void allPlayersDoneTransitionsToSelectChart() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);
        when(playerManager.getPlayers()).thenReturn(Set.of(player, secondPlayer));
        when(playerManager.getMonitors()).thenReturn(Set.of());
        when(playerManager.getPlayersCopy()).thenReturn(Set.of(player, secondPlayer));
        when(player.operations()).thenReturn(java.util.Optional.of(playerOperations));
        when(secondPlayer.operations()).thenReturn(java.util.Optional.of(secondPlayerOperations));
        when(player.isOnline()).thenReturn(true);
        when(secondPlayer.isOnline()).thenReturn(true);

        roomPlaying.played(player, 1000);
        roomPlaying.played(secondPlayer, 1001);

        ArgumentCaptor<RoomGameState> stateCaptor = ArgumentCaptor.forClass(RoomGameState.class);
        verify(stateUpdater).accept(stateCaptor.capture());
        assertThat(stateCaptor.getValue()).isInstanceOf(RoomSelectChart.class);
    }

    @Test
    @DisplayName("require start throws invalid state")
    void requireStartThrowsInvalidState() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);

        assertThatThrownBy(() -> roomPlaying.requireStart(player))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("ready throws invalid state")
    void readyThrowsInvalidState() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);

        assertThatThrownBy(() -> roomPlaying.ready(player))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("cancel ready throws invalid state")
    void cancelReadyThrowsInvalidState() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);

        assertThatThrownBy(() -> roomPlaying.cancelReady(player))
                .isInstanceOf(GameOperationException.class);
    }

    @Test
    @DisplayName("should return playing state when to protocol")
    void shouldReturnPlayingStateWhenToProtocol() {
        roomPlaying = new RoomPlaying(room, stateUpdater, chartInfo);

        GameState result = roomPlaying.toProtocol();

        assertThat(result).isInstanceOf(Playing.class);
    }
}
