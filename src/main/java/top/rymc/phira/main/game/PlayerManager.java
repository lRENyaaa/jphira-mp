package top.rymc.phira.main.game;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;

import java.util.Map;
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

    public static Player getPlayer(int playerId) {
        return PLAYERS.get(playerId);
    }

    public static boolean isOnline(int playerId) {
        Player p = PLAYERS.get(playerId);
        return p != null && p.isOnline();
    }
}