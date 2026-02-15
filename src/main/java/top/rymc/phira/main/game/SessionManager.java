package top.rymc.phira.main.game;

import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;
import top.rymc.phira.main.network.handler.RoomHandler;

import java.util.Map;
import java.util.concurrent.*;

public class SessionManager {
    private static final Map<Integer, SuspendedRoomSession> SUSPENDED = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);
    private static long suspendTimeoutMillis = TimeUnit.MINUTES.toMillis(5);

    public static void setSuspendTimeout(long timeout, TimeUnit unit) {
        suspendTimeoutMillis = unit.toMillis(timeout);
    }

    public static long getSuspendTimeoutMillis() {
        return suspendTimeoutMillis;
    }

    public static boolean resume(Player player, PlayerConnection newConn) {
        SuspendedRoomSession session = SUSPENDED.remove(player.getId());
        if (session == null) {
            return false;
        }

        session.timeout.cancel(false);

        if (!session.room.containsPlayer(player)) {
            return false;
        }

        player.bind(newConn);

        RoomHandler handler = new RoomHandler(player, session.room, PlayHandler.create(player));
        newConn.setPacketHandler(handler);

        return true;
    }

    public static boolean suspend(Player player) {
        if (!(player.getConnection().getPacketHandler() instanceof RoomHandler rh)) {
            return false;
        }

        Room room = rh.getRoom();
        if (room.containsMonitor(player)) {
            room.leave(player);
        }

        if (!room.containsPlayer(player)) {
            return false;
        }

        return SUSPENDED.compute(player.getId(), (id, oldSession) -> {
            if (oldSession != null) {
                oldSession.timeout.cancel(false);
            }
            return new SuspendedRoomSession(
                    room,
                    player,
                    TIMER.schedule(() -> forceLeave(player, room), suspendTimeoutMillis, TimeUnit.MILLISECONDS)
            );
        }) != null;
    }

    private static void forceLeave(Player player, Room room) {
        SUSPENDED.remove(player.getId());
        if (room.containsPlayer(player)) {
            room.leave(player);
        }
    }

    private record SuspendedRoomSession(Room room, Player player, ScheduledFuture<?> timeout) {}
}