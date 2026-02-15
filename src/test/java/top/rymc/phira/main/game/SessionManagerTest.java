package top.rymc.phira.main.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.test.TestServerSetup;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SessionManager")
class SessionManagerTest {

    private Room room;

    @BeforeAll
    static void initServer() throws Exception {
        TestServerSetup.init();
    }

    @BeforeEach
    void setUp() {
        java.util.Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
        room = RoomManager.createRoom("session-test-room", new Room.RoomSetting());
    }

    @AfterEach
    void tearDown() {
        PlayerManager.getAllPlayers().forEach(Player::kick);
        java.util.Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
    }

    @Test
    @DisplayName("should return false when resuming non-suspended player")
    void shouldReturnFalseWhenResumingNonSuspendedPlayer() {
        var player = TestPlayerFactory.createPlayer(1, "player");
        var newConn = mock(PlayerConnection.class);

        boolean resumed = SessionManager.resume(player, newConn);

        assertThat(resumed).isFalse();
    }

    @Test
    @DisplayName("should return false when suspending player not in room handler")
    void shouldReturnFalseWhenSuspendingPlayerNotInRoomHandler() {
        var player = TestPlayerFactory.createPlayer(1, "player");

        boolean suspended = SessionManager.suspend(player);

        assertThat(suspended).isFalse();
    }

    @Test
    @DisplayName("should allow setting and getting suspend timeout")
    void shouldAllowSettingAndGettingSuspendTimeout() {
        long originalTimeout = SessionManager.getSuspendTimeoutMillis();

        SessionManager.setSuspendTimeout(100, TimeUnit.MILLISECONDS);

        assertThat(SessionManager.getSuspendTimeoutMillis()).isEqualTo(100);

        SessionManager.setSuspendTimeout(originalTimeout, TimeUnit.MILLISECONDS);
    }
}
