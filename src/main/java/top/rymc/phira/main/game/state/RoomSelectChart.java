package top.rymc.phira.main.game.state;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.event.game.GameRequireStartEvent;
import top.rymc.phira.main.event.game.GameStartEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.protocol.data.message.GameStartMessage;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class RoomSelectChart extends RoomGameState {
    public RoomSelectChart(Room room, Consumer<RoomGameState> stateUpdater) {
        super(room, stateUpdater);
    }

    public RoomSelectChart(Room room, Consumer<RoomGameState> stateUpdater, ChartInfo chart){
        super(room, stateUpdater, chart);
    }

    @Override
    public void handleJoin(Player player) {

    }

    @Override
    public void handleLeave(Player player) {

    }

    @Override
    public void requireStart(Player player) {
        Set<Player> players = room.getPlayers();
        Set<Player> monitors = room.getMonitors();
        int totalPlayers = players.size() + monitors.size();

        GameRequireStartEvent event = new GameRequireStartEvent(room, player, chart);
        Server.postEvent(event);
        if (event.isCancelled()) {
            return;
        }

        GameStartEvent startEvent = new GameStartEvent(room, player, chart, Set.copyOf(players), Set.copyOf(monitors));
        Server.postEvent(startEvent);

        if (totalPlayers == 1) {
            RoomPlaying state = new RoomPlaying(room, stateUpdater, chart);
            updateGameState(state);
        } else {
            RoomWaitForReady state = new RoomWaitForReady(room, stateUpdater, chart, player);
            updateGameState(state);
            broadcast(ClientBoundMessagePacket.create(new GameStartMessage(player.getId())));
        }
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
    public void touchSend(Player player, List<TouchFrame> touchFrames) {

    }

    @Override
    public void judgeSend(Player player, List<JudgeEvent> judgeEvents) {

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
