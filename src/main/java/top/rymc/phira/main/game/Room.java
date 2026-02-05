package top.rymc.phira.main.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.state.RoomGameState;
import top.rymc.phira.main.game.state.RoomSelectChart;
import top.rymc.phira.main.game.state.RoomWaitForReady;
import top.rymc.phira.main.game.state.OperationType;
import top.rymc.phira.main.network.PlayerConnection;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.message.JoinRoomMessage;
import top.rymc.phira.protocol.data.message.LeaveRoomMessage;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeHostPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeStatePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundJoinRoomPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundOnJoinRoomPacket;

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

    // 双向引用：Room 持有 Player，但 Player 不持有 Room
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
        private boolean host = false;
        private int maxPlayer = 8;
        private boolean locked = false;
        private boolean cycle = false;
        private boolean live = true;
    }

    // 工厂：创建即绑定 Host
    public static Room create(String roomId, Consumer<Room> onDestroy, Player hostPlayer) {
        Room room = new Room(roomId, onDestroy);
        room.state = new RoomSelectChart(room::updateState);
        room.join(hostPlayer, false);
        room.host = hostPlayer;
        return room;
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
    }

    // 玩家离开（自动转移 Host 或销毁）
    public synchronized void leave(Player player) {
        if (!players.remove(player) && !monitors.remove(player)) return;

        broadcast(new ClientBoundMessagePacket(new LeaveRoomMessage(player.getId(), player.getName())));

        if (players.isEmpty() && monitors.isEmpty()) {
            if (setting.autoDestroy) {
                onDestroy.accept(this);
            }
        } else if (setting.host && player.equals(host)) {
            host = players.iterator().next(); // 转移 Host 给任意剩余玩家
            host.getConnection().send(new ClientBoundChangeHostPacket(true));
        }
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

    // 状态机委托
    public void handleJoin(Player player) { state.handleJoin(player); }
    public void handleLeave(Player player) { state.handleLeave(player); }
    public void operation(OperationType op, Player player) { state.operation(op, player); }

    private void updateState(RoomGameState newState) {
        this.state = newState;
        broadcast(new ClientBoundChangeStatePacket(newState.toProtocol()));
    }

    // 广播给房间内所有玩家（自动过滤离线）
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

            // 这是一个客户端协议问题，我们必须构造假的状态来尽可能避免客户端崩溃
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