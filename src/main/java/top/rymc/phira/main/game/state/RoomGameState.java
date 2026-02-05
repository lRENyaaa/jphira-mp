package top.rymc.phira.main.game.state;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.protocol.data.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract sealed class RoomGameState implements ProtocolConvertible<GameState> permits RoomPlaying, RoomWaitForReady, RoomSelectChart {

    protected final Consumer<RoomGameState> stateUpdater;
    @Setter
    @Getter
    protected ChartInfo chart;

    public RoomGameState(Consumer<RoomGameState> stateUpdater) {
        this(stateUpdater, null, new ArrayList<>());

    }

    protected RoomGameState(Consumer<RoomGameState> stateUpdater, ChartInfo chart, List<Player> playerList) {
        this.stateUpdater = stateUpdater;
        this.chart = chart;
    }

    protected void updateGameState(RoomGameState newRoomGameState) {
        stateUpdater.accept(newRoomGameState);
    }


    public abstract void handleJoin(Player player);

    public abstract void handleLeave(Player player);

    public abstract void operation(OperationType operation, Player player);

}
