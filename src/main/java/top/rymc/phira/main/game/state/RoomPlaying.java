package top.rymc.phira.main.game.state;

import top.rymc.phira.main.game.Player;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.Playing;

import java.util.function.Consumer;

public final class RoomPlaying extends RoomGameState {
    public RoomPlaying(Consumer<RoomGameState> stateUpdater) {
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
        return new Playing();
    }
}
