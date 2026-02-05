package top.rymc.phira.main.redis;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import top.rymc.phira.main.util.GsonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis 连接与读写封装，对应 redis-data-schema：
 * - 玩家会话 mp:player:{uid}:session (Hash)
 * - 房间信息 mp:room:{rid}:info (Hash)
 * - 房间成员 mp:room:{rid}:players (Set)
 * - 事件频道 mp:events (Pub/Sub)
 */
@Getter
public class RedisManager {
    private static final Logger logger = LogManager.getLogger(RedisManager.class);
    private static final Gson GSON = GsonUtil.getGson();

    private final RedisConfig config;
    private final JedisPool pool;
    private final String joinRoomScriptSha;
    private volatile boolean closed;

    public RedisManager(RedisConfig config) {
        this.config = config;
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(32);
        poolConfig.setMinIdle(2);
        this.pool = new JedisPool(poolConfig, config.getHost(), config.getPort(), 2000, null, config.getDatabase());
        this.joinRoomScriptSha = loadJoinRoomScript();
        this.closed = false;
    }

    // ---------- 玩家会话 (Hash: server_id, room_id, name, is_monitor, last_seen) ----------

    public void setPlayerSession(int uid, String roomId, String name, boolean isMonitor) {
        String key = RedisKeys.playerSession(uid);
        Map<String, String> map = new HashMap<>();
        map.put("server_id", config.getServerId());
        map.put("room_id", roomId == null || roomId.isEmpty() ? "" : roomId);
        map.put("name", name == null ? "" : name);
        map.put("is_monitor", String.valueOf(isMonitor));
        map.put("last_seen", String.valueOf(System.currentTimeMillis()));
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, map);
        }
    }

    public void updatePlayerSessionRoom(int uid, String roomId) {
        String key = RedisKeys.playerSession(uid);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, "room_id", roomId == null || roomId.isEmpty() ? "" : roomId);
        }
    }

    public void updatePlayerSessionMonitor(int uid, boolean isMonitor) {
        String key = RedisKeys.playerSession(uid);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, "is_monitor", String.valueOf(isMonitor));
        }
    }

    public void updateLastSeen(int uid) {
        String key = RedisKeys.playerSession(uid);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, "last_seen", String.valueOf(System.currentTimeMillis()));
        }
    }

    public void removePlayerSession(int uid) {
        String key = RedisKeys.playerSession(uid);
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        }
    }

    /** 获取当前连接在本服务器的玩家 UID 列表（用于关闭时清理） */
    public Set<String> findPlayerIdsByServer() {
        try (Jedis jedis = pool.getResource()) {
            Set<String> keys = jedis.keys("mp:player:*:session");
            String serverId = config.getServerId();
            Set<String> uids = new java.util.HashSet<>();
            for (String key : keys) {
                String sid = jedis.hget(key, "server_id");
                if (serverId.equals(sid)) {
                    String uid = key.replace(RedisKeys.PREFIX + "player:", "").replace(":session", "");
                    uids.add(uid);
                }
            }
            return uids;
        }
    }

    // ---------- 房间信息 (Hash: host_id, state, chart_id, is_locked, is_cycle) ----------
    // state: 0=SelectChart, 1=WaitingForReady, 2=Playing

    public void setRoomInfo(String roomId, int hostId, int state, int chartId, boolean isLocked, boolean isCycle) {
        String key = RedisKeys.roomInfo(roomId);
        Map<String, String> map = new HashMap<>();
        map.put("host_id", String.valueOf(hostId));
        map.put("state", String.valueOf(state));
        map.put("chart_id", String.valueOf(chartId));
        map.put("is_locked", String.valueOf(isLocked));
        map.put("is_cycle", String.valueOf(isCycle));
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, map);
        }
    }

    public void updateRoomState(String roomId, int state, int chartId) {
        String key = RedisKeys.roomInfo(roomId);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, "state", String.valueOf(state));
            jedis.hset(key, "chart_id", String.valueOf(chartId));
        }
    }

    public void updateRoomHost(String roomId, int hostId) {
        String key = RedisKeys.roomInfo(roomId);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, "host_id", String.valueOf(hostId));
        }
    }

    public void updateRoomSettings(String roomId, boolean isLocked, boolean isCycle) {
        String key = RedisKeys.roomInfo(roomId);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, "is_locked", String.valueOf(isLocked));
            jedis.hset(key, "is_cycle", String.valueOf(isCycle));
        }
    }

    public void removeRoomInfo(String roomId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(RedisKeys.roomInfo(roomId));
        }
    }

    /** 读取房间信息，若不存在返回 null。 */
    public RoomInfoDto getRoomInfo(String roomId) {
        String key = RedisKeys.roomInfo(roomId);
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> map = jedis.hgetAll(key);
            if (map == null || map.isEmpty()) return null;
            int hostId = Integer.parseInt(map.getOrDefault("host_id", "0"));
            int state = Integer.parseInt(map.getOrDefault("state", "0"));
            int chartId = Integer.parseInt(map.getOrDefault("chart_id", "0"));
            boolean isLocked = Boolean.parseBoolean(map.getOrDefault("is_locked", "false"));
            boolean isCycle = Boolean.parseBoolean(map.getOrDefault("is_cycle", "false"));
            return new RoomInfoDto(hostId, state, chartId, isLocked, isCycle);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class RoomInfoDto {
        private final int hostId;
        private final int state;
        private final int chartId;
        private final boolean isLocked;
        private final boolean isCycle;
    }

    // ---------- 房间成员 (Set of uid) ----------

    private static final String JOIN_ROOM_LUA =
            "local current_count = redis.call('SCARD', KEYS[1])\n"
            + "if current_count < tonumber(ARGV[2]) then\n"
            + "    redis.call('SADD', KEYS[1], ARGV[1])\n"
            + "    return 1\n"
            + "else\n"
            + "    return 0\n"
            + "end";

    private String loadJoinRoomScript() {
        try (Jedis jedis = pool.getResource()) {
            return jedis.scriptLoad(JOIN_ROOM_LUA);
        }
    }

    /** 原子加入房间。返回 true 表示成功，false 表示房间已满。 */
    public boolean tryAddRoomPlayer(String roomId, int uid, int maxPlayers) {
        String key = RedisKeys.roomPlayers(roomId);
        try (Jedis jedis = pool.getResource()) {
            Object result = jedis.evalsha(joinRoomScriptSha, 1, key, String.valueOf(uid), String.valueOf(maxPlayers));
            return Long.valueOf(1).equals(result);
        }
    }

    public void addRoomPlayer(String roomId, int uid) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(RedisKeys.roomPlayers(roomId), String.valueOf(uid));
        }
    }

    public void removeRoomPlayer(String roomId, int uid) {
        try (Jedis jedis = pool.getResource()) {
            jedis.srem(RedisKeys.roomPlayers(roomId), String.valueOf(uid));
        }
    }

    public Set<String> getRoomPlayerIds(String roomId) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.smembers(RedisKeys.roomPlayers(roomId));
        }
    }

    public void removeRoomPlayers(String roomId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(RedisKeys.roomPlayers(roomId));
        }
    }

    // ---------- Pub/Sub ----------

    public void publish(PubSubEvent event) {
        String channel = RedisKeys.eventsChannel();
        String message = GSON.toJson(event);
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, message);
        }
    }

    public void publishRoomCreate(String roomId, int uid, String name) {
        publish(new PubSubEvent("ROOM_CREATE", roomId, Map.of("uid", uid, "name", name != null ? name : "")));
    }

    public void publishRoomDelete(String roomId, int uid, String name) {
        publish(new PubSubEvent("ROOM_DELETE", roomId, Map.of("uid", uid, "name", name != null ? name : "")));
    }

    public void publishPlayerJoin(String roomId, int uid, String name, boolean isMonitor) {
        publish(new PubSubEvent("PLAYER_JOIN", roomId, Map.of("uid", uid, "name", name != null ? name : "", "is_monitor", isMonitor)));
    }

    public void publishPlayerLeave(String roomId, int uid, boolean isHostChanged, int newHost) {
        publish(new PubSubEvent("PLAYER_LEAVE", roomId, Map.of("uid", uid, "is_host_changed", isHostChanged, "new_host", newHost)));
    }

    public void publishStateChange(String roomId, int newState, int chartId) {
        publish(new PubSubEvent("STATE_CHANGE", roomId, Map.of("new_state", newState, "chart_id", chartId)));
    }

    public void publishSyncScore(String roomId, int uid, String recordId) {
        publish(new PubSubEvent("SYNC_SCORE", roomId, Map.of("uid", uid, "record_id", recordId)));
    }

    /** 在后台线程订阅 mp:events，收到消息时回调 onMessage。 */
    public void subscribe(Consumer<PubSubEvent> onMessage) {
        Thread t = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (closed) return;
                        try {
                            PubSubEvent event = GSON.fromJson(message, PubSubEvent.class);
                            if (event != null && event.getEvent() != null) {
                                onMessage.accept(event);
                            }
                        } catch (Exception e) {
                            logger.warn("Parse mp:events message failed: {}", message, e);
                        }
                    }
                }, RedisKeys.eventsChannel());
            } catch (Exception e) {
                if (!closed) {
                    logger.error("Redis subscribe failed", e);
                }
            }
        }, "redis-subscriber");
        t.setDaemon(true);
        t.start();
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }

    // ---------- 关闭与清理 ----------

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        closed = true;
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}
