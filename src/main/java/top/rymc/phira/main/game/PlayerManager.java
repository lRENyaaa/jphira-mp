package top.rymc.phira.main.game;

import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.PlayerConnection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private static final Map<Integer, Player> PLAYER_MAP = new ConcurrentHashMap<>();

    public static Player bindOrCreate(UserInfo userInfo, PlayerConnection connection) {
        Player bind = PLAYER_MAP.get(userInfo.getId());
        if (bind != null) {
            if (bind.isOnline()) {
                throw new RuntimeException("Duplicate join"); // TODO throw right exception
            }
            bind.bind(connection);
            return bind;
        }

        return Player.create(userInfo, connection, (p) -> PLAYER_MAP.remove(p.getUserInfo().getId()));
    }

    public static List<Player> getOnlinePlayers() {
        return PLAYER_MAP
                .values()
                .stream()
                .filter(Player::isOnline)
                .toList();
    }

    public static Player getOnlinePlayer(int playerId) {
        Player player = PLAYER_MAP.get(playerId);
        return player != null && player.isOnline() ? player : null;
    }

}