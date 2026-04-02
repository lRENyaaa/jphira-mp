package top.rymc.phira.main.game.room;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.event.operation.RoomAdminDestroyEvent;
import top.rymc.phira.main.event.operation.RoomAdminForceStartEvent;
import top.rymc.phira.main.event.operation.RoomAdminPostSelectChartEvent;
import top.rymc.phira.main.event.operation.RoomAdminPreSelectChartEvent;
import top.rymc.phira.main.event.operation.RoomAdminRequireStartEvent;
import top.rymc.phira.main.event.room.PlayerLeaveRoomEvent;
import top.rymc.phira.main.event.room.RoomStateChangeEvent;
import top.rymc.phira.main.event.operation.RoomPostSelectChartEvent;
import top.rymc.phira.main.event.operation.RoomPreSelectChartEvent;
import top.rymc.phira.main.event.room.RoomDestroyEvent;
import top.rymc.phira.main.event.room.RoomHostChangeEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.i18n.I18nService;
import top.rymc.phira.main.game.player.LocalPlayer;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.state.RoomGameState;
import top.rymc.phira.main.game.state.RoomPlaying;
import top.rymc.phira.main.game.state.RoomSelectChart;
import top.rymc.phira.main.game.state.RoomWaitForReady;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.main.util.PhiraFetcher;

import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJoinRoomPacket;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntFunction;

@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Room {
    @Getter private final String roomId;
    private final Consumer<Room> onDestroy;

    private final Set<Player> players = ConcurrentHashMap.newKeySet();
    private final Set<Player> monitors = ConcurrentHashMap.newKeySet();
    @Getter private Player host;

    private volatile RoomGameState state;
    @Getter
    private final RoomSetting setting;

    private final Object stateLock = new Object();

    @Getter
    @Setter(AccessLevel.PRIVATE)
    public static class RoomSetting {
        private boolean autoDestroy = true;
        private boolean host = true;
        private int maxPlayer = 8;
        private boolean locked = false;
        private boolean cycle = false;
        private boolean live = false;
        private boolean chat = true;

        public RoomSetting() {}

        public RoomSetting(boolean autoDestroy, boolean host, int maxPlayer, boolean locked, boolean cycle, boolean live, boolean chat) {
            this.autoDestroy = autoDestroy;
            this.host = host;
            this.maxPlayer = maxPlayer;
            this.locked = locked;
            this.cycle = cycle;
            this.live = live;
            this.chat = chat;
        }
    }

    public static Room create(String roomId, Consumer<Room> onDestroy, RoomSetting setting) {
        Room room = new Room(roomId, onDestroy, setting);
        room.state = new RoomSelectChart(room, room::updateState);
        return room;
    }

    public static Room create(String roomId, Consumer<Room> onDestroy, LocalPlayer hostPlayer) {
        Room room = new Room(roomId, onDestroy, new RoomSetting());
        room.state = new RoomSelectChart(room, room::updateState);
        room.join(hostPlayer, false);
        room.host = hostPlayer;
        return room;
    }

    public static Room create(String roomId, Consumer<Room> onDestroy, LocalPlayer hostPlayer, RoomSetting setting) {
        Room room = new Room(roomId, onDestroy, setting);
        room.state = new RoomSelectChart(room, room::updateState);
        room.join(hostPlayer, false);
        room.host = hostPlayer;
        return room;
    }

    public void join(Player player, boolean isMonitor) {
        synchronized (stateLock) {
            if (players.size() >= setting.maxPlayer) throw GameOperationException.roomFull();
            if (setting.locked && !players.isEmpty()) throw GameOperationException.roomLocked();

            boolean isInit = players.isEmpty();
            Set<Player> set = isMonitor ? monitors : players;
            set.add(player);

            if (isInit) {
                if (setting.host) {
                    host = player;
                }
            } else {
                broadcast(op -> op.memberJoined(player.getId(), player.getName(), isMonitor));
            }

            state.handleJoin(player);
        }
    }

    public void leave(LocalPlayer player) {
        synchronized (stateLock) {
            if (!players.remove(player) && !monitors.remove(player)) return;

            broadcast(op -> op.memberLeft(player.getId(), player.getName()));
            state.handleLeave(player);

            PlayerLeaveRoomEvent event = new PlayerLeaveRoomEvent(player, this);
            Server.postEvent(event);

            if (players.isEmpty() && monitors.isEmpty()) {
                if (setting.autoDestroy) {
                    destroyRoom();
                }
            } else if (setting.host && player.equals(host)) {
                transferHostToNextPlayer();
            }
        }
    }

    private void destroyRoom() {
        RoomDestroyEvent event = new RoomDestroyEvent(this, Set.copyOf(players), Set.copyOf(monitors));
        Server.postEvent(event);
        onDestroy.accept(this);
    }

    private void transferHostToNextPlayer() {
        Player previousHost = host;

        for (Player player : players) {
            if (player.equals(previousHost)) {
                continue;
            }

            Optional<PlayerOperations> operations = player.operations();
            if (operations.isEmpty()) {
                continue;
            }

            host = player;
            operations.get().updateHostStatus(true);

            RoomHostChangeEvent event = new RoomHostChangeEvent(this, previousHost, player);
            Server.postEvent(event);
            return;
        }
    }

    public boolean containsPlayer(LocalPlayer player) {
        return players.contains(player);
    }

    public boolean isInRoom(LocalPlayer player) {
        return players.contains(player) || monitors.contains(player);
    }

    public boolean containsMonitor(LocalPlayer player) {
        return monitors.contains(player);
    }

    private void broadcast(Consumer<PlayerOperations> action) {
        players.forEach(p -> p.operations().ifPresent(action));
        monitors.forEach(p -> p.operations().ifPresent(action));
    }

    private void broadcastToMonitors(Consumer<PlayerOperations> action) {
        monitors.forEach(p -> p.operations().ifPresent(action));
    }

    @Getter
    private final Operation operation = new Operation();

    public class Operation {

        private void validateHost(LocalPlayer player) {
            if (!isHost(player)) {
                throw GameOperationException.permissionDenied();
            }
        }

        public void lockRoom(LocalPlayer player) {
            validateHost(player);

            setting.locked = !setting.locked;

            broadcast(op -> op.lockRoom(setting.locked));
        }

        public void cycleRoom(LocalPlayer player) {
            validateHost(player);

            setting.cycle = !setting.cycle;

            broadcast(op -> op.cycleRoom(setting.cycle));
        }

        public void selectChart(LocalPlayer player, int id) {
            validateHost(player);

            if (!(state instanceof RoomSelectChart)) {
                throw GameOperationException.invalidState();
            }

            RoomPreSelectChartEvent preEvent = new RoomPreSelectChartEvent(Room.this, player, id);
            Server.postEvent(preEvent);
            if (preEvent.isCancelled()) {
                throw new GameOperationException(preEvent.getCancelReason());
            }

            IntFunction<ChartInfo> getInfoFunc = PhiraFetcher.GET_CHART_INFO.toIntFunction(e -> {
                throw GameOperationException.chartNotFound();
            });

            ChartInfo eventChartInfo = preEvent.getChartInfo();

            ChartInfo info = eventChartInfo != null ? eventChartInfo : getInfoFunc.apply(id);

            state.setChart(info);
            broadcast(operations -> operations.selectChart(info.getId(), info.getName(), player.getId()));

            RoomPostSelectChartEvent postEvent = new RoomPostSelectChartEvent(Room.this, player, info);
            Server.postEvent(postEvent);
        }

        public void chat(LocalPlayer player, String message) {
            if (!setting.chat) {
                throw GameOperationException.chatNotEnabled();
            }

            broadcast(operations -> operations.receiveChat(player.getId(), message));
        }

        public void touchSend(LocalPlayer player, List<TouchFrame> touchFrames) {
            state.touchSend(player, touchFrames);
            broadcastToMonitors(operations -> operations.receiveTouchStream(player.getId(), touchFrames));
        }

        public void judgeSend(LocalPlayer player, List<JudgeEvent> judgeEvents) {
            state.judgeSend(player, judgeEvents);
            broadcastToMonitors(operations -> operations.receiveJudgeStream(player.getId(), judgeEvents));
        }

        public void requireStart(LocalPlayer player){
            validateHost(player);
            state.requireStart(player);
        }

        public void ready(LocalPlayer player){
            state.ready(player);
        }

        public void cancelReady(LocalPlayer player) {
            state.cancelReady(player);
        }

        public void abort(LocalPlayer player) {
            state.abort(player);
        }

        public void played(LocalPlayer player, int recordId) {
            state.played(player, recordId);
        }

    }

    @Getter
    private final AdminOperation adminOperation = new AdminOperation();

    public class AdminOperation {

        public void selectChart(int id) {
            if (!(state instanceof RoomSelectChart)) {
                throw GameOperationException.invalidState();
            }

            RoomAdminPreSelectChartEvent preEvent = new RoomAdminPreSelectChartEvent(Room.this, id);
            Server.postEvent(preEvent);
            String cancelReason = preEvent.getCancelReason();
            if (cancelReason != null) {
                throw new GameOperationException(cancelReason);
            }

            IntFunction<ChartInfo> getInfoFunc = PhiraFetcher.GET_CHART_INFO.toIntFunction(e -> {
                throw GameOperationException.chartNotFound();
            });

            ChartInfo eventChartInfo = preEvent.getChartInfo();

            ChartInfo info = eventChartInfo != null ? eventChartInfo : getInfoFunc.apply(id);
            state.setChart(info);
            broadcast(operations -> operations.selectChart(info.getId(), info.getName(), -1));

            RoomAdminPostSelectChartEvent postEvent = new RoomAdminPostSelectChartEvent(Room.this, info);
            Server.postEvent(postEvent);
        }

        public void requireStart(){
            if (!(state instanceof RoomSelectChart)) {
                throw GameOperationException.invalidState();
            }

            ChartInfo chart = state.getChart();
            if (chart == null) {
                throw GameOperationException.chartNotSelected();
            }

            RoomAdminRequireStartEvent event = new RoomAdminRequireStartEvent(Room.this);
            Server.postEvent(event);
            String cancelReason = event.getCancelReason();
            if (cancelReason != null) {
                throw new GameOperationException(cancelReason);
            }

            updateState(new RoomWaitForReady(Room.this, Room.this::updateState, chart));
            broadcast(operations -> operations.gameRequireStart(-1));
        }

        public void forceStart(){
            ChartInfo chart = state.getChart();
            if (chart == null) {
                throw GameOperationException.chartNotSelected();
            }

            RoomAdminForceStartEvent event = new RoomAdminForceStartEvent(Room.this);
            Server.postEvent(event);
            String cancelReason = event.getCancelReason();
            if (cancelReason != null) {
                throw new GameOperationException(cancelReason);
            }

            updateState(new RoomPlaying(Room.this, Room.this::updateState, state.getChart()));
            broadcast(PlayerOperations::gameStartPlaying);
        }

        public boolean destroy() {
            if (!players.isEmpty() || !monitors.isEmpty()) {
                return false;
            }

            RoomAdminDestroyEvent event = new RoomAdminDestroyEvent(Room.this);
            Server.postEvent(event);
            String cancelReason = event.getCancelReason();
            if (cancelReason != null) {
                throw new GameOperationException(cancelReason);
            }

            onDestroy.accept(Room.this);
            return true;
        }
    }

    private void updateState(RoomGameState newState) {
        synchronized (stateLock) {
            RoomGameState oldState = this.state;
            this.state = newState;
            Server.postEvent(new RoomStateChangeEvent(this, oldState, newState));
            broadcast(operations -> operations.enterState(newState.toProtocol()));
        }
    }

    public boolean isHost(Player player) {
        return setting.host && player.getId() == host.getId();
    }

    public ProtocolConvertible<RoomInfo> asProtocolConvertible(Player viewer) {
        return () -> new RoomInfo(
                roomId,
                state.toProtocol(),
                setting.live, setting.locked, setting.cycle,
                isHost(viewer),
                state instanceof RoomWaitForReady,
                players.stream().map(Player::toProtocol).toList(),
                monitors.stream().map(Player::toProtocol).toList()
        );
    }


    @Getter
    private final ProtocolHack protocolHack = new ProtocolHack();

    public class ProtocolHack {
        public ClientBoundJoinRoomPacket buildJoinSuccessPacket() {

            GameState protocolState;
            ChartInfo chart = state.getChart();
            if (!(state instanceof RoomSelectChart) && chart != null) {
                protocolState = new SelectChart(chart.getId());
            } else {
                protocolState = state.toProtocol();
            }

            return ClientBoundJoinRoomPacket.success(
                    protocolState,
                    players.stream().map(Player::toProtocol).toList(),
                    monitors.stream().map(Player::toProtocol).toList(),
                    setting.live
            );
        }

        private static final Executor executor = CompletableFuture.delayedExecutor(2, TimeUnit.MILLISECONDS);

        public void forceSyncHost(LocalPlayer player, boolean delay) {
            if (!isInRoom(player)) return;

            Runnable task = () -> player.operations().ifPresent(operations -> operations.updateHostStatus(isHost(player)));

            runTask(task, delay);
        }

        public void forceSyncInfo(LocalPlayer player, boolean delay) {
            if (!isInRoom(player)) return;

            Runnable task = () -> {
                if (!isHost(player)) {
                    player.operations().ifPresent(operations -> operations.updateHostStatus(false));
                }

                if (setting.live) {
                    String name = I18nService.INSTANCE.getMessage(player, "system.live_recorder_name");
                    player.operations().ifPresent(operations -> {
                        operations.memberJoined(-1, name, true);
                        operations.memberLeft(-1, name);
                    });
                }

                if (!(state instanceof RoomSelectChart && state.getChart() == null)) {
                    fixClientRoomState0(player);
                }
            };

            runTask(task, delay);
        }

        public void fixClientRoomState(LocalPlayer player) {
            if (!isInRoom(player)) return;

            if (!(state instanceof RoomSelectChart) && state.getChart() != null) {
                fixClientRoomState0(player);
            }
        }

        private void fixClientRoomState0(LocalPlayer player) {
            ChartInfo chart = state.getChart();
            if (chart != null) {
                player.operations().ifPresent(operations -> operations.enterState(new SelectChart(chart.getId())));
            }

            if (state instanceof RoomSelectChart) {
                return;
            }

            runTask(() -> player.operations().ifPresent(operations -> operations.enterState(state.toProtocol())), true);
        }

        private void runTask(Runnable task, boolean delay) {
            if (delay) {
                executor.execute(task);
            } else {
                task.run();
            }
        }
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public Set<Player> getMonitors() {
        return Collections.unmodifiableSet(monitors);
    }

}
