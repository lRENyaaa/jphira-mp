package top.rymc.phira.main.game;

import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;
import top.rymc.phira.main.network.handler.RoomHandler;

import java.util.Map;
import java.util.concurrent.*;

public class SessionManager {
    private static final Map<Integer, SuspendedRoomSession> SUSPENDED = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);

    /**
     * 尝试恢复玩家的房间会话
     * @return true 如果成功恢复，false 如果没有可恢复的会话
     */
    public static boolean resume(Player player, PlayerConnection newConn) {
        SuspendedRoomSession session = SUSPENDED.remove(player.getId());
        if (session == null) {
            return false; // 没有挂起的会话
        }

        // 取消超时踢出任务
        session.timeout.cancel(false);

        // 验证玩家是否还在房间内（可能已被其他机制踢出）
        if (!session.room.containsPlayer(player)) {
            return false; // 已经不在房间了
        }

        // 绑定新连接
        player.bind(newConn);

        // 创建新的 RoomHandler
        RoomHandler handler = new RoomHandler(player, session.room, PlayHandler.create(player));
        newConn.setPacketHandler(handler);

        return true;
    }

    /**
     * 挂起会话（断线时调用）
     */
    public static boolean suspend(Player player) {
        if (!(player.getConnection().getPacketHandler() instanceof RoomHandler rh)) {
            return false; // 不在房间中，无需挂起
        }

        if (rh.getRoom().containsMonitor(player)) {
            rh.getRoom().leave(player);
        }

        if (!rh.getRoom().containsPlayer(player)) {
            return false;
        }

        // 如果已经有挂起的会话，先取消旧的定时器
        SuspendedRoomSession old = SUSPENDED.get(player.getId());
        if (old != null) {
            old.timeout.cancel(false);
        }

        SuspendedRoomSession session = new SuspendedRoomSession(
                rh.getRoom(),
                player,
                TIMER.schedule(() -> forceLeave(player, rh.getRoom()), 5, TimeUnit.MINUTES)
        );

        SUSPENDED.put(player.getId(), session);
        return true;
    }

    private static void forceLeave(Player player, Room room) {
        SUSPENDED.remove(player.getId());
        if (room.containsPlayer(player)) {
            room.leave(player);
        }
    }

    private record SuspendedRoomSession(Room room, Player player, ScheduledFuture<?> timeout) {}
}