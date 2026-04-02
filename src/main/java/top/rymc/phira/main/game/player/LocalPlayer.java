package top.rymc.phira.main.game.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.holder.RoomHolder;
import top.rymc.phira.main.network.ConnectionReference;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class LocalPlayer implements Player {
    @Getter private final UserInfo userInfo;
    @Getter private final ConnectionReference connectionRef;

    public PlayerConnection getConnection() {
        return connectionRef.get();
    }

    public Optional<Room> getRoom() {
        PlayerConnection conn = getConnection();
        if (conn == null) return Optional.empty();
        ServerBoundPacketHandler h = conn.getPacketHandler();
        return (h instanceof RoomHolder rh) ? Optional.of(rh.getRoom()) : Optional.empty();
    }

    public Optional<RoomInfo> getRoomInfo() {
        return getRoom().map(r -> r.asProtocolConvertible(this).toProtocol());
    }

    public boolean isOnline() {
        return getConnection() != null && !getConnection().isClosed();
    }

    public void kick() {
        getRoom().ifPresent(room -> room.leave(this));
        getConnection().markAsKicked();
    }

    @Override
    public UserProfile toProtocol() {
        return new UserProfile(userInfo.getId(), userInfo.getName());
    }
}