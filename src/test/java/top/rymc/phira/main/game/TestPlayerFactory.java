package top.rymc.phira.main.game;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestPlayerFactory {

    private TestPlayerFactory() {}

    public static Player createPlayer(int id, String name) {
        var userInfo = createUserInfo(id, name);
        var connection = mock(PlayerConnection.class);
        when(connection.isClosed()).thenReturn(false);
        return Player.create(userInfo, connection, p -> {});
    }

    public static Player createOfflinePlayer(int id, String name) {
        var userInfo = createUserInfo(id, name);
        var connection = mock(PlayerConnection.class);
        when(connection.isClosed()).thenReturn(true);
        return Player.create(userInfo, connection, p -> {});
    }

    private static UserInfo createUserInfo(int id, String name) {
        var userInfo = new UserInfo();
        ReflectionUtil.setField(userInfo, "id", id);
        ReflectionUtil.setField(userInfo, "name", name);
        return userInfo;
    }
}
