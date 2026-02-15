package top.rymc.phira.main.game.state;

import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.protocol.data.message.GameStartMessage;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.util.Set;
import java.util.function.Consumer;

public final class RoomSelectChart extends RoomGameState {
    public RoomSelectChart(Consumer<RoomGameState> stateUpdater) {
        super(stateUpdater);
    }

    public RoomSelectChart(Consumer<RoomGameState> stateUpdater, ChartInfo chart){
        super(stateUpdater, chart);
    }

    @Override
    public void handleJoin(Player player) {

    }

    @Override
    public void handleLeave(Player player) {

    }

    @Override
    public void requireStart(Player player, Set<Player> players, Set<Player> monitors) {
        int totalPlayers = players.size() + monitors.size();

        if (totalPlayers == 1) {
            RoomPlaying state = new RoomPlaying(stateUpdater, chart);
            updateGameState(state, players, monitors);
        } else {
            RoomWaitForReady state = new RoomWaitForReady(stateUpdater, chart, player);
            updateGameState(state, players, monitors);
            broadcast(players, monitors, ClientBoundMessagePacket.create(new GameStartMessage(player.getId())));
        }
    }

    @Override
    public void ready(Player player, Set<Player> players, Set<Player> monitors) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void cancelReady(Player player, Set<Player> players, Set<Player> monitors) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void abort(Player player, Set<Player> players, Set<Player> monitors) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void played(Player player, int recordId, Set<Player> players, Set<Player> monitors) {
        throw GameOperationException.invalidState();
    }

    @Override
    public GameState toProtocol() {
        Integer id = chart == null ? null : chart.getId();
        return new SelectChart(id);
    }
}
