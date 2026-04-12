package top.rymc.phira.main.game.session;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.exception.session.ResumeFailedException;
import top.rymc.phira.main.game.exception.session.SuspendFailedException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.network.ConnectionReference;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.test.TestHandler;
import top.rymc.phira.test.TestServerSetup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
class LocalSessionManagerTest {

    private static final int PLAYER_ID = 123;

    @Mock
    private UserInfo userInfo;

    @Mock
    private PlayerConnection playerConnection;

    @Mock
    private PlayerConnection newPlayerConnection;

    @BeforeAll
    static void setUpServer() throws Exception {
        TestServerSetup.init();
    }

    private TestHandler currentHandler;

    @BeforeEach
    void setUp() {
        LocalSessionManager.setSuspendTimeout(100, TimeUnit.MILLISECONDS);
        lenient().when(userInfo.getId()).thenReturn(PLAYER_ID);
        lenient().when(userInfo.getName()).thenReturn("TestPlayer");
        lenient().when(playerConnection.getPacketHandler()).thenAnswer(inv -> currentHandler);
    }

    private LocalPlayer createTestPlayer() {
        ConnectionReference connectionRef = new ConnectionReference(playerConnection);
        return new LocalPlayer(userInfo, connectionRef);
    }

    private LocalRoom createTestRoom() {
        LocalRoom.RoomSetting setting = new LocalRoom.RoomSetting(
            false, true, 4, false, false, false, true
        );
        return new LocalRoom(
            () -> {},
            "test-room",
            setting,
            RoomGameState.Type.SelectChart,
            null
        );
    }

    @Test
    @DisplayName("should create session with timeout when suspend")
    void shouldCreateSessionWithTimeoutWhenSuspend() {
        LocalPlayer player = createTestPlayer();
        LocalRoom room = createTestRoom();
        room.join(player, false);

        currentHandler = new TestHandler(player, room);
        player.getConnection().setPacketHandler(currentHandler);

        AtomicBoolean removerCalled = new AtomicBoolean(false);
        Runnable remover = () -> removerCalled.set(true);

        LocalSessionManager.suspend(player, remover);

        assertThat(removerCalled.get()).isFalse();
    }

    @Test
    @DisplayName("should throw SuspendFailedException when suspend without SuspendableRoomHolder")
    void shouldThrowSuspendFailedExceptionWhenSuspendWithoutSuspendableRoomHolder() {
        LocalPlayer player = createTestPlayer();

        assertThatThrownBy(() -> LocalSessionManager.suspend(player, () -> {}))
                .isInstanceOf(SuspendFailedException.class);
    }

    @Test
    @DisplayName("should throw SuspendFailedException when suspend when not in room")
    void shouldThrowSuspendFailedExceptionWhenSuspendWhenNotInRoom() {
        LocalPlayer player = createTestPlayer();
        LocalRoom room = createTestRoom();

        currentHandler = new TestHandler(player, room);
        player.getConnection().setPacketHandler(currentHandler);

        assertThatThrownBy(() -> LocalSessionManager.suspend(player, () -> {}))
                .isInstanceOf(SuspendFailedException.class);
    }

    @Test
    @DisplayName("should restore connection when resume")
    void shouldRestoreConnectionWhenResume() {
        LocalPlayer player = createTestPlayer();
        LocalRoom room = createTestRoom();
        room.join(player, false);

        currentHandler = new TestHandler(player, room);
        player.getConnection().setPacketHandler(currentHandler);

        LocalSessionManager.suspend(player, () -> {});
        LocalSessionManager.resume(player, newPlayerConnection);

        assertThat(player.getConnection()).isEqualTo(newPlayerConnection);
    }

    @Test
    @DisplayName("should cancel timeout when resume")
    void shouldCancelTimeoutWhenResume() {
        LocalPlayer player = createTestPlayer();
        LocalRoom room = createTestRoom();
        room.join(player, false);

        currentHandler = new TestHandler(player, room);
        player.getConnection().setPacketHandler(currentHandler);

        AtomicBoolean removerCalled = new AtomicBoolean(false);
        Runnable remover = () -> removerCalled.set(true);

        LocalSessionManager.suspend(player, remover);
        LocalSessionManager.resume(player, newPlayerConnection);

        await().atMost(200, TimeUnit.MILLISECONDS).pollDelay(150, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(removerCalled.get()).isFalse());
    }

    @Test
    @DisplayName("should throw ResumeFailedException when resume without suspended session")
    void shouldThrowResumeFailedExceptionWhenResumeWithoutSuspendedSession() {
        LocalPlayer player = createTestPlayer();

        assertThatThrownBy(() -> LocalSessionManager.resume(player, newPlayerConnection))
                .isInstanceOf(ResumeFailedException.class);
    }

    @Test
    @DisplayName("should throw ResumeFailedException when resume when player not in room")
    void shouldThrowResumeFailedExceptionWhenResumeWhenPlayerNotInRoom() {
        LocalPlayer player = createTestPlayer();
        LocalRoom room = createTestRoom();
        room.join(player, false);

        currentHandler = new TestHandler(player, room);
        player.getConnection().setPacketHandler(currentHandler);

        LocalSessionManager.suspend(player, () -> {});
        room.leave(player);

        assertThatThrownBy(() -> LocalSessionManager.resume(player, newPlayerConnection))
                .isInstanceOf(ResumeFailedException.class);
    }

    @Test
    @DisplayName("should force leave when session timeout")
    void shouldForceLeaveWhenSessionTimeout() {
        LocalPlayer player = createTestPlayer();
        LocalRoom room = createTestRoom();
        room.join(player, false);

        currentHandler = new TestHandler(player, room);
        player.getConnection().setPacketHandler(currentHandler);

        AtomicBoolean removerCalled = new AtomicBoolean(false);
        Runnable remover = () -> removerCalled.set(true);

        LocalSessionManager.suspend(player, remover);

        await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(removerCalled.get()).isTrue());

        assertThat(room.containsPlayer(player)).isFalse();
    }

    @Test
    @DisplayName("should cancel old timeout when duplicate suspend")
    void shouldCancelOldTimeoutWhenDuplicateSuspend() {
        LocalPlayer player = createTestPlayer();
        LocalRoom room = createTestRoom();
        room.join(player, false);

        currentHandler = new TestHandler(player, room);
        player.getConnection().setPacketHandler(currentHandler);

        AtomicBoolean firstRemoverCalled = new AtomicBoolean(false);
        AtomicBoolean secondRemoverCalled = new AtomicBoolean(false);

        Runnable firstRemoverRunnable = () -> firstRemoverCalled.set(true);
        Runnable secondRemoverRunnable = () -> secondRemoverCalled.set(true);

        LocalSessionManager.suspend(player, firstRemoverRunnable);
        LocalSessionManager.suspend(player, secondRemoverRunnable);

        await().atMost(500, TimeUnit.MILLISECONDS).pollDelay(150, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(firstRemoverCalled.get()).isFalse());

        await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(secondRemoverCalled.get()).isTrue());
    }
}
