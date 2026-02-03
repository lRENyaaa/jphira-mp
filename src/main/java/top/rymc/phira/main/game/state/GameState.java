package top.rymc.phira.main.game.state;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract sealed class GameState permits Playing, WaitForReady, SelectChart  {

    protected final Consumer<GameState> stateUpdater;
    @Setter
    @Getter
    protected ChartInfo chart;

    public GameState(Consumer<GameState> stateUpdater) {
        this(stateUpdater, null, new ArrayList<>());

    }

    protected GameState(Consumer<GameState> stateUpdater, ChartInfo chart, List<Player> playerList) {
        this.stateUpdater = stateUpdater;
        this.chart = chart;
    }

    protected void updateGameState(GameState newGameState) {
        stateUpdater.accept(newGameState);
    }


    public abstract void handleJoin(Player player);

    public abstract void handleLeave(Player player);

    public abstract void operation(OperationType operation, Player player);

    public abstract top.rymc.phira.protocol.data.state.GameState toProtocolGameState();
}
