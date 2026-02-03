package top.rymc.phira.main.game.state;

import top.rymc.phira.main.game.Player;

import java.util.function.Consumer;

public final class SelectChart extends GameState {
    public SelectChart(Consumer<GameState> stateUpdater) {
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
    public top.rymc.phira.protocol.data.state.GameState toProtocolGameState() {
        Integer id = chart == null ? null : chart.getId();
        return new top.rymc.phira.protocol.data.state.SelectChart(id);
    }
}
