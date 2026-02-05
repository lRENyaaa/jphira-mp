package top.rymc.phira.main.game.state;

import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.protocol.data.message.CancelReadyMessage;
import top.rymc.phira.protocol.data.message.ReadyMessage;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.WaitForReady;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RoomWaitForReady extends RoomGameState {

    public RoomWaitForReady(Consumer<RoomGameState> stateUpdater) {
        super(stateUpdater);
    }

    private final Set<Player> readyPlayers = ConcurrentHashMap.newKeySet();

    public RoomWaitForReady(Consumer<RoomGameState> stateUpdater, ChartInfo chart){
        super(stateUpdater, chart);
    }

    @Override
    public void handleJoin(Player player) {

    }

    @Override
    public void handleLeave(Player player) {
        readyPlayers.remove(player);
    }

    @Override
    public void requireStart(Player player, Set<Player> players, Set<Player> monitors) {
        throw new IllegalStateException("你不能在当前状态执行这个操作");
    }

    @Override
    public void ready(Player player, Set<Player> players, Set<Player> monitors) {
        readyPlayers.add(player);
        broadcast(players, monitors, new ClientBoundMessagePacket(new ReadyMessage(player.getId())));
        updateState(players, monitors);
    }

    @Override
    public void cancelReady(Player player, Set<Player> players, Set<Player> monitors) {
        readyPlayers.remove(player);
        broadcast(players, monitors, new ClientBoundMessagePacket(new CancelReadyMessage(player.getId())));
    }

    @Override
    public void abort(Player player, Set<Player> players, Set<Player> monitors) {
        throw new IllegalStateException("你不能在当前状态执行这个操作");
    }

    @Override
    public void played(Player player, int recordId, Set<Player> players, Set<Player> monitors) {
        throw new IllegalStateException("你不能在当前状态执行这个操作");
    }

    private void updateState(Set<Player> players, Set<Player> monitors) {
        if (isAllOnlinePlayersDone(players, monitors)) {
            RoomPlaying state = new RoomPlaying(stateUpdater, chart);
            updateGameState(state, players, monitors);
        }

    }

    private boolean isAllOnlinePlayersDone(Set<Player> players, Set<Player> monitors) {
        long onlineCount = Stream.concat(players.stream(), monitors.stream())
                .filter(Player::isOnline)
                .count();

        if (readyPlayers.size() != onlineCount) {
            return false;
        }

        Set<Integer> onlineIds = Stream.concat(players.stream(), monitors.stream())
                .filter(Player::isOnline)
                .map(Player::getId)
                .collect(Collectors.toSet());

        return readyPlayers.stream()
                .map(Player::getId)
                .allMatch(onlineIds::contains);
    }

    @Override
    public GameState toProtocol() {
        return new WaitForReady();
    }
}
