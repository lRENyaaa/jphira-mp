package top.rymc.phira.main.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.state.RoomGameState;
import top.rymc.phira.main.game.state.RoomPlaying;
import top.rymc.phira.main.game.state.RoomSelectChart;
import top.rymc.phira.main.game.state.RoomWaitForReady;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.main.redis.PubSubEvent;
import top.rymc.phira.main.redis.RedisHolder;
import top.rymc.phira.main.redis.RoomState;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.message.ChatMessage;
import top.rymc.phira.protocol.data.message.CycleRoomMessage;
import top.rymc.phira.protocol.data.message.JoinRoomMessage;
import top.rymc.phira.protocol.data.message.LeaveRoomMessage;
import top.rymc.phira.protocol.data.message.LockRoomMessage;
import top.rymc.phira.protocol.data.message.SelectChartMessage;
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
    private final RoomSetting setting = new RoomSetting();

    @Getter
    @Setter(AccessLevel.PRIVATE)
    public static class RoomSetting {
        private boolean autoDestroy = true;
        private boolean host = true;
        private int maxPlayer = 8;
        private boolean locked = false;
        private boolean cycle = false;
        private boolean live = true;
        private boolean chat = true;
    }

    public static Room create(String roomId, Consumer<Room> onDestroy, Player hostPlayer) {
        Room room = new Room(roomId, onDestroy);
        room.state = new RoomSelectChart(room::updateState);
        room.join(hostPlayer, false);
        room.host = hostPlayer;
        return room;
    }

    /** 从 Redis 恢复的本地房间视图（跨服加入时用），不调用 join，由调用方随后 join */
    public static Room createFromRedis(String roomId, Consumer<Room> onDestroy,
                                       Map<String, String> redisInfo, Player localHostPlaceholder) {
        Room room = new Room(roomId, onDestroy);
        if (redisInfo != null) {
            room.setting.setLocked("true".equalsIgnoreCase(redisInfo.get("is_locked")));
            room.setting.setCycle("true".equalsIgnoreCase(redisInfo.get("is_cycle")));
            room.initStateFromRedis(redisInfo.get("state"), redisInfo.get("chart_id"));
        } else {
            room.state = new RoomSelectChart(room::updateState);
        }
        room.host = localHostPlaceholder;
        return room;
    }

    void initStateFromRedis(String stateCode, String chartIdStr) {
        int code = stateCode != null && !stateCode.isEmpty() ? Integer.parseInt(stateCode, 10) : 0;
        int chartId = 0;
        if (chartIdStr != null && !chartIdStr.isEmpty()) {
            try {
                chartId = Integer.parseInt(chartIdStr, 10);
            } catch (NumberFormatException ignored) {}
        }
        ChartInfo chart = chartId > 0 ? PhiraFetcher.GET_CHART_INFO.toIntFunction(e -> null).apply(chartId) : null;
        state = switch (code) {
            case 1 -> new RoomWaitForReady(stateUpdater());
        case 2 -> new RoomPlaying(stateUpdater());
        default -> new RoomSelectChart(stateUpdater());
        };
        if (state != null && chart != null) state.setChart(chart);
    }

    private Consumer<RoomGameState> stateUpdater() {
        return this::updateState;
    }

    // 玩家加入（线程安全）
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
            broadcast(new ClientBoundOnJoinRoomPacket(player.toProtocol(), isMonitor));
            broadcast(new ClientBoundMessagePacket(new JoinRoomMessage(player.getId(), player.getName())));
        }

        handleJoin(player);
    }

    public synchronized void leave(Player player) {
        boolean wasMonitor = monitors.contains(player);
        if (!players.remove(player) && !monitors.remove(player)) return;

        broadcast(new ClientBoundMessagePacket(new LeaveRoomMessage(player.getId(), player.getName())));
        handleLeave(player);

        boolean hostChanged = setting.host && player.equals(host);
        Player newHost = null;
        if (hostChanged && !players.isEmpty()) {
            newHost = players.iterator().next();
            host = newHost;
            host.getConnection().send(new ClientBoundChangeHostPacket(true));
        }

        var redis = RedisHolder.get();
        redis.removeRoomPlayer(roomId, player.getId());
        redis.setPlayerSession(player.getId(), redis.getServerId(), "0", player.getName(), wasMonitor);
        redis.publishEvent(PubSubEvent.PLAYER_LEAVE, roomId,
                Map.of("uid", player.getId(), "is_host_changed", hostChanged,
                        "new_host", newHost != null ? newHost.getId() : 0));
        if (hostChanged && newHost != null) {
            redis.setRoomInfo(roomId, newHost.getId(), RoomState.fromCode(stateToRedisCode()), chartIdFromState(), setting.locked, setting.cycle);
        }

        if (players.isEmpty() && monitors.isEmpty()) {
            if (setting.autoDestroy) {
                onDestroy.accept(this);
            }
        }
    }

    private int stateToRedisCode() {
        if (state instanceof RoomSelectChart) return 0;
        if (state instanceof RoomWaitForReady) return 1;
        if (state instanceof RoomPlaying) return 2;
        return 0;
    }

    private Integer chartIdFromState() {
        ChartInfo c = state == null ? null : state.getChart();
        return c != null ? c.getId() : null;
    }

    // 断线重连专用：验证玩家是否在此房间，monitor不支持断线重连
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

            broadcast(new ClientBoundMessagePacket(new LockRoomMessage(setting.locked)));

        }

        public void cycleRoom(Player player) {
            if (!isHost(player)) {
                throw new IllegalStateException("你没有权限");
            }

            setting.cycle = !setting.cycle;

            broadcast(new ClientBoundMessagePacket(new CycleRoomMessage(setting.cycle)));

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
            broadcast(new ClientBoundMessagePacket(new SelectChartMessage(player.getId(), info.getName(), id)));
            broadcast(new ClientBoundChangeStatePacket(state.toProtocol()));
        }

        public void chat(Player player, String message) {
            if (!setting.chat) {
                throw new IllegalStateException("房间未启用聊天");
            }

            broadcast(new ClientBoundMessagePacket(new ChatMessage(player.getId(), message)));
        }

        public void touchSend(Player player, List<TouchFrame> touchFrames) {
            ClientBoundTouchesPacket packet = new ClientBoundTouchesPacket(player.getId(), touchFrames);
            monitors.forEach(p -> p.getConnection().send(packet));
        }

        public void judgeSend(Player player, List<JudgeEvent> judgeEvents) {
            ClientBoundJudgesPacket packet = new ClientBoundJudgesPacket(player.getId(), judgeEvents);
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

    private void updateState(RoomGameState newState) {
        this.state = newState;
        broadcast(new ClientBoundChangeStatePacket(newState.toProtocol()));
        var redis = RedisHolder.get();
        redis.setRoomStateAndChart(roomId, RoomState.fromCode(stateToRedisCode()), chartIdFromState());
        redis.publishEvent(PubSubEvent.STATE_CHANGE, roomId,
                Map.of("new_state", stateToRedisCode(), "chart_id", chartIdFromState() != null ? chartIdFromState() : 0));
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
        public ClientBoundJoinRoomPacket.Success buildJoinSuccessPacket() {

            GameState protocolState;
            ChartInfo chart = state.getChart();
            if (!(state instanceof RoomSelectChart) && chart != null) {
                protocolState = new SelectChart(chart.getId());
            } else {
                protocolState = state.toProtocol();
            }

            return new ClientBoundJoinRoomPacket.Success(
                    protocolState,
                    players.stream().map(Player::toProtocol).toList(),
                    monitors.stream().map(Player::toProtocol).toList(),
                    setting.live
            );
        }

        private static final Executor executor = CompletableFuture.delayedExecutor(1, TimeUnit.MILLISECONDS);

        public void forceSyncInfo(Player player) {
            if (!isInRoom(player)) return; // TODO: Throw exception

            PlayerConnection connection = player.getConnection();

            executor.execute(() -> {
                if (!isHost(player)) {
                    connection.send(new ClientBoundChangeHostPacket(false));
                }

                if (setting.live) {
                    String name = "录制状态设置器(请忽略该账号)";
                    connection.send(new ClientBoundOnJoinRoomPacket(new FullUserProfile(-1, name, true)));
                    connection.send(new ClientBoundMessagePacket(new JoinRoomMessage(-1, name)));
                    connection.send(new ClientBoundMessagePacket(new LeaveRoomMessage(-1, name)));
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
                connection.send(new ClientBoundChangeStatePacket(new SelectChart(chart.getId())));
            }

            if (state instanceof RoomSelectChart) {
                return;
            }

            executor.execute(() -> connection.send(new ClientBoundChangeStatePacket(state.toProtocol())));
        }
    }

}