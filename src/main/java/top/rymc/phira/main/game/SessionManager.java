package top.rymc.phira.main.game;

import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;
import top.rymc.phira.main.network.handler.RoomHandler;

import java.util.Map;
import java.util.concurrent.*;

public class SessionManager {
    private static final Map<Integer, SuspendedRoomSession> SUSPENDED = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);

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

        if (rh.getRoom().containsMonitor(player)) {
            rh.getRoom().leave(player);
        }

        if (!rh.getRoom().containsPlayer(player)) {
            return false;
        }

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