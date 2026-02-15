package top.rymc.phira.main.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PlayerManager")
class PlayerManagerTest {

    @AfterEach
    void tearDown() {
        PlayerManager.getAllPlayers().forEach(Player::kick);
    }

    @Test
    @DisplayName("should create new player when not exists")
    void shouldCreateNewPlayerWhenNotExists() {
        var userInfo = createUserInfo(1, "player1");
        var connection = mock(PlayerConnection.class);

        Player player = PlayerManager.resumeOrCreate(userInfo, connection);

        assertThat(player).isNotNull();
        assertThat(player.getId()).isEqualTo(1);
        assertThat(player.getName()).isEqualTo("player1");
        assertThat(PlayerManager.getPlayer(1)).isPresent().hasValue(player);
    }

    @Test
    @DisplayName("should find player by connection")
    void shouldFindPlayerByConnection() {
        var userInfo = createUserInfo(2, "player2");
        var connection = mock(PlayerConnection.class);
        Player created = PlayerManager.resumeOrCreate(userInfo, connection);

        var found = PlayerManager.getPlayer(connection);

        assertThat(found).isPresent().hasValue(created);
    }

    @Test
    @DisplayName("should find player by id")
    void shouldFindPlayerById() {
        var userInfo = createUserInfo(3, "player3");
        var connection = mock(PlayerConnection.class);
        Player created = PlayerManager.resumeOrCreate(userInfo, connection);

        var found = PlayerManager.getPlayer(3);

        assertThat(found).isPresent().hasValue(created);
    }

    @Test
    @DisplayName("should return empty when player not found by id")
    void shouldReturnEmptyWhenPlayerNotFoundById() {
        var found = PlayerManager.getPlayer(999);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should return empty when player not found by connection")
    void shouldReturnEmptyWhenPlayerNotFoundByConnection() {
        var connection = mock(PlayerConnection.class);

        var found = PlayerManager.getPlayer(connection);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should check online status correctly")
    void shouldCheckOnlineStatusCorrectly() {
        var userInfo = createUserInfo(4, "player4");
        var connection = mock(PlayerConnection.class);
        when(connection.isClosed()).thenReturn(false);
        PlayerManager.resumeOrCreate(userInfo, connection);

        assertThat(PlayerManager.isOnline(4)).isTrue();
        assertThat(PlayerManager.isOnline(999)).isFalse();
    }

    @Test
    @DisplayName("should get online players only")
    void shouldGetOnlinePlayersOnly() {
        var onlineUser = createUserInfo(5, "online");
        var onlineConn = mock(PlayerConnection.class);
        when(onlineConn.isClosed()).thenReturn(false);
        Player onlinePlayer = PlayerManager.resumeOrCreate(onlineUser, onlineConn);

        var offlineUser = createUserInfo(6, "offline");
        var offlineConn = mock(PlayerConnection.class);
        when(offlineConn.isClosed()).thenReturn(true);
        Player offlinePlayer = PlayerManager.resumeOrCreate(offlineUser, offlineConn);

        var onlinePlayers = PlayerManager.getOnlinePlayers();

        assertThat(onlinePlayers).contains(onlinePlayer);
        assertThat(onlinePlayers).doesNotContain(offlinePlayer);
    }

    @Test
    @DisplayName("should get all players including offline")
    void shouldGetAllPlayersIncludingOffline() {
        var onlineUser = createUserInfo(7, "online");
        var onlineConn = mock(PlayerConnection.class);
        Player onlinePlayer = PlayerManager.resumeOrCreate(onlineUser, onlineConn);

        var offlineUser = createUserInfo(8, "offline");
        var offlineConn = mock(PlayerConnection.class);
        Player offlinePlayer = PlayerManager.resumeOrCreate(offlineUser, offlineConn);

        var allPlayers = PlayerManager.getAllPlayers();

        assertThat(allPlayers).contains(onlinePlayer, offlinePlayer);
    }

    @Test
    @DisplayName("should return defensive copy of players list")
    void shouldReturnDefensiveCopyOfPlayersList() {
        PlayerManager.resumeOrCreate(createUserInfo(9, "player9"), mock(PlayerConnection.class));

        var firstCall = PlayerManager.getAllPlayers();
        var secondCall = PlayerManager.getAllPlayers();

        assertThat(firstCall).isNotSameAs(secondCall);
    }

    private UserInfo createUserInfo(int id, String name) {
        var userInfo = new UserInfo();
        ReflectionUtil.setField(userInfo, "id", id);
        ReflectionUtil.setField(userInfo, "name", name);
        return userInfo;
    }
}
