package top.rymc.phira.main.game;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private static final Map<Integer, Player> PLAYERS = new ConcurrentHashMap<>();

    public static Player resumeOrCreate(UserInfo userInfo, PlayerConnection newConn) {
        return PLAYERS.compute(userInfo.getId(), (id, existing) -> {
            if (existing != null && SessionManager.resume(existing, newConn)) {
                return existing;
            }
            Player newPlayer = Player.create(userInfo, newConn, key -> PLAYERS.remove(id));
            newPlayer.getConnection().setPacketHandler(PlayHandler.create(newPlayer));
            return newPlayer;
        });
    }

    public static Optional<Player> getPlayer(PlayerConnection connection) {
        return PLAYERS.values()
                .stream()
                .filter(player -> player.getConnection() == connection)
                .findFirst();
    }

    public static Optional<Player> getPlayer(int playerId) {
        return Optional.ofNullable(PLAYERS.get(playerId));
    }

    public static boolean isOnline(int playerId) {
        Player p = PLAYERS.get(playerId);
        return p != null && p.isOnline();
    }

    public static List<Player> getOnlinePlayers() {
        return PLAYERS.values()
                .stream()
                .filter(Player::isOnline)
                .toList();
    }


    public static List<Player> getAllPlayers() {
        return new ArrayList<>(PLAYERS.values());

    }
}