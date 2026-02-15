package top.rymc.phira.main.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.event.PlayerLeaveRoomEvent;
import top.rymc.phira.main.event.RoomStateChangeEvent;
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
        room.state = new RoomSelectChart(room::updateState);
        return room;
    }

    public static Room create(String roomId, Consumer<Room> onDestroy, Player hostPlayer) {
        Room room = new Room(roomId, onDestroy, new RoomSetting());
        room.state = new RoomSelectChart(room::updateState);
        room.join(hostPlayer, false);
        room.host = hostPlayer;
        return room;
    }

    public synchronized void join(Player player, boolean isMonitor) {
        if (players.size() >= setting.maxPlayer) throw new IllegalStateException("Room is full");
        if (setting.locked && !players.isEmpty()) throw new IllegalStateException("Room is locked");

        boolean isInit = players.isEmpty();
        Set<Player> set = isMonitor ? monitors : players;
        set.add(player);

        if (isInit) {
            if (setting.host) {
                host = player;
            }
        } else {
            broadcast(ClientBoundOnJoinRoomPacket.create(player.toProtocol(), isMonitor));
            broadcast(ClientBoundMessagePacket.create(new JoinRoomMessage(player.getId(), player.getName())));
        }

        handleJoin(player);
    }

    public synchronized void leave(Player player) {
        if (!players.remove(player) && !monitors.remove(player)) return;

        broadcast(ClientBoundMessagePacket.create(new LeaveRoomMessage(player.getId(), player.getName())));
        handleLeave(player);

        PlayerLeaveRoomEvent event = new PlayerLeaveRoomEvent(player, this);
        Server.postEvent(event);

        if (players.isEmpty() && monitors.isEmpty()) {
            if (setting.autoDestroy) {
                onDestroy.accept(this);
            }
        } else if (setting.host && player.equals(host)) {
            host = players.iterator().next();
            host.getConnection().send(ClientBoundChangeHostPacket.create(true));
        }
    }

    public boolean containsPlayer(Player player) {
        return players.contains(player);
    }

    public boolean isInRoom(Player player) {
        return players.contains(player) || monitors.contains(player);
    }

    public boolean containsMonitor(Player player) {
        return monitors.contains(player);
    }

    public void handleJoin(Player player) { state.handleJoin(player); }
    public void handleLeave(Player player) { state.handleLeave(player); }
    @Getter
    private final Operation operation = new Operation();

    public class Operation {

        public void lockRoom(Player player) {
            if (!isHost(player)) {
                throw new IllegalStateException("你没有权限");
            }

            setting.locked = !setting.locked;

            broadcast(ClientBoundMessagePacket.create(new LockRoomMessage(setting.locked)));

        }

        public void cycleRoom(Player player) {
            if (!isHost(player)) {
                throw new IllegalStateException("你没有权限");
            }

            setting.cycle = !setting.cycle;

            broadcast(ClientBoundMessagePacket.create(new CycleRoomMessage(setting.cycle)));

        }

        public void selectChart(Player player, int id) {
            if (!isHost(player)) {
                throw new IllegalStateException("你没有权限");
            }

            if (!(state instanceof RoomSelectChart)) {
                throw new IllegalStateException("房间不在选择谱面状态");
            }

            ChartInfo info = PhiraFetcher.GET_CHART_INFO.toIntFunction(e -> {
                throw new IllegalStateException("谱面信息获取失败");
            }).apply(id);

            state.setChart(info);
            broadcast(ClientBoundMessagePacket.create(new SelectChartMessage(player.getId(), info.getName(), id)));
            broadcast(ClientBoundChangeStatePacket.create(state.toProtocol()));
        }

        public void chat(Player player, String message) {
            if (!setting.chat) {
                throw new IllegalStateException("房间未启用聊天");
            }

            broadcast(ClientBoundMessagePacket.create(new ChatMessage(player.getId(), message)));
        }

        public void touchSend(Player player, List<TouchFrame> touchFrames) {
            ClientBoundTouchesPacket packet = ClientBoundTouchesPacket.create(player.getId(), touchFrames);
            monitors.forEach(p -> p.getConnection().send(packet));
        }

        public void judgeSend(Player player, List<JudgeEvent> judgeEvents) {
            ClientBoundJudgesPacket packet = ClientBoundJudgesPacket.create(player.getId(), judgeEvents);
            monitors.forEach(p -> p.getConnection().send(packet));
        }

        public void requireStart(Player player){
            if (!isHost(player)) {
                throw new IllegalStateException("你没有权限");
            }

            state.requireStart(player, players, monitors);
        }

        public void ready(Player player){
            state.ready(player, players, monitors);
        }

        public void cancelReady(Player player) {
            state.cancelReady(player, players, monitors);
        }

        public void abort(Player player) {
            state.abort(player, players, monitors);
        }

        public void played(Player player, int recordId) {
            state.played(player, recordId, players, monitors);
        }

    }

    @Getter
    private final AdminOperation adminOperation = new AdminOperation();

    public class AdminOperation {

        public void selectChart(int id) {
            if (!(state instanceof RoomSelectChart)) {
                throw new IllegalStateException("房间不在选择谱面状态");
            }

            ChartInfo info = PhiraFetcher.GET_CHART_INFO.toIntFunction(e -> {
                throw new IllegalStateException("谱面信息获取失败");
            }).apply(id);

            state.setChart(info);
            broadcast(ClientBoundMessagePacket.create(new SelectChartMessage(-1, info.getName(), id)));
            broadcast(ClientBoundChangeStatePacket.create(state.toProtocol()));
        }

        public void requireStart(){
            if (!(state instanceof RoomSelectChart)) {
                throw new IllegalStateException("房间不在选择谱面状态");
            }

            ChartInfo chart = state.getChart();
            if (chart == null) {
                throw new IllegalStateException("未选择谱面");
            }
            updateState(new RoomWaitForReady(Room.this::updateState, chart));
            broadcast(ClientBoundMessagePacket.create(new GameStartMessage(-1)));
        }

        public void forceStart(){
            ChartInfo chart = state.getChart();
            if (chart == null) {
                throw new IllegalStateException("未选择谱面");
            }

            updateState(new RoomPlaying(Room.this::updateState, state.getChart()));
            broadcast(ClientBoundMessagePacket.create(StartPlayingMessage.INSTANCE));
        }

        public boolean destroy() {
            if (!players.isEmpty() || !monitors.isEmpty()) {
                return false;
            }

            onDestroy.accept(Room.this);
            return true;
        }
    }

    private void updateState(RoomGameState newState) {
        RoomStateChangeEvent event = new RoomStateChangeEvent(this, this.state, newState);
        Server.postEvent(event);
        
        this.state = newState;
        broadcast(ClientBoundChangeStatePacket.create(newState.toProtocol()));
    }

    public void broadcast(ClientBoundPacket packet) {
        Consumer<Player> broadcastProcessor = p -> {
            if (p.isOnline()) p.getConnection().send(packet);
        };

        players.forEach(broadcastProcessor);
        monitors.forEach(broadcastProcessor);
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

        private static final Executor executor = CompletableFuture.delayedExecutor(1, TimeUnit.MILLISECONDS);

        public void forceSyncHost(Player player) {
            if (!isInRoom(player)) return; // TODO: Throw exception

            PlayerConnection connection = player.getConnection();

            executor.execute(() -> {
                connection.send(ClientBoundChangeHostPacket.create(isHost(player)));
            });
        }

        public void forceSyncInfo(Player player) {
            if (!isInRoom(player)) return; // TODO: Throw exception

            PlayerConnection connection = player.getConnection();

            executor.execute(() -> {
                if (!isHost(player)) {
                    connection.send(ClientBoundChangeHostPacket.create(false));
                }

                if (setting.live) {
                    String name = "录制状态设置器(请忽略该账号)";
                    connection.send(ClientBoundOnJoinRoomPacket.create(new FullUserProfile(-1, name, true)));
                    connection.send(ClientBoundMessagePacket.create(new JoinRoomMessage(-1, name)));
                    connection.send(ClientBoundMessagePacket.create(new LeaveRoomMessage(-1, name)));
                }

                if (!(state instanceof RoomSelectChart && state.getChart() == null)) {
                    fixClientRoomState0(player);
                }
            });
        }

        public void fixClientRoomState(Player player) {
            if (!isInRoom(player)) return; // TODO: Throw exception

            if (!(state instanceof RoomSelectChart) && state.getChart() != null) {
                fixClientRoomState0(player);
            }
        }

        private void fixClientRoomState0(Player player) {
            PlayerConnection connection = player.getConnection();
            ChartInfo chart = state.getChart();
            if (chart != null) {
                connection.send(ClientBoundChangeStatePacket.create(new SelectChart(chart.getId())));
            }

            if (state instanceof RoomSelectChart) {
                return;
            }

            executor.execute(() -> connection.send(ClientBoundChangeStatePacket.create(state.toProtocol())));
        }
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public Set<Player> getMonitors() {
        return Collections.unmodifiableSet(monitors);
    }

}