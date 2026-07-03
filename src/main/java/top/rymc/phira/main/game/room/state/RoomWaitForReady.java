package top.rymc.phira.main.game.room.state;

import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.data.state.WaitForReady;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RoomWaitForReady extends RoomGameState {

    private final Set<Player> readyPlayers = ConcurrentHashMap.newKeySet();

    public RoomWaitForReady(LocalRoom room, Consumer<RoomGameState> stateUpdater) {
        super(room, stateUpdater);
    }

    public RoomWaitForReady(LocalRoom room, Consumer<RoomGameState> stateUpdater, ChartInfo chart, Player initiator) {
        this(room, stateUpdater, chart);
        if (initiator != null) {
            readyPlayers.add(initiator);
        }
    }

    public RoomWaitForReady(LocalRoom room, Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        super(room, stateUpdater, chart);
    }

    @Override
    public void handleJoin(Player player) {
    }

    @Override
    public void handleLeave(Player player) {
        readyPlayers.remove(player);
        updateState(false);
    }

    @Override
    public void requestStart(Player player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void forceRequestStart() {
        throw GameOperationException.invalidState();
    }

    @Override
    public void forceStart() {
        updateState(true);
    }

    @Override
    public void ready(Player player) {
        readyPlayers.add(player);
        broadcast(op -> op.memberReady(player.getId()));
        updateState(false);
    }

    @Override
    public void cancelReady(Player player) {
        if (room.containsMonitor(player)) {
            throw GameOperationException.invalidState();
        }

        readyPlayers.remove(player);
        broadcast(op -> op.memberCancelReady(player.getId()));
    }

    @Override
    public boolean touchSend(Player player, List<TouchFrame> touchFrames) {
        return false;
    }

    @Override
    public boolean judgeSend(Player player, List<JudgeEvent> judgeEvents) {
        return false;
    }

    @Override
    public void abort(Player player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void played(Player player, int recordId) {
        throw GameOperationException.invalidState();
    }

    private void updateState(boolean force) {
        if (!force && !isAllOnlinePlayersReady()) {
            return;
        }

        Set<Player> participants = force
                ? readyPlayers.stream().filter(Player::isOnline).collect(Collectors.toSet())
                : onlineMembers();

        RoomPlaying state = RoomPlaying.create(room, stateUpdater, chart, participants, Set.of());
        stateUpdater.accept(state);

        if (participants.isEmpty()) {
            state.finishEmptyGame();
            return;
        }

        room.getPlayerManager().getPlayers().forEach(player -> sendPlayingState(player, participants.contains(player)));
        room.getPlayerManager().getMonitors().forEach(player -> sendPlayingState(player, participants.contains(player)));
    }

    private void sendPlayingState(Player player, boolean playing) {
        player.operations().ifPresent(operations -> {
            if (playing) {
                operations.gameStartPlaying();
                operations.enterState(stateToProtocol());
            } else {
                operations.enterState(new SelectChart(chart == null ? null : chart.getId()));
            }
        });
    }

    private GameState stateToProtocol() {
        return new top.rymc.phira.protocol.data.state.Playing();
    }

    private boolean isAllOnlinePlayersReady() {
        return readyPlayers.containsAll(onlineMembers());
    }

    private Set<Player> onlineMembers() {
        Set<Player> onlinePlayers = room.getPlayerManager().getPlayers().stream()
                .filter(Player::isOnline)
                .collect(Collectors.toSet());
        room.getPlayerManager().getMonitors().stream()
                .filter(Player::isOnline)
                .forEach(onlinePlayers::add);
        return onlinePlayers;
    }

    @Override
    public GameState toProtocol() {
        return new WaitForReady();
    }
}
