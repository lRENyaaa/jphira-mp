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
import top.rymc.phira.main.game.state.RoomGameState;
import top.rymc.phira.main.game.state.RoomPlaying;
import top.rymc.phira.main.game.state.RoomSelectChart;
import top.rymc.phira.main.game.state.RoomWaitForReady;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.message.ChatMessage;
import top.rymc.phira.protocol.data.message.CycleRoomMessage;
import top.rymc.phira.protocol.data.message.GameStartMessage;
import top.rymc.phira.protocol.data.message.JoinRoomMessage;
import top.rymc.phira.protocol.data.message.LeaveRoomMessage;
import top.rymc.phira.protocol.data.message.LockRoomMessage;
import top.rymc.phira.protocol.data.message.SelectChartMessage;
import top.rymc.phira.protocol.data.message.StartPlayingMessage;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeHostPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeStatePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJoinRoomPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJudgesPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundOnJoinRoomPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundTouchesPacket;

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

    private final Set<LocalPlayer> players = ConcurrentHashMap.newKeySet();
    private final Set<LocalPlayer> monitors = ConcurrentHashMap.newKeySet();
    @Getter private LocalPlayer host;

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

    public void join(LocalPlayer player, boolean isMonitor) {
        synchronized (stateLock) {
            if (players.size() >= setting.maxPlayer) throw GameOperationException.roomFull();
            if (setting.locked && !players.isEmpty()) throw GameOperationException.roomLocked();

            boolean isInit = players.isEmpty();
            Set<LocalPlayer> set = isMonitor ? monitors : players;
            set.add(player);

            if (isInit) {
                if (setting.host) {
                    host = player;
                }
            } else {
                broadcast(ClientBoundOnJoinRoomPacket.create(player.toProtocol(), isMonitor));
                broadcast(ClientBoundMessagePacket.create(new JoinRoomMessage(player.getId(), player.getName())));
            }

            state.handleJoin(player);
        }
    }

    public void leave(LocalPlayer player) {
        synchronized (stateLock) {
            if (!players.remove(player) && !monitors.remove(player)) return;

            broadcast(ClientBoundMessagePacket.create(new LeaveRoomMessage(player.getId(), player.getName())));
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
        LocalPlayer previousHost = host;
        LocalPlayer nextHost = players.iterator().next();
        if (nextHost != null && nextHost.isOnline()) {
            host = nextHost;
            host.getConnection().send(ClientBoundChangeHostPacket.create(true));

            RoomHostChangeEvent event = new RoomHostChangeEvent(this, previousHost, nextHost);
            Server.postEvent(event);
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

            broadcast(ClientBoundMessagePacket.create(new LockRoomMessage(setting.locked)));

        }

        public void cycleRoom(LocalPlayer player) {
            validateHost(player);

            setting.cycle = !setting.cycle;

            broadcast(ClientBoundMessagePacket.create(new CycleRoomMessage(setting.cycle)));

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
            broadcast(ClientBoundMessagePacket.create(new SelectChartMessage(player.getId(), info.getName(), id)));
            broadcast(ClientBoundChangeStatePacket.create(state.toProtocol()));

            RoomPostSelectChartEvent postEvent = new RoomPostSelectChartEvent(Room.this, player, info);
            Server.postEvent(postEvent);
        }

        public void chat(LocalPlayer player, String message) {
            if (!setting.chat) {
                throw GameOperationException.chatNotEnabled();
            }

            broadcast(ClientBoundMessagePacket.create(new ChatMessage(player.getId(), message)));
        }

        public void touchSend(LocalPlayer player, List<TouchFrame> touchFrames) {
            state.touchSend(player, touchFrames);
            ClientBoundTouchesPacket packet = ClientBoundTouchesPacket.create(player.getId(), touchFrames);
            monitors.forEach(p -> p.getConnection().send(packet));
        }

        public void judgeSend(LocalPlayer player, List<JudgeEvent> judgeEvents) {
            state.judgeSend(player, judgeEvents);
            ClientBoundJudgesPacket packet = ClientBoundJudgesPacket.create(player.getId(), judgeEvents);
            monitors.forEach(p -> p.getConnection().send(packet));
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
            broadcast(ClientBoundMessagePacket.create(new SelectChartMessage(-1, info.getName(), info.getId())));
            broadcast(ClientBoundChangeStatePacket.create(state.toProtocol()));

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
            broadcast(ClientBoundMessagePacket.create(new GameStartMessage(-1)));
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
            broadcast(ClientBoundMessagePacket.create(StartPlayingMessage.INSTANCE));
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
            broadcast(ClientBoundChangeStatePacket.create(newState.toProtocol()));
        }
    }

    public void broadcast(ClientBoundPacket packet) {
        Consumer<LocalPlayer> broadcastProcessor = p -> {
            if (p.isOnline()) p.getConnection().send(packet);
        };

        players.forEach(broadcastProcessor);
        monitors.forEach(broadcastProcessor);
    }

    public boolean isHost(LocalPlayer player) {
        return setting.host && player.getId() == host.getId();
    }

    public ProtocolConvertible<RoomInfo> asProtocolConvertible(LocalPlayer viewer) {
        return () -> new RoomInfo(
                roomId,
                state.toProtocol(),
                setting.live, setting.locked, setting.cycle,
                isHost(viewer),
                state instanceof RoomWaitForReady,
                players.stream().map(LocalPlayer::toProtocol).toList(),
                monitors.stream().map(LocalPlayer::toProtocol).toList()
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
                    players.stream().map(LocalPlayer::toProtocol).toList(),
                    monitors.stream().map(LocalPlayer::toProtocol).toList(),
                    setting.live
            );
        }

        private static final Executor executor = CompletableFuture.delayedExecutor(2, TimeUnit.MILLISECONDS);

        public void forceSyncHost(LocalPlayer player, boolean delay) {
            if (!isInRoom(player)) return;

            PlayerConnection connection = player.getConnection();

            Runnable task = () -> connection.send(ClientBoundChangeHostPacket.create(isHost(player)));

            runTask(task, delay);

        }

        public void forceSyncInfo(LocalPlayer player, boolean delay) {
            if (!isInRoom(player)) return;

            PlayerConnection connection = player.getConnection();

            Runnable task = () -> {
                if (!isHost(player)) {
                    connection.send(ClientBoundChangeHostPacket.create(false));
                }

                if (setting.live) {
                    String name = I18nService.INSTANCE.getMessage(player, "system.live_recorder_name");
                    connection.send(ClientBoundOnJoinRoomPacket.create(new FullUserProfile(-1, name, true)));
                    connection.send(ClientBoundMessagePacket.create(new JoinRoomMessage(-1, name)));
                    connection.send(ClientBoundMessagePacket.create(new LeaveRoomMessage(-1, name)));
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
            PlayerConnection connection = player.getConnection();
            ChartInfo chart = state.getChart();
            if (chart != null) {
                connection.send(ClientBoundChangeStatePacket.create(new SelectChart(chart.getId())));
            }

            if (state instanceof RoomSelectChart) {
                return;
            }

            runTask(() -> connection.send(ClientBoundChangeStatePacket.create(state.toProtocol())), true);
        }

        private void runTask(Runnable task, boolean delay) {
            if (delay) {
                executor.execute(task);
            } else {
                task.run();
            }
        }
    }

    public Set<LocalPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public Set<LocalPlayer> getMonitors() {
        return Collections.unmodifiableSet(monitors);
    }

}
