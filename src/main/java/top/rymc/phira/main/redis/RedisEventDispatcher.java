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
            String myServerId = r.getConfig().getServerId();
            if (event.getServerId() != null && event.getServerId().equals(myServerId)) {
                return;
            }
            String roomId = event.getRoomId();
            if (roomId == null || roomId.isEmpty()) return;

            String type = event.getEvent();
            Object data = event.getData();
            if (data == null && !"ROOM_CREATE".equals(type)) return;

            JsonObject obj = data != null
                    ? (data instanceof JsonObject ? (JsonObject) data : GSON.toJsonTree(data).getAsJsonObject())
                    : null;

            switch (type == null ? "" : type) {
                case "ROOM_CREATE" -> handleRoomCreate(roomId, obj);
                case "ROOM_DELETE" -> {
                    Room room = RoomManager.findRoom(roomId);
                    if (room != null) handleRoomDelete(room);
                }
                case "PLAYER_JOIN" -> {
                    Room room = RoomManager.findRoom(roomId);
                    if (room != null) handlePlayerJoin(room, obj);
                }
                case "PLAYER_LEAVE" -> {
                    Room room = RoomManager.findRoom(roomId);
                    if (room != null) handlePlayerLeave(room, obj);
                }
                case "STATE_CHANGE" -> {
                    Room room = RoomManager.findRoom(roomId);
                    if (room != null) handleStateChange(room, obj);
                }
                case "SYNC_SCORE" -> {
                    Room room = RoomManager.findRoom(roomId);
                    if (room != null) handleSyncScore(room, obj);
                }
                default -> { }
            }
        });
    }

    private static void handleRoomCreate(String roomId, JsonObject data) {
        if (RoomManager.findRoom(roomId) != null) return;
        int uid = (data != null && data.has("uid")) ? data.get("uid").getAsInt() : -1;
        String name = (data != null && data.has("name")) ? data.get("name").getAsString() : "";
        RoomManager.createRemoteRoom(roomId, uid, name);
    }

    private static void handleRoomDelete(Room room) {
        room.forceDestroy();
    }

    private static void handlePlayerJoin(Room room, JsonObject data) {
        int uid = data.has("uid") ? data.get("uid").getAsInt() : -1;
        String name = data.has("name") ? data.get("name").getAsString() : "";
        boolean isMonitor = data.has("is_monitor") && data.get("is_monitor").getAsBoolean();
        room.joinRemote(uid, name, isMonitor);
        UserProfile profile = new UserProfile(uid, name);
        room.broadcast(new ClientBoundOnJoinRoomPacket(profile, isMonitor));
        room.broadcast(new ClientBoundMessagePacket(new JoinRoomMessage(uid, name)));
    }

    private static void handlePlayerLeave(Room room, JsonObject data) {
        int uid = data.has("uid") ? data.get("uid").getAsInt() : -1;
        room.removeRemotePlayer(uid);
        room.broadcast(new ClientBoundMessagePacket(new LeaveRoomMessage(uid, "")));
        if (data.has("is_host_changed") && data.get("is_host_changed").getAsBoolean()) {
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
