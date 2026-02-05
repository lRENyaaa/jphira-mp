package top.rymc.phira.main.redis;

/**
 * 与 redis-data-schema 一致的 Key 前缀与命名。所有 Key 使用 mp: 前缀。
 */
public final class RedisKeys {
    public static final String PREFIX = "mp:";

    public static String playerSession(int uid) {
        return PREFIX + "player:" + uid + ":session";
    }

    public static String roomInfo(String roomId) {
        return PREFIX + "room:" + roomId + ":info";
    }

    public static String roomPlayers(String roomId) {
        return PREFIX + "room:" + roomId + ":players";
    }

    public static String eventsChannel() {
        return PREFIX + "events";
    }

    private RedisKeys() {}
}
