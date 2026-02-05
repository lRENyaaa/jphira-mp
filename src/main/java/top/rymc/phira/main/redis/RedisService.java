package top.rymc.phira.main.redis;

import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.util.GsonUtil;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Redis 业务封装：玩家会话、房间信息、房间成员、原子加入、Pub/Sub 事件。
 * 对应 redis-data-schema.md 规范。
 */
public class RedisService {

    private static final Logger LOG = LogManager.getLogger(RedisService.class);
    private static final Gson GSON = GsonUtil.getGson();

    /** 原子加入房间 Lua：KEYS[1]=room:players, ARGV[1]=uid, ARGV[2]=max_players */
    private static final String LUA_JOIN_ROOM =
            "local n = redis.call('SCARD', KEYS[1])\n"
                    + "if n < tonumber(ARGV[2]) then\n"
                    + "  redis.call('SADD', KEYS[1], ARGV[1])\n"
                    + "  return 1\n"
                    + "else\n"
                    + "  return 0\n"
                    + "end";

    private final RedisConfig config;
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final ExecutorService subscriberExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "redis-pubsub");
        t.setDaemon(true);
        return t;
    });

    public RedisService(RedisConfig config) {
        this.config = config;
    }

    public void start() {
        RedisURI uri = RedisURI.builder()
                .withHost(config.getHost())
                .withPort(config.getPort())
                .withDatabase(config.getDatabase())
                .withTimeout(Duration.ofSeconds(10))
                .build();
        client = RedisClient.create(uri);
        connection = client.connect();
        pubSubConnection = client.connectPubSub();
        LOG.info("Redis connected: {}:{}, db={}", config.getHost(), config.getPort(), config.getDatabase());
    }

    public void stop() {
        if (pubSubConnection != null) pubSubConnection.close();
        if (connection != null) connection.close();
        if (client != null) client.shutdown();
        subscriberExecutor.shutdown();
        LOG.info("Redis disconnected");
    }

    private RedisCommands<String, String> cmd() {
        return connection.sync();
    }

    // ---------- 玩家会话 (mp:player:{uid}:session) ----------

    public void setPlayerSession(int uid, String serverId, int roomId, String name, boolean isMonitor) {
        String key = RedisKeys.playerSession(uid);
        Map<String, String> map = new HashMap<>();
        map.put("server_id", serverId);
        map.put("room_id", String.valueOf(roomId));
        map.put("name", name != null ? name : "");
        map.put("is_monitor", String.valueOf(isMonitor));
        map.put("last_seen", String.valueOf(System.currentTimeMillis()));
        cmd().hset(key, map);
    }

    public void updatePlayerLastSeen(int uid) {
        String key = RedisKeys.playerSession(uid);
        cmd().hset(key, "last_seen", String.valueOf(System.currentTimeMillis()));
    }

    public void setPlayerRoom(int uid, int roomId) {
        String key = RedisKeys.playerSession(uid);
        cmd().hset(key, "room_id", String.valueOf(roomId));
    }

    public void removePlayerSession(int uid) {
        cmd().del(RedisKeys.playerSession(uid));
    }

    // ---------- 房间信息 (mp:room:{rid}:info) ----------

    public void setRoomInfo(String roomId, int hostId, RoomState state, Integer chartId, boolean isLocked, boolean isCycle) {
        String key = RedisKeys.roomInfo(roomId);
        Map<String, String> map = new HashMap<>();
        map.put("host_id", String.valueOf(hostId));
        map.put("state", String.valueOf(state.getCode()));
        map.put("chart_id", chartId != null ? String.valueOf(chartId) : "0");
        map.put("is_locked", String.valueOf(isLocked));
        map.put("is_cycle", String.valueOf(isCycle));
        cmd().hset(key, map);
    }

    public void setRoomStateAndChart(String roomId, RoomState state, Integer chartId) {
        String key = RedisKeys.roomInfo(roomId);
        cmd().hset(key, "state", String.valueOf(state.getCode()));
        if (chartId != null) cmd().hset(key, "chart_id", String.valueOf(chartId));
    }

    public Map<String, String> getRoomInfo(String roomId) {
        return cmd().hgetall(RedisKeys.roomInfo(roomId));
    }

    public void removeRoomInfo(String roomId) {
        cmd().del(RedisKeys.roomInfo(roomId));
    }

    // ---------- 房间成员 (mp:room:{rid}:players) ----------

    /** 原子加入：房间未满则 SADD 并返回 true，否则返回 false */
    public boolean joinRoomAtomic(String roomId, int uid, int maxPlayers) {
        String key = RedisKeys.roomPlayers(roomId);
        Long result = cmd().eval(LUA_JOIN_ROOM, ScriptOutputType.INTEGER,
                new String[]{key}, String.valueOf(uid), String.valueOf(maxPlayers));
        return result != null && result == 1;
    }

    public void addRoomPlayer(String roomId, int uid) {
        cmd().sadd(RedisKeys.roomPlayers(roomId), String.valueOf(uid));
    }

    public void removeRoomPlayer(String roomId, int uid) {
        cmd().srem(RedisKeys.roomPlayers(roomId), String.valueOf(uid));
    }

    public long getRoomPlayerCount(String roomId) {
        return cmd().scard(RedisKeys.roomPlayers(roomId));
    }

    public void removeRoomPlayers(String roomId) {
        cmd().del(RedisKeys.roomPlayers(roomId));
    }

    // ---------- Pub/Sub ----------

    public void publishEvent(String eventType, String roomId, Map<String, Object> data) {
        PubSubEvent event = new PubSubEvent(eventType, roomId, data);
        String json = GSON.toJson(event);
        connection.sync().publish(RedisKeys.EVENTS_CHANNEL, json);
    }

    /** 订阅 mp:events，在独立线程中调用 listener */
    public void subscribe(RedisEventListener listener) {
        RedisPubSubCommands<String, String> sync = pubSubConnection.sync();
        pubSubConnection.addListener(new io.lettuce.core.pubsub.RedisPubSubListener<String, String>() {
            @Override
            public void message(String channel, String message) {
                if (!RedisKeys.EVENTS_CHANNEL.equals(channel)) return;
                subscriberExecutor.execute(() -> {
                    try {
                        PubSubEvent event = GSON.fromJson(message, PubSubEvent.class);
                        if (event != null && event.getEvent() != null && event.getRoomId() != null) {
                            listener.onEvent(event.getEvent(), event.getRoomId(), event.getData());
                        }
                    } catch (Exception e) {
                        LOG.warn("Parse pub/sub event failed: {}", message, e);
                    }
                });
            }

            @Override
            public void message(String pattern, String channel, String message) {}

            @Override
            public void subscribed(String channel, long count) {}

            @Override
            public void psubscribed(String pattern, long count) {}

            @Override
            public void unsubscribed(String channel, long count) {}

            @Override
            public void punsubscribed(String pattern, long count) {}
        });
        sync.subscribe(RedisKeys.EVENTS_CHANNEL);
        LOG.info("Subscribed to Redis channel: {}", RedisKeys.EVENTS_CHANNEL);
    }

    public String getServerId() {
        return config.getServerId();
    }

    public interface RedisEventListener {
        void onEvent(String eventType, String roomId, Map<String, Object> data);
    }
}
