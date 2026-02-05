package top.rymc.phira.main.game;

import lombok.Getter;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.main.network.handler.RoomHandler;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.handler.PacketHandler;

import java.util.Optional;
import java.util.function.Consumer;

public class Player implements ProtocolConvertible<UserProfile> {
    @Getter private final UserInfo userInfo;
    @Getter private PlayerConnection connection;
    private final Consumer<Player> removeFromManager;
    /** 来自 Redis 远端的占位玩家（无连接，仅 uid/name）。 */
    @Getter private final boolean remote;

    private Player(UserInfo info, PlayerConnection conn, Consumer<Player> remover, boolean remote) {
        this.userInfo = info;
        this.connection = conn;
        this.removeFromManager = remover != null ? remover : p -> {};
        this.remote = remote;
    }

    public static Player create(UserInfo info, PlayerConnection conn, Consumer<Player> remover) {
        Player p = new Player(info, null, remover, false);
        p.bind(conn);
        return p;
    }

    /** 创建 Redis 远端占位玩家（无连接，不加入 PlayerManager）。 */
    public static Player createRemote(int uid, String name) {
        return new Player(new UserInfo(uid, name), null, null, true);
    }

    public void bind(PlayerConnection newConn) {
        if (this.connection != null) {
            this.connection.sendChat("账号在其他地方登录");
            this.connection.close(); // 关闭旧连接
        }
        this.connection = newConn;

        // 断线时触发挂起，否则离开房间并从管理器移除
        newConn.onClose(ctx -> {
            if (!SessionManager.suspend(this)) {
                getRoom().ifPresent(r -> r.leave(this));
                removeFromManager.accept(this);
            }
        });
    }

    public Optional<Room> getRoom() {
        if (connection == null) return Optional.empty();
        PacketHandler h = connection.getPacketHandler();
        return (h instanceof RoomHandler rh) ? Optional.of(rh.getRoom()) : Optional.empty();
    }

    public Optional<RoomInfo> getRoomInfo() {
        return getRoom().map(r -> r.asProtocolConvertible(this).toProtocol());
    }

    public boolean isOnline() {
        return connection != null && !connection.isClosed();
    }

    public void kick() {
        getRoom().ifPresent(r -> r.leave(this));
        if (removeFromManager != null) removeFromManager.accept(this);
        if (connection != null) connection.close();
    }

    public int getId() { return userInfo.getId(); }
    public String getName() { return userInfo.getName(); }

    @Override
    public UserProfile toProtocol() {
        return new UserProfile(userInfo.getId(), userInfo.getName());
    }
}