package top.rymc.phira.main.game.room.state;

import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;

import java.util.List;
import java.util.function.Consumer;

public final class RoomSelectChart extends RoomGameState {
    public RoomSelectChart(LocalRoom room, Consumer<RoomGameState> stateUpdater) {
        super(room, stateUpdater);
    }

    public RoomSelectChart(LocalRoom room, Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        super(room, stateUpdater, chart);
    }

    @Override
    public void handleJoin(Player player) {
    }

    @Override
    public void handleLeave(Player player) {
    }

    @Override
    public void requestStart(Player player) {
        requestStart(player, true);
    }

    @Override
    public void forceRequestStart() {
        requestStart(null, false);
    }

    private void requestStart(Player initiator, boolean sendStartMessage) {
        if (sendStartMessage && initiator != null) {
            broadcast(op -> op.gameRequireStart(initiator.getId()));
        }

        long onlinePlayers = room.getPlayerManager().getPlayers().stream()
                .filter(Player::isOnline)
                .count();
        long onlineMonitors = room.getPlayerManager().getMonitors().stream()
                .filter(Player::isOnline)
                .count();

        if (sendStartMessage && onlinePlayers == 1 && onlineMonitors == 0) {
            RoomPlaying state = RoomPlaying.create(room, stateUpdater, chart, room.getPlayerManager().getPlayers(), room.getPlayerManager().getMonitors());
            updateGameState(state);
            room.getPlayerManager().broadcast(op -> op.gameStartPlaying());
            return;
        }

        Player autoReady = initiator != null ? initiator : room.getPlayerManager().getHost().orElse(null);
        RoomWaitForReady state = new RoomWaitForReady(room, stateUpdater, chart, autoReady);
        updateGameState(state);
    }

    @Override
    public void forceStart() {
        throw GameOperationException.invalidState();
    }

    @Override
    public void ready(Player player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void cancelReady(Player player) {
        throw GameOperationException.invalidState();
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

    @Override
    public GameState toProtocol() {
        Integer id = chart == null ? null : chart.getId();
        return new SelectChart(id);
    }
}
