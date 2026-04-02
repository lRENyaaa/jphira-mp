package top.rymc.phira.main.game.player;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.game.session.SessionManager;
import top.rymc.phira.main.network.ConnectionReference;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;
import top.rymc.phira.protocol.data.RoomInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerManager {
    private static final Map<Integer, LocalPlayer> PLAYERS = new ConcurrentHashMap<>();

    private static void initOnClose(PlayerConnection connection, LocalPlayer player) {
        connection.onClose(ctx -> {
            if (!SessionManager.suspend(player)) {
                PLAYERS.remove(player.getId());
            }
        });
    }

    public static RoomInfo resumeOrCreate(UserInfo userInfo, PlayerConnection newConn) {
        AtomicReference<RoomInfo> reference = new AtomicReference<>();
        PLAYERS.compute(userInfo.getId(), (id, existing) -> {
            if (existing != null && SessionManager.resume(existing, newConn)) {
                initOnClose(newConn, existing);
                reference.set(existing.getRoomInfo().orElse(null));
                return existing;
            }

            LocalPlayer newPlayer = new LocalPlayer(userInfo, new ConnectionReference(newConn));
            initOnClose(newConn, newPlayer);
            newPlayer.getConnection().setPacketHandler(PlayHandler.create(newPlayer));
            return newPlayer;
        });
        return reference.get();
    }

    public static Optional<LocalPlayer> getPlayer(PlayerConnection connection) {
        return PLAYERS.values()
                .stream()
                .filter(player -> player.getConnection() == connection)
                .findFirst();
    }

    public static Optional<LocalPlayer> getPlayer(int playerId) {
        return Optional.ofNullable(PLAYERS.get(playerId));
    }

    public static boolean isOnline(int playerId) {
        LocalPlayer p = PLAYERS.get(playerId);
        return p != null && p.isNotSuspend();
    }

    public static List<LocalPlayer> getOnlinePlayers() {
        return PLAYERS.values()
                .stream()
                .filter(LocalPlayer::isNotSuspend)
                .toList();
    }


    public static List<LocalPlayer> getAllPlayers() {
        return new ArrayList<>(PLAYERS.values());

    }
}