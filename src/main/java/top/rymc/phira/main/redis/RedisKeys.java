package top.rymc.phira.main.redis;

/**
 * Redis Key 常量，与 redis-data-schema.md 一致，前缀 mp:
 */
public final class RedisKeys {

    public static final String PREFIX = "mp:";

    /** 玩家会话 Hash: mp:player:{uid}:session */
    public static String playerSession(int uid) {
        return PREFIX + "player:" + uid + ":session";
    }

    /** 房间信息 Hash: mp:room:{rid}:info */
    public static String roomInfo(String roomId) {
        return PREFIX + "room:" + roomId + ":info";
    }

    /** 房间成员 Set: mp:room:{rid}:players */
    public static String roomPlayers(String roomId) {
        return PREFIX + "room:" + roomId + ":players";
    }

    /** 跨服事件频道 */
    public static final String EVENTS_CHANNEL = PREFIX + "events";

    private RedisKeys() {}
}
