package top.rymc.phira.main.game;

import top.rymc.phira.main.redis.PubSubEvent;
import top.rymc.phira.main.redis.RedisHolder;
import top.rymc.phira.main.redis.RoomState;
import top.rymc.phira.protocol.data.UserProfile;
import top.rymc.phira.protocol.data.message.JoinRoomMessage;
import top.rymc.phira.protocol.data.message.LeaveRoomMessage;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.Playing;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.data.state.WaitForReady;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeStatePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundOnJoinRoomPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();

    public static Room createRoom(String roomId, Player host) {
        if (ROOMS.containsKey(roomId)) {
            throw new IllegalStateException("Room already exists");
        }
        Room room = Room.create(roomId, r -> {
            ROOMS.remove(r.getRoomId());
            var redis = RedisHolder.get();
            redis.removeRoomInfo(r.getRoomId());
            redis.removeRoomPlayers(r.getRoomId());
        }, host);
        ROOMS.put(roomId, room);
        var redis = RedisHolder.get();
        redis.setRoomInfo(roomId, host.getId(), RoomState.SelectChart, null,
                room.getSetting().isLocked(), room.getSetting().isCycle());
        redis.addRoomPlayer(roomId, host.getId());
        return room;
    }

    /** 查找房间；若不存在则尝试从 Redis 加载并创建本地视图（加入跨服房间时用） */
    public static Room findRoom(String roomId) {
        return ROOMS.get(roomId);
    }

    /** 获取或从 Redis 加载房间，用于跨服加入时创建本地房间视图 */
    public static Room getOrCreateFromRedis(String roomId, Player joiningPlayer) {
        Room existing = ROOMS.get(roomId);
        if (existing != null) return existing;
        var redis = RedisHolder.get();
        Map<String, String> info = redis.getRoomInfo(roomId);
        if (info == null || info.isEmpty()) return null;
        Room room = Room.createFromRedis(roomId, r -> {
            ROOMS.remove(r.getRoomId());
            redis.removeRoomInfo(r.getRoomId());
            redis.removeRoomPlayers(r.getRoomId());
        }, info, joiningPlayer);
        ROOMS.put(roomId, room);
        return room;
    }

    /**
     * 处理来自 Redis Pub/Sub 的跨服事件，向本机该房间内玩家广播对应协议包。
     */
    public static void handleRedisEvent(String roomId, String eventType, Map<String, Object> data) {
        if (roomId == null || eventType == null || data == null) return;
        Room room = ROOMS.get(roomId);
        if (room == null) return;

        switch (eventType) {
            case PubSubEvent.STATE_CHANGE -> {
                int newState = number(data.get("new_state"), 0);
                Integer chartId = data.get("chart_id") instanceof Number n ? n.intValue() : null;
                GameState gs = switch (newState) {
                    case 1 -> new WaitForReady();
                    case 2 -> new Playing();
                    default -> new SelectChart(chartId);
                };
                room.broadcast(new ClientBoundChangeStatePacket(gs));
            }
            case PubSubEvent.PLAYER_JOIN -> {
                int uid = number(data.get("uid"), 0);
                String name = data.get("name") != null ? String.valueOf(data.get("name")) : "";
                boolean isMonitor = Boolean.TRUE.equals(data.get("is_monitor"));
                room.broadcast(new ClientBoundOnJoinRoomPacket(new UserProfile(uid, name), isMonitor));
                room.broadcast(new ClientBoundMessagePacket(new JoinRoomMessage(uid, name)));
            }
            case PubSubEvent.PLAYER_LEAVE -> {
                int uid = number(data.get("uid"), 0);
                String name = data.get("name") != null ? String.valueOf(data.get("name")) : "";
                room.broadcast(new ClientBoundMessagePacket(new LeaveRoomMessage(uid, name)));
            }
            case PubSubEvent.SYNC_SCORE -> {
                // Touches/Judges 仅通过 Pub/Sub 推送，不写库；具体协议包可后续扩展
            }
            default -> {}
        }
    }

    private static int number(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        return def;
    }
}