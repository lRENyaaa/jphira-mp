package top.rymc.phira.main.game;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private static final Map<Integer, Player> PLAYERS = new ConcurrentHashMap<>();

    public static Player resumeOrCreate(UserInfo userInfo, PlayerConnection newConn) {
        Player existing = PLAYERS.get(userInfo.getId());

        if (existing != null && SessionManager.resume(existing, newConn)) {
            return existing;
        }

        // 新玩家
        Player player = Player.create(userInfo, newConn, key -> PLAYERS.remove(userInfo.getId()));
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