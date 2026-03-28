package top.rymc.phira.main.game.session;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.login.PlayerPostResumeEvent;
import top.rymc.phira.main.event.login.PlayerPreResumeEvent;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.holder.SuspendableRoomHolder;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.handler.PlayHandler;
import top.rymc.phira.main.network.handler.RoomHandler;
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

    public static boolean resume(Player player, PlayerConnection newConn) {
        SuspendedRoomSession session = SUSPENDED.remove(player.getId());
        if (session == null) {
            return false;
        }

        ScheduledFuture<?> timeout = session.timeout;
        if (timeout != null) {
            timeout.cancel(false);
        }

        PlayerPreResumeEvent preResumeEvent =
                new PlayerPreResumeEvent(newConn, player, session.room, session.handlerClass);
        Server.postEvent(preResumeEvent);

        if (!session.room.containsPlayer(player)) {
            return false;
        }

        if (preResumeEvent.isCancelled()) {
            session.room.leave(player);
            return false;
        }

        player.bind(newConn);

        PlayerPostResumeEvent postResumeEvent =
                new PlayerPostResumeEvent(newConn, player, session.room, session.handlerClass);
        Server.postEvent(postResumeEvent);

        ServerBoundPacketHandler handler = resolveHandler(player, postResumeEvent, session);
        newConn.setPacketHandler(handler);

        return true;
    }

    public static boolean suspend(Player player) {
        ServerBoundPacketHandler handler = player.getConnection().getPacketHandler();
        if (!(handler instanceof SuspendableRoomHolder roomHolder)) {
            return false;
        }

        Room room = roomHolder.getRoom();
        if (room.containsMonitor(player)) {
            room.leave(player);
        }

        if (!room.containsPlayer(player)) {
            return false;
        }

        SUSPENDED.compute(player.getId(), (id, oldSession) -> {
            if (oldSession != null && oldSession.timeout != null) {
                oldSession.timeout.cancel(false);
            }

            SuspendedRoomSession newSession = new SuspendedRoomSession(
                    room,
                    player,
                    handler.getClass()
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

        if (session.room.containsPlayer(session.player)) {
            session.room.leave(session.player);
        }
    }

    private static ServerBoundPacketHandler resolveHandler(Player player, PlayerPostResumeEvent event, SuspendedRoomSession session) {
        ServerBoundPacketHandler eventHandler = event.getPacketHandler();
        if (eventHandler != null) {
            return eventHandler;
        }

        if (session.handlerClass == RoomHandler.class) {
            return new RoomHandler(player, session.room, PlayHandler.create(player));
        }

        Server.getLogger().warn(
                "Custom room packet handler {} was not restored during PlayerPostResumeEvent. " +
                "Player {} has been removed from room {} and will fall back to default PlayHandler.",
                session.handlerClass.getName(),
                player.getId(),
                session.room.getRoomId()
        );

        session.room.leave(player);
        return PlayHandler.create(player);
    }

    private static final class SuspendedRoomSession {
        private final Room room;
        private final Player player;
        private final Class<? extends ServerBoundPacketHandler> handlerClass;
        private volatile ScheduledFuture<?> timeout;

        private SuspendedRoomSession(
                Room room,
                Player player,
                Class<? extends ServerBoundPacketHandler> handlerClass
        ) {
            this.room = room;
            this.player = player;
            this.handlerClass = handlerClass;
        }
    }
}