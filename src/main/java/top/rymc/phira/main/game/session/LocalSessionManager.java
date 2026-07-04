package top.rymc.phira.main.game.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.game.exception.session.ResumeFailedException;
import top.rymc.phira.main.game.exception.session.SuspendFailedException;
import top.rymc.phira.main.game.player.local.LocalPlayer;
import top.rymc.phira.main.game.room.ProtocolHackService;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.holder.SuspendableRoomHolder;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.game.room.state.RoomPlaying;
import top.rymc.phira.main.game.room.state.RoomWaitForReady;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

public class LocalSessionManager {

    private static final Map<Integer, SuspendedRoomSession> SUSPENDED = new ConcurrentHashMap<>();
    private static final AtomicLong SESSION_VERSION = new AtomicLong();
    private static final Lock[] LOCKS = new Lock[64];
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);

    static {
        for (int i = 0; i < LOCKS.length; i++) {
            LOCKS[i] = new ReentrantLock();
        }
    }

    @Getter
    @Setter
    private static long suspendTimeoutMillis = TimeUnit.MINUTES.toMillis(5);

    public static void setSuspendTimeout(long timeout, TimeUnit unit) {
        suspendTimeoutMillis = unit.toMillis(timeout);
    }

    public static void resume(LocalPlayer player, PlayerConnection newConn) throws ResumeFailedException {
        Lock lock = lockFor(player.getId());
        lock.lock();
        try {
            SuspendedRoomSession session = SUSPENDED.get(player.getId());
            if (session == null) {
                Server.logPluginSensitiveIssue("Resume failed without suspended session, player {}", player.getId());
                throw new ResumeFailedException();
            }

            if (!removeSession(player.getId(), session)) {
                Server.logPluginSensitiveIssue("Resume session CAS failed, player {}, session {}", player.getId(), session.version);
                throw new ResumeFailedException();
            }

            ScheduledFuture<?> timeout = session.timeout;
            if (timeout != null) {
                timeout.cancel(false);
            }

            Room room = session.room;
            if (!room.containsPlayer(player)) {
                Server.logPluginSensitiveIssue("Resume failed because player is not in room, player {}, room {}, session {}", player.getId(), room.getRoomId(), session.version);
                throw new ResumeFailedException();
            }

            ServerBoundPacketHandler handler = session.oldConnection.getPacketHandler();
            newConn.setPacketHandler(handler);

            if (!player.getConnectionRef().swap(session.oldConnection, newConn)) {
                Server.getLogger().error(
                        "Failed to resume player {}, session {}, oldConnection {}, newConnection {}, actualConnection {}",
                        player.getId(), session.version, session.oldConnection, newConn, player.getConnection()
                );
                newConn.close();
                throw new ResumeFailedException();
            }

            if (!session.oldConnection.isClosed()) {
                Server.getLogger().error(
                        "Old connection still active after resume, player {}, session {}, oldConnection {}",
                        player.getId(), session.version, session.oldConnection
                );
                session.oldConnection.markDuplicateLogin();
            }

            ProtocolHackService.reconnect(room, player);
        } finally {
            lock.unlock();
        }
    }

    public static void suspend(LocalPlayer player, PlayerConnection oldConn, BooleanSupplier remover) throws SuspendFailedException {
        Lock lock = lockFor(player.getId());
        lock.lock();
        try {
            if (player.getConnection() != oldConn) {
                Server.logPluginSensitiveIssue("Suspend failed because connection changed, player {}, oldConnection {}, actualConnection {}", player.getId(), oldConn, player.getConnection());
                throw new SuspendFailedException();
            }

            ServerBoundPacketHandler handler = oldConn.getPacketHandler();
            if (!(handler instanceof SuspendableRoomHolder roomHolder)) {
                Server.logPluginSensitiveIssue("Suspend failed because handler is not room holder, player {}, handler {}", player.getId(), handler);
                throw new SuspendFailedException();
            }

            Room room = roomHolder.getRoom();
            if (room.containsMonitor(player)) {
                boolean left = room.leave(player);
                if (!left) {
                    Server.logPluginSensitiveIssue("Monitor leave failed during suspend, player {}, room {}", player.getId(), room.getRoomId());
                }
                throw new SuspendFailedException();
            }

            if (!room.containsPlayer(player)) {
                Server.logPluginSensitiveIssue("Suspend failed because player is not in players, player {}, room {}", player.getId(), room.getRoomId());
                throw new SuspendFailedException();
            }

            RoomGameState state = room.getView().getState();
            if (state instanceof RoomWaitForReady) {
                room.getOperation().cancelReady(player);
            } else if (state instanceof RoomPlaying) {
                room.getOperation().abort(player);
            }

            SuspendedRoomSession oldSession = SUSPENDED.remove(player.getId());
            if (oldSession != null && oldSession.timeout != null) {
                Server.getLogger().warn("Player {} already has a suspended session, cancelling old timeout", player.getId());
                oldSession.timeout.cancel(false);
            }

            SuspendedRoomSession session = new SuspendedRoomSession(
                    SESSION_VERSION.incrementAndGet(),
                    player,
                    room,
                    state.getClass(),
                    oldConn,
                    System.currentTimeMillis(),
                    remover
            );

            SUSPENDED.put(player.getId(), session);
            session.timeout = TIMER.schedule(
                    () -> forceLeave(player.getId(), session),
                    suspendTimeoutMillis,
                    TimeUnit.MILLISECONDS
            );
        } finally {
            lock.unlock();
        }
    }

    public static boolean hasSuspendedSession(int playerId) {
        return SUSPENDED.containsKey(playerId);
    }

    public static void removeSuspendedSession(LocalPlayer player) {
        SuspendedRoomSession session = SUSPENDED.remove(player.getId());
        if (session != null && session.timeout != null) {
            session.timeout.cancel(false);
        }
    }

    private static void forceLeave(int playerId, SuspendedRoomSession session) {
        if (!removeSession(playerId, session)) {
            return;
        }

        boolean left = true;
        if (session.room.containsPlayer(session.player)) {
            left = session.room.leave(session.player);
        }

        boolean removed = session.remover.getAsBoolean();
        if (!left || !removed) {
            Server.logPluginSensitiveIssue(
                    "Failed to timeout suspended session, player {}, session {}, leave {}, remove {}",
                    playerId, session.version, left, removed
            );
        }
    }

    private static boolean removeSession(int playerId, SuspendedRoomSession expected) {
        return SUSPENDED.remove(playerId, expected);
    }

    private static Lock lockFor(int playerId) {
        return LOCKS[Math.floorMod(playerId, LOCKS.length)];
    }

    @RequiredArgsConstructor
    private static final class SuspendedRoomSession {
        private final long version;
        private final LocalPlayer player;
        private final Room room;
        private final Class<? extends RoomGameState> stateType;
        private final PlayerConnection oldConnection;
        private final long createdAtMillis;
        private final BooleanSupplier remover;
        private volatile ScheduledFuture<?> timeout;
    }
}
