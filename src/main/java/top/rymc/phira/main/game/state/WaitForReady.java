package top.rymc.phira.main.game.state;

import top.rymc.phira.main.game.Player;

import java.util.function.Consumer;

public final class WaitForReady extends GameState {

    public WaitForReady(Consumer<GameState> stateUpdater) {
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
        return new top.rymc.phira.protocol.data.state.WaitForReady();
    }
}
