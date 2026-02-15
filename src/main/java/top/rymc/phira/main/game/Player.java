package top.rymc.phira.main.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.main.network.handler.RoomHandler;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

import java.util.Optional;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Player implements ProtocolConvertible<UserProfile> {
    @Getter private final UserInfo userInfo;
    @Getter private volatile PlayerConnection connection;
    private final Consumer<Player> removeFromManager;

    public static Player create(UserInfo info, PlayerConnection conn, Consumer<Player> remover) {
        Player p = new Player(info, remover);
        p.bind(conn);
        return p;
    }

    public void bind(PlayerConnection newConn) {
        if (this.connection != null) {
            this.connection.sendChat("账号在其他地方登录");
            this.connection.close();
        }
        this.connection = newConn;

        newConn.onClose(ctx -> {
            if (!SessionManager.suspend(this)) {
                removeFromManager.accept(this);
            }
        });
    }

    public Optional<Room> getRoom() {
        PlayerConnection conn = this.connection;
        if (conn == null) return Optional.empty();
        ServerBoundPacketHandler h = conn.getPacketHandler();
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
        removeFromManager.accept(this);
        connection.close();
    }

    public int getId() { return userInfo.getId(); }
    public String getName() { return userInfo.getName(); }

    public String getLanguage() {
        String lang = userInfo.getLanguage();
        return lang != null ? lang : Server.getInstance().getArgs().getDefaultLanguage();
    }

    @Override
    public UserProfile toProtocol() {
        return new UserProfile(userInfo.getId(), userInfo.getName());
    }
}