package top.rymc.phira.main.game.player.local;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.LocalPlayerOperations;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.holder.RoomHolder;
import top.rymc.phira.main.network.ConnectionReference;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

import java.util.Optional;

@RequiredArgsConstructor
public class LocalPlayer implements Player {
    @Getter private final UserInfo userInfo;
    @Getter private final ConnectionReference connectionRef;

    public PlayerConnection getConnection() {
        return connectionRef.get();
    }

    @Override
    public Optional<Room> getRoom() {
        ServerBoundPacketHandler handler = getConnection().getPacketHandler();
        return (handler instanceof RoomHolder roomHolder) ? Optional.of(roomHolder.getRoom()) : Optional.empty();
    }

    @Override
    public void kick() {
        getRoom().ifPresent(room -> room.leave(this));
        getConnection().markAsKicked();
    }

    @Override
    public Optional<PlayerOperations> operations() {
        PlayerConnection conn = getConnection();
        if (conn.isClosed()) return Optional.empty();
        return Optional.of(new LocalPlayerOperations(conn));
    }

    @Override
    public boolean isOnline() {
        return !getConnection().isClosed();
    }


}