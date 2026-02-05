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
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.FullUserProfile;
import top.rymc.phira.protocol.data.RoomInfo;
import top.rymc.phira.protocol.data.message.ChatMessage;
import top.rymc.phira.protocol.data.message.CycleRoomMessage;
import top.rymc.phira.protocol.data.message.JoinRoomMessage;
import top.rymc.phira.protocol.data.message.LeaveRoomMessage;
import top.rymc.phira.protocol.data.message.LockRoomMessage;
import top.rymc.phira.protocol.data.message.SelectChartMessage;
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
        if (!players.remove(player) && !monitors.remove(player)) return;

        broadcast(new ClientBoundMessagePacket(new LeaveRoomMessage(player.getId(), player.getName())));
        handleLeave(player);

        if (players.isEmpty() && monitors.isEmpty()) {
            if (setting.autoDestroy) {
                onDestroy.accept(this);
            }
        } else if (setting.host && player.equals(host)) {
            host = players.iterator().next(); // 转移 Host 给任意剩余玩家
            host.getConnection().send(new ClientBoundChangeHostPacket(true));
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
    public void gameOperation(OperationType op, Player player) { state.operation(op, player); }

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

        }

        public void chat(Player player, String message) {
            if (!setting.chat) {
                throw new IllegalStateException("房间未启用聊天");
            }

            broadcast(new ClientBoundMessagePacket(new ChatMessage(player.getId(), message)));
        }

    }

    private void updateState(RoomGameState newState) {
        this.state = newState;
        broadcast(new ClientBoundChangeStatePacket(newState.toProtocol()));
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