package top.rymc.phira.main.game;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;
import top.rymc.phira.main.redis.RedisHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private static final Map<Integer, Player> PLAYERS = new ConcurrentHashMap<>();

    public static Player resumeOrCreate(UserInfo userInfo, PlayerConnection newConn) {
        Player existing = PLAYERS.get(userInfo.getId());

        if (existing != null && SessionManager.resume(existing, newConn)) {
            return existing;
        }

        Player player = Player.create(userInfo, newConn, p -> {
            PLAYERS.remove(p.getId());
            RedisHolder.get().removePlayerSession(p.getId());
        });
        PLAYERS.put(userInfo.getId(), player);
        player.getConnection().setPacketHandler(PlayHandler.create(player));
        return player;
    }

    public static Player getPlayer(int playerId) {
        return PLAYERS.get(playerId);
    }

    public static boolean isOnline(int playerId) {
        Player p = PLAYERS.get(playerId);
        return p != null && p.isOnline();
    }
}