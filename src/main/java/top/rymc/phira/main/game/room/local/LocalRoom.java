package top.rymc.phira.main.game.room.local;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.event.operation.RoomChatEvent;
import top.rymc.phira.main.event.operation.RoomCycleChangeEvent;
import top.rymc.phira.main.event.operation.RoomLockChangeEvent;
import top.rymc.phira.main.event.operation.RoomPostSelectChartEvent;
import top.rymc.phira.main.event.operation.RoomPreSelectChartEvent;
import top.rymc.phira.main.event.room.PlayerLeaveRoomEvent;
import top.rymc.phira.main.event.room.RoomDestroyEvent;
import top.rymc.phira.main.event.room.RoomHostChangeEvent;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.game.room.RoomSnapshot;
import top.rymc.phira.main.game.room.state.RoomGameState;
import top.rymc.phira.main.game.room.state.RoomGameStateReference;
import top.rymc.phira.main.game.room.state.RoomSelectChart;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class LocalRoom implements Room {

    private final Object lifecycleLock = new Object();

    @Getter
    private final String roomId;
    @Getter
    private final RoomSetting setting;

    private final Runnable onDestroy;
    private final RoomGameStateReference stateRef;

    public LocalRoom(
            Runnable onDestroy,
            String roomId,
            RoomSetting setting,
            RoomGameState.Type state,
            ChartInfo chart
    ) {
        this.roomId = roomId;
        this.onDestroy = onDestroy;
        this.setting = setting;
        this.stateRef = new RoomGameStateReference(updater -> state.build(this, updater, chart));
    }

    @Getter
    @Setter(AccessLevel.PRIVATE)
    @AllArgsConstructor
    public static class RoomSetting {
        private boolean autoDestroy;
        private boolean host;
        private int maxPlayer;
        private boolean locked;
        private boolean cycle;
        private boolean live;
        private boolean chat;

    }

    @Getter
    private final PlayerManager playerManager = new PlayerManager();

    @SuppressWarnings("InnerClassMayBeStatic")
    public class PlayerManager {

        @Getter
        private Player host;

        private final Set<Player> players = ConcurrentHashMap.newKeySet();
        private final Set<Player> monitors = ConcurrentHashMap.newKeySet();

        public Set<Player> getPlayersCopy() {
            return Set.copyOf(players);
        }

        public Set<Player> getMonitorsCopy() {
            return Set.copyOf(monitors);
        }

        public Set<Player> getPlayers() {
            return Collections.unmodifiableSet(players);
        }

        public Set<Player> getMonitors() {
            return Collections.unmodifiableSet(monitors);
        }

        public boolean containsPlayer(Player player) {
            return players.contains(player);
        }

        public boolean containsMonitor(Player player) {
            return monitors.contains(player);
        }

        public void transferHostToNextPlayer() {
            Player previousHost = host;

            List<Player> sorted = players.stream()
                    .sorted(Comparator.comparing(Player::getId))
                    .toList();

            Player nextHost = sorted.stream().findFirst().orElse(null);

            if (previousHost != null) {
                for (Player player : sorted) {
                    if (player.getId() <= previousHost.getId()) {
                        continue;
                    }

                    nextHost = player;
                    break;
                }
            }

            if (nextHost != null) {
                host = nextHost;
                if (previousHost != null) {
                    previousHost.operations().ifPresent((o) -> o.updateHostStatus(false));
                }
                nextHost.operations().ifPresent((o) -> o.updateHostStatus(true));

                RoomHostChangeEvent event = new RoomHostChangeEvent(LocalRoom.this, previousHost, host);
                Server.postEvent(event);

                return;
            }

            if (host != null) {
                RoomHostChangeEvent event = new RoomHostChangeEvent(LocalRoom.this, host, null);
                Server.postEvent(event);
            }

            host = null;
        }

        public void broadcast(Consumer<PlayerOperations> action) {
            players.forEach(p -> p.operations().ifPresent(action));
            monitors.forEach(p -> p.operations().ifPresent(action));
        }

        public void broadcastToMonitors(Consumer<PlayerOperations> action) {
            monitors.forEach(p -> p.operations().ifPresent(action));
        }
    }

    public boolean containsPlayer(Player player) {
        return playerManager.containsPlayer(player);
    }

    public boolean containsMonitor(Player player) {
        return playerManager.containsMonitor(player);
    }

    public boolean isHost(Player player) {
        return playerManager.host != null && setting.host && player.getId() == playerManager.host.getId();
    }

    public void join(Player player, boolean isMonitor) {
        boolean shouldBroadcastJoin = false;

        synchronized (lifecycleLock) {
            if (!isMonitor && playerManager.players.size() >= setting.maxPlayer) {
                throw GameOperationException.roomFull();
            }

            if (!isMonitor && setting.locked && !playerManager.players.isEmpty()) {
                throw GameOperationException.roomLocked();
            }

            Set<Player> set = isMonitor ? playerManager.monitors : playerManager.players;
            boolean added = set.add(player);
            if (!added) {
                return;
            }

            if (!isMonitor && playerManager.players.size() == 1 && setting.host) {
                playerManager.host = player;
            } else {
                shouldBroadcastJoin = true;
            }
        }

        if (shouldBroadcastJoin) {
            playerManager.broadcast(op -> op.memberJoined(player.getId(), player.getName(), isMonitor));
        }

        stateRef.get().handleJoin(player);
    }

    public void leave(Player player) {
        boolean shouldDestroy = false;

        synchronized (lifecycleLock) {
            if (!playerManager.players.remove(player) && !playerManager.monitors.remove(player)) {
                return;
            }

            if (playerManager.players.isEmpty() && playerManager.monitors.isEmpty()) {
                shouldDestroy = setting.autoDestroy;
            }

            if (setting.host && player.equals(playerManager.host)) {
                playerManager.transferHostToNextPlayer();
            }
        }

        playerManager.broadcast(op -> op.memberLeft(player.getId(), player.getName()));
        stateRef.get().handleLeave(player);

        PlayerLeaveRoomEvent event = new PlayerLeaveRoomEvent(player, this);
        Server.postEvent(event);

        if (shouldDestroy) {
            destroyRoom();
        }
    }

    @Getter
    private final LocalOperation operation = new LocalOperation();

    public class LocalOperation implements Operation {

        private void validateHost(Player player) {
            if (!isHost(player)) {
                throw GameOperationException.permissionDenied();
            }
        }

        public void lockRoom(Player player) {
            validateHost(player);

            boolean newLockState = !setting.locked;

            RoomLockChangeEvent event = new RoomLockChangeEvent(LocalRoom.this, player, newLockState);
            Server.postEvent(event);

            setting.locked = newLockState;
            playerManager.broadcast(op -> op.lockRoom(setting.locked));
        }

        public void cycleRoom(Player player) {
            validateHost(player);

            boolean newCycleState = !setting.cycle;

            RoomCycleChangeEvent event = new RoomCycleChangeEvent(LocalRoom.this, player, newCycleState);
            Server.postEvent(event);

            setting.cycle = newCycleState;
            playerManager.broadcast(op -> op.cycleRoom(setting.cycle));
        }

        public void selectChart(Player player, int id) {
            validateHost(player);

            if (!(stateRef.get() instanceof RoomSelectChart)) {
                throw GameOperationException.invalidState();
            }

            RoomPreSelectChartEvent preEvent = new RoomPreSelectChartEvent(LocalRoom.this, player, id);
            Server.postEvent(preEvent);
            if (preEvent.isCancelled()) {
                throw new GameOperationException(preEvent.getCancelReason());
            }

            IntFunction<ChartInfo> getInfoFunc = PhiraFetcher.GET_CHART_INFO.toIntFunction(e -> {
                throw GameOperationException.chartNotFound();
            });

            ChartInfo eventChartInfo = preEvent.getChartInfo();
            ChartInfo info = eventChartInfo != null ? eventChartInfo : getInfoFunc.apply(id);

            stateRef.get().setChart(info);
            playerManager.broadcast(operations -> operations.selectChart(info.getId(), info.getName(), player.getId()));

            RoomPostSelectChartEvent postEvent = new RoomPostSelectChartEvent(LocalRoom.this, player, info);
            Server.postEvent(postEvent);
        }

        public void chat(Player player, String message) {
            if (!setting.chat) {
                throw GameOperationException.chatNotEnabled();
            }

            RoomChatEvent event = new RoomChatEvent(player, LocalRoom.this, message);
            if (Server.postEvent(event)) {
                return;
            }

            playerManager.broadcast(operations -> operations.receiveChat(player.getId(), event.getMessage()));
        }

        public void touchSend(Player player, List<TouchFrame> touchFrames) {
            stateRef.get().touchSend(player, touchFrames);
            playerManager.broadcastToMonitors(operations -> operations.receiveTouchStream(player.getId(), touchFrames));
        }

        public void judgeSend(Player player, List<JudgeEvent> judgeEvents) {
            stateRef.get().judgeSend(player, judgeEvents);
            playerManager.broadcastToMonitors(operations -> operations.receiveJudgeStream(player.getId(), judgeEvents));
        }

        public void requireStart(Player player) {
            validateHost(player);
            stateRef.get().requireStart(player);
        }

        public void ready(Player player) {
            stateRef.get().ready(player);
        }

        public void cancelReady(Player player) {
            stateRef.get().cancelReady(player);
        }

        public void abort(Player player) {
            stateRef.get().abort(player);
        }

        public void played(Player player, int recordId) {
            stateRef.get().played(player, recordId);
        }
    }

    public RoomSnapshot getView() {
        synchronized (lifecycleLock) {
            return new RoomSnapshot(
                    roomId,
                    stateRef.get(),
                    setting.live,
                    setting.locked,
                    setting.cycle,
                    setting.host ? playerManager.host.getId() : null,
                    playerManager.getPlayersCopy(),
                    playerManager.getMonitorsCopy()
            );
        }
    }

    private void destroyRoom() {
        RoomDestroyEvent event = new RoomDestroyEvent(
                this,
                playerManager.getPlayersCopy(),
                playerManager.getMonitorsCopy()
        );
        Server.postEvent(event);
        onDestroy.run();
    }
}