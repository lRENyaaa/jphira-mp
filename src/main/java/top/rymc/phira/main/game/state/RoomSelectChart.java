package top.rymc.phira.main.game.state;

import top.rymc.phira.main.game.Player;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;

import java.util.function.Consumer;

public final class RoomSelectChart extends RoomGameState {
    public RoomSelectChart(Consumer<RoomGameState> stateUpdater) {
        super(stateUpdater);
    }

    @Override
    public void handleJoin(Player player) {

    }

    @Override
    public void handleLeave(Player player) {

    }

    @Override
    public void operation(OperationType operation, Player player) {

    }

    @Override
    public GameState toProtocol() {
        Integer id = chart == null ? null : chart.getId();
        return new SelectChart(id);
    }
}
