package top.rymc.phira.main.game.session;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.session.PlayerSessionSuspendEvent;
import top.rymc.phira.main.event.session.PlayerSessionTimeoutEvent;
import top.rymc.phira.main.game.i18n.I18nService;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.room.LocalRoom;
import top.rymc.phira.main.game.room.holder.SuspendableRoomHolder;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SessionManager {

    private static final Map<Integer, SuspendedRoomSession> SUSPENDED = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);

    @Getter
    @Setter
    private static long suspendTimeoutMillis = TimeUnit.MINUTES.toMillis(5);

    public static void setSuspendTimeout(long timeout, TimeUnit unit) {
        suspendTimeoutMillis = unit.toMillis(timeout);
    }

    public static boolean resume(LocalPlayer player, PlayerConnection newConn) {
        SuspendedRoomSession session = SUSPENDED.remove(player.getId());
        if (session == null) {
            return false;
        }

        ScheduledFuture<?> timeout = session.timeout;
        if (timeout != null) {
            timeout.cancel(false);
        }

        if (!session.room.getPlayerManager().containsPlayer(player)) {
            return false;
        }

        player.getConnectionRef().resume(newConn, (oldConn) -> {
            oldConn.sendChat(I18nService.INSTANCE.getMessage(player.getLanguage(), "error.logged_in_elsewhere"));
        });

        newConn.setPacketHandler(session.handler);

        return true;
    }

    public static boolean suspend(LocalPlayer player) {
        ServerBoundPacketHandler handler = player.getConnection().getPacketHandler();
        if (!(handler instanceof SuspendableRoomHolder roomHolder)) {
            return false;
        }

        LocalRoom room = roomHolder.getRoom();
        if (room.getPlayerManager().containsMonitor(player)) {
            room.leave(player);
        }

        if (!room.getPlayerManager().containsPlayer(player)) {
            return false;
        }

        PlayerSessionSuspendEvent suspendEvent = new PlayerSessionSuspendEvent(player, room);
        Server.postEvent(suspendEvent);
        if (suspendEvent.isCancelled()) {
            return false;
        }

        SUSPENDED.compute(player.getId(), (id, oldSession) -> {
            if (oldSession != null && oldSession.timeout != null) {
                oldSession.timeout.cancel(false);
            }

            SuspendedRoomSession newSession = new SuspendedRoomSession(
                    room,
                    player,
                    handler
            );

            newSession.timeout = TIMER.schedule(
                    () -> forceLeave(id, newSession),
                    suspendTimeoutMillis,
                    TimeUnit.MILLISECONDS
            );

            return newSession;
        });

        return true;
    }

    private static void forceLeave(int playerId, SuspendedRoomSession session) {
        if (!SUSPENDED.remove(playerId, session)) {
            return;
        }

        PlayerSessionTimeoutEvent timeoutEvent = new PlayerSessionTimeoutEvent(session.player, session.room);
        Server.postEvent(timeoutEvent);

        if (session.room.getPlayerManager().containsPlayer(session.player)) {
            session.room.leave(session.player);
        }
    }

    private static final class SuspendedRoomSession {
        private final LocalRoom room;
        private final LocalPlayer player;
        private final ServerBoundPacketHandler handler;
        private volatile ScheduledFuture<?> timeout;

        private SuspendedRoomSession(
                LocalRoom room,
                LocalPlayer player,
                ServerBoundPacketHandler handler
        ) {
            this.room = room;
            this.player = player;
            this.handler = handler;
        }
    }
}