package top.rymc.phira.main.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.game.RoomManager;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.data.state.WaitForReady;
import top.rymc.phira.protocol.data.state.Playing;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.message.JoinRoomMessage;
import top.rymc.phira.protocol.data.message.LeaveRoomMessage;
import top.rymc.phira.protocol.data.message.PlayedMessage;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeHostPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeStatePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundOnJoinRoomPacket;
import top.rymc.phira.protocol.data.UserProfile;

import java.util.Map;

/**
 * 订阅 mp:events，将事件转换为协议包并广播给本机该房间内的连接。
 */
public class RedisEventDispatcher {
    private static final Logger logger = LogManager.getLogger(RedisEventDispatcher.class);
    private static final Gson GSON = new Gson();

    public static void start(RedisManager redis) {
        Room.setStateChangeCallback(room -> {
            if (!RedisHolder.isAvailable()) return;
            var r = RedisHolder.get();
            int state = RoomStateCode.from(room.getState());
            int chartId = room.getState().getChart() != null ? room.getState().getChart().getId() : 0;
            r.updateRoomState(room.getRoomId(), state, chartId);
            r.publishStateChange(room.getRoomId(), state, chartId);
        });

        Room.setLeaveCallback(ctx -> {
            if (!RedisHolder.isAvailable()) return;
            var r = RedisHolder.get();
            Room room = ctx.room();
            Player player = ctx.player();
            String roomId = room.getRoomId();
            boolean isHost = room.isHost(player);
            int newHostId = room.getNewHostIdIfLeave(player);
            boolean willBeDestroyed = room.getMemberCount() == 1;
            r.removeRoomPlayer(roomId, player.getId());
            r.updatePlayerSessionRoom(player.getId(), null);
            if (isHost && newHostId != 0) r.updateRoomHost(roomId, newHostId);
            r.publishPlayerLeave(roomId, player.getId(), isHost && newHostId != 0, newHostId);
            if (willBeDestroyed) {
                r.removeRoomInfo(roomId);
                r.removeRoomPlayers(roomId);
                r.publishRoomDelete(roomId, player.getId(), player.getName());
            }
        });

        redis.subscribe(event -> {
            RedisManager r = RedisHolder.get();
            if (r == null) return;
            String roomId = event.getRoomId();
            if (roomId == null || roomId.isEmpty()) return;
            Room room = RoomManager.findRoom(roomId);
            if (room == null) return;

            String type = event.getEvent();
            Object data = event.getData();
            if (data == null) return;

            JsonObject obj = data instanceof JsonObject ? (JsonObject) data : GSON.toJsonTree(data).getAsJsonObject();

            switch (type == null ? "" : type) {
                case "ROOM_CREATE" -> { /* 创建端已处理，其他服仅作通知用，可不推送 */ }
                case "ROOM_DELETE" -> handleRoomDelete(room);
                case "PLAYER_JOIN" -> handlePlayerJoin(room, obj);
                case "PLAYER_LEAVE" -> handlePlayerLeave(room, obj);
                case "STATE_CHANGE" -> handleStateChange(room, obj);
                case "SYNC_SCORE" -> handleSyncScore(room, obj);
                default -> { }
            }
        });
    }

    private static void handleRoomDelete(Room room) {
        // 房间被删除，本机若有该房间则由上层在销毁时已处理；这里仅做广播给还在房内的本地玩家（若房间还在）
        // 实际由 RoomManager 在收到删除逻辑时移除房间，此处只做状态同步
    }

    private static void handlePlayerJoin(Room room, JsonObject data) {
        int uid = data.has("uid") ? data.get("uid").getAsInt() : -1;
        String name = data.has("name") ? data.get("name").getAsString() : "";
        boolean isMonitor = data.has("is_monitor") && data.get("is_monitor").getAsBoolean();
        UserProfile profile = new UserProfile(uid, name);
        room.broadcast(new ClientBoundOnJoinRoomPacket(profile, isMonitor));
        room.broadcast(new ClientBoundMessagePacket(new JoinRoomMessage(uid, name)));
    }

    private static void handlePlayerLeave(Room room, JsonObject data) {
        int uid = data.has("uid") ? data.get("uid").getAsInt() : -1;
        room.broadcast(new ClientBoundMessagePacket(new LeaveRoomMessage(uid, "")));
        if (data.has("is_host_changed") && data.get("is_host_changed").getAsBoolean()) {
            int newHost = data.has("new_host") ? data.get("new_host").getAsInt() : 0;
            room.broadcast(new ClientBoundChangeHostPacket(true));
        }
    }

    private static void handleStateChange(Room room, JsonObject data) {
        int newState = data.has("new_state") ? data.get("new_state").getAsInt() : 0;
        int chartId = data.has("chart_id") ? data.get("chart_id").getAsInt() : 0;
        GameState protocolState = switch (newState) {
            case RoomStateCode.SELECT_CHART -> new SelectChart(chartId);
            case RoomStateCode.WAITING_FOR_READY -> new WaitForReady();
            case RoomStateCode.PLAYING -> new Playing();
            default -> new SelectChart(chartId);
        };
        room.broadcast(new ClientBoundChangeStatePacket(protocolState));
    }

    private static void handleSyncScore(Room room, JsonObject data) {
        int uid = data.has("uid") ? data.get("uid").getAsInt() : -1;
        String recordIdStr = data.has("record_id") ? data.get("record_id").getAsString() : "0";
        int recordId;
        try {
            recordId = Integer.parseInt(recordIdStr);
        } catch (NumberFormatException e) {
            return;
        }
        try {
            var record = PhiraFetcher.GET_RECORD_INFO.toIntFunction(ignored -> null).apply(recordId);
            if (record == null) return;
            room.broadcast(new ClientBoundMessagePacket(new PlayedMessage(
                    uid, record.getScore(), record.getAccuracy(), record.isFullCombo())));
        } catch (Exception e) {
            logger.debug("Fetch record for SYNC_SCORE failed: recordId={}", recordId, e);
        }
    }
}
