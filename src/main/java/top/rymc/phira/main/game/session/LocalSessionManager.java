package top.rymc.phira.main.game.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.session.PlayerSessionSuspendEvent;
import top.rymc.phira.main.game.i18n.I18nService;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.holder.SuspendableRoomHolder;
import top.rymc.phira.main.game.exception.session.ResumeFailedException;
import top.rymc.phira.main.game.exception.session.SuspendFailedException;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LocalSessionManager {

    private static final Map<Integer, SuspendedRoomSession> SUSPENDED = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);

    @Getter
    @Setter
    private static long suspendTimeoutMillis = TimeUnit.MINUTES.toMillis(5);

    public static void setSuspendTimeout(long timeout, TimeUnit unit) {
        suspendTimeoutMillis = unit.toMillis(timeout);
    }

    public static void resume(LocalPlayer player, PlayerConnection newConn) {
        SuspendedRoomSession session = SUSPENDED.remove(player.getId());
        if (session == null) {
            throw new ResumeFailedException();
        }

        ScheduledFuture<?> timeout = session.timeout;
        if (timeout != null) {
            timeout.cancel(false);
        }

        player.getRoom().ifPresent((room) -> {
            if (!room.containsPlayer(player)) {
                throw new ResumeFailedException();
            }
        });

        ServerBoundPacketHandler handler = player.getConnection().getPacketHandler();
        newConn.setPacketHandler(handler);

        player.getConnectionRef().resume(newConn, (oldConn) -> {
            oldConn.sendChat(I18nService.INSTANCE.getMessage(player.getLanguage(), "error.logged_in_elsewhere"));
        });
    }

    public static boolean suspend(LocalPlayer player, Runnable remover) {
        ServerBoundPacketHandler handler = player.getConnection().getPacketHandler();
        if (!(handler instanceof SuspendableRoomHolder roomHolder)) {
            throw new SuspendFailedException();
        }

        Room room = roomHolder.getRoom();
        if (room.containsMonitor(player)) {
            room.leave(player);
        }

        if (!room.containsPlayer(player)) {
            throw new SuspendFailedException();
        }

        PlayerSessionSuspendEvent suspendEvent = new PlayerSessionSuspendEvent(player, room);
        Server.postEvent(suspendEvent);
        if (suspendEvent.isCancelled()) {
            throw new SuspendFailedException();
        }

        SUSPENDED.compute(player.getId(), (id, oldSession) -> {
            if (oldSession != null && oldSession.timeout != null) {
                // Should not happen, TODO: warn in logger
                oldSession.timeout.cancel(false);
            }

            SuspendedRoomSession newSession = new SuspendedRoomSession(player);

            newSession.timeout = TIMER.schedule(
                    () -> forceLeave(id, newSession, remover),
                    suspendTimeoutMillis,
                    TimeUnit.MILLISECONDS
            );

            return newSession;
        });

        return true;
    }

    private static void forceLeave(int playerId, SuspendedRoomSession session, Runnable remover) {
        remover.run();

        if (!SUSPENDED.remove(playerId, session)) {
            return;
        }

        session.player.getRoom().ifPresent((room) -> {
            if (room.containsPlayer(session.player)) {
                room.leave(session.player);
            }
        });

    }

    @RequiredArgsConstructor
    private static final class SuspendedRoomSession {
        private final LocalPlayer player;
        private volatile ScheduledFuture<?> timeout;
    }
}