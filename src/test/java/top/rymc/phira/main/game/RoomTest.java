package top.rymc.phira.main.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.test.TestServerSetup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Room")
class RoomTest {

    private Room room;

    @BeforeAll
    static void initServer() throws Exception {
        TestServerSetup.init();
    }

    @BeforeEach
    void setUp() {
        java.util.Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
        room = RoomManager.createRoom("test-room", new Room.RoomSetting());
    }

    @AfterEach
    void tearDown() {
        java.util.Map<String, Room> rooms = ReflectionUtil.getField(RoomManager.class, "ROOMS");
        rooms.clear();
    }

    @Test
    @DisplayName("should create room with default settings")
    void shouldCreateRoomWithDefaultSettings() {
        assertThat(room.getRoomId()).isEqualTo("test-room");
        assertThat(room.getPlayers()).isEmpty();
        assertThat(room.getMonitors()).isEmpty();
    }

    @Test
    @DisplayName("should allow player to join")
    void shouldAllowPlayerToJoin() {
        var player = TestPlayerFactory.createPlayer(1, "player1");

        room.join(player, false);

        assertThat(room.containsPlayer(player)).isTrue();
        assertThat(room.getPlayers()).contains(player);
    }

    @Test
    @DisplayName("should allow monitor to join")
    void shouldAllowMonitorToJoin() {
        var monitor = TestPlayerFactory.createPlayer(2, "monitor");

        room.join(monitor, true);

        assertThat(room.containsMonitor(monitor)).isTrue();
        assertThat(room.getMonitors()).contains(monitor);
        assertThat(room.containsPlayer(monitor)).isFalse();
    }

    @Test
    @DisplayName("should throw when room is full")
    void shouldThrowWhenRoomIsFull() {
        var setting = new Room.RoomSetting(true, true, 2, false, false, false, true);
        var fullRoom = RoomManager.createRoom("full-room", setting);

        fullRoom.join(TestPlayerFactory.createPlayer(1, "p1"), false);
        fullRoom.join(TestPlayerFactory.createPlayer(2, "p2"), false);

        assertThatThrownBy(() ->
            fullRoom.join(TestPlayerFactory.createPlayer(3, "p3"), false)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("full");
    }

    @Test
    @DisplayName("should throw when joining locked room")
    void shouldThrowWhenJoiningLockedRoom() {
        var setting = new Room.RoomSetting(true, true, 8, true, false, false, true);
        var lockedRoom = RoomManager.createRoom("locked-room", setting);
        lockedRoom.join(TestPlayerFactory.createPlayer(1, "host"), false);

        assertThatThrownBy(() ->
            lockedRoom.join(TestPlayerFactory.createPlayer(2, "player"), false)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("should allow player to leave")
    void shouldAllowPlayerToLeave() {
        var player = TestPlayerFactory.createPlayer(1, "player1");
        room.join(player, false);

        room.leave(player);

        assertThat(room.containsPlayer(player)).isFalse();
        assertThat(room.getPlayers()).doesNotContain(player);
    }

    @Test
    @DisplayName("should check if player is in room")
    void shouldCheckIfPlayerIsInRoom() {
        var player = TestPlayerFactory.createPlayer(1, "player1");
        var outsider = TestPlayerFactory.createPlayer(2, "outsider");
        room.join(player, false);

        assertThat(room.isInRoom(player)).isTrue();
        assertThat(room.isInRoom(outsider)).isFalse();
    }

    @Test
    @DisplayName("should identify host correctly")
    void shouldIdentifyHostCorrectly() {
        var host = TestPlayerFactory.createPlayer(1, "host");
        var regular = TestPlayerFactory.createPlayer(2, "regular");
        var hostRoom = RoomManager.createRoom("host-room", host);
        hostRoom.join(regular, false);

        assertThat(hostRoom.isHost(host)).isTrue();
        assertThat(hostRoom.isHost(regular)).isFalse();
    }

    @Test
    @DisplayName("should transfer host when host leaves")
    void shouldTransferHostWhenHostLeaves() {
        var host = TestPlayerFactory.createPlayer(1, "host");
        var newHost = TestPlayerFactory.createPlayer(2, "newHost");
        var hostRoom = RoomManager.createRoom("transfer-room", host);
        hostRoom.join(newHost, false);

        hostRoom.leave(host);

        assertThat(hostRoom.isHost(newHost)).isTrue();
    }

    @Test
    @DisplayName("should not transfer host when host system disabled")
    void shouldNotTransferHostWhenHostSystemDisabled() {
        var setting = new Room.RoomSetting(true, false, 8, false, false, false, true);
        var player1 = TestPlayerFactory.createPlayer(1, "p1");
        var player2 = TestPlayerFactory.createPlayer(2, "p2");
        var noHostRoom = RoomManager.createRoom("no-host-room", setting);
        noHostRoom.join(player1, false);
        noHostRoom.join(player2, false);

        noHostRoom.leave(player1);

        assertThat(noHostRoom.isHost(player2)).isFalse();
    }

    @Test
    @DisplayName("should allow host to lock room")
    void shouldAllowHostToLockRoom() {
        var host = TestPlayerFactory.createPlayer(1, "host");
        var hostRoom = RoomManager.createRoom("lock-test-room", host);

        hostRoom.getOperation().lockRoom(host);

        assertThat((Boolean) ReflectionUtil.getField(hostRoom.getSetting(), "locked")).isTrue();
    }

    @Test
    @DisplayName("should throw when non-host tries to lock room")
    void shouldThrowWhenNonHostTriesToLockRoom() {
        var host = TestPlayerFactory.createPlayer(1, "host");
        var regular = TestPlayerFactory.createPlayer(2, "regular");
        var hostRoom = RoomManager.createRoom("lock-perm-room", host);
        hostRoom.join(regular, false);

        assertThatThrownBy(() ->
            hostRoom.getOperation().lockRoom(regular)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("权限");
    }

    @Test
    @DisplayName("should allow host to cycle room")
    void shouldAllowHostToCycleRoom() {
        var host = TestPlayerFactory.createPlayer(1, "host");
        var hostRoom = RoomManager.createRoom("cycle-test-room", host);

        hostRoom.getOperation().cycleRoom(host);

        assertThat((Boolean) ReflectionUtil.getField(hostRoom.getSetting(), "cycle")).isTrue();
    }

    @Test
    @DisplayName("should throw when non-host tries to cycle room")
    void shouldThrowWhenNonHostTriesToCycleRoom() {
        var host = TestPlayerFactory.createPlayer(1, "host");
        var regular = TestPlayerFactory.createPlayer(2, "regular");
        var hostRoom = RoomManager.createRoom("cycle-perm-room", host);
        hostRoom.join(regular, false);

        assertThatThrownBy(() ->
            hostRoom.getOperation().cycleRoom(regular)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("权限");
    }

    @Test
    @DisplayName("should throw when chatting in room with chat disabled")
    void shouldThrowWhenChattingInRoomWithChatDisabled() {
        var setting = new Room.RoomSetting(true, true, 8, false, false, false, false);
        var noChatRoom = RoomManager.createRoom("no-chat-room", setting);
        var player = TestPlayerFactory.createPlayer(1, "player");
        noChatRoom.join(player, false);

        assertThatThrownBy(() ->
            noChatRoom.getOperation().chat(player, "hello")
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("聊天");
    }

    @Test
    @DisplayName("should return unmodifiable player set")
    void shouldReturnUnmodifiablePlayerSet() {
        var player = TestPlayerFactory.createPlayer(1, "player");
        room.join(player, false);

        var players = room.getPlayers();

        assertThatThrownBy(() -> players.add(TestPlayerFactory.createPlayer(2, "p2")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should return unmodifiable monitor set")
    void shouldReturnUnmodifiableMonitorSet() {
        var monitor = TestPlayerFactory.createPlayer(1, "monitor");
        room.join(monitor, true);

        var monitors = room.getMonitors();

        assertThatThrownBy(() -> monitors.add(TestPlayerFactory.createPlayer(2, "m2")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should destroy empty room via admin operation")
    void shouldDestroyEmptyRoomViaAdminOperation() {
        var emptyRoom = RoomManager.createRoom("empty-room", new Room.RoomSetting());

        boolean destroyed = emptyRoom.getAdminOperation().destroy();

        assertThat(destroyed).isTrue();
        assertThat(RoomManager.findRoom("empty-room")).isNull();
    }

    @Test
    @DisplayName("should not destroy room with players")
    void shouldNotDestroyRoomWithPlayers() {
        var player = TestPlayerFactory.createPlayer(1, "player");
        room.join(player, false);

        boolean destroyed = room.getAdminOperation().destroy();

        assertThat(destroyed).isFalse();
        assertThat(RoomManager.findRoom("test-room")).isEqualTo(room);
    }
}
