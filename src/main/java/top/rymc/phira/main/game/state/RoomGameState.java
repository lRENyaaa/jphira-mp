package top.rymc.phira.main.game.state;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;

import java.util.List;
import java.util.function.Consumer;

public abstract sealed class RoomGameState implements ProtocolConvertible<GameState> permits RoomPlaying, RoomWaitForReady, RoomSelectChart {

    protected final Consumer<RoomGameState> stateUpdater;
    @Getter
    protected final Room room;
    @Setter
    @Getter
    protected ChartInfo chart;

    public RoomGameState(Room room, Consumer<RoomGameState> stateUpdater) {
        this(room, stateUpdater, null);
    }

    protected RoomGameState(Room room, Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        this.room = room;
        this.stateUpdater = stateUpdater;
        this.chart = chart;
    }

    protected void updateGameState(RoomGameState newRoomGameState) {
        stateUpdater.accept(newRoomGameState);
        room.getPlayers().forEach(p -> p.operations().ifPresent(op -> op.enterState(newRoomGameState.toProtocol())));
        room.getMonitors().forEach(p -> p.operations().ifPresent(op -> op.enterState(newRoomGameState.toProtocol())));
    }

    protected void broadcast(Consumer<PlayerOperations> action) {
        room.getPlayers().forEach(p -> p.operations().ifPresent(action));
        room.getMonitors().forEach(p -> p.operations().ifPresent(action));
    }

    public abstract void handleJoin(Player player);

    public abstract void handleLeave(Player player);

    public abstract void requireStart(Player player);

    public abstract void ready(Player player);

    public abstract void cancelReady(Player player);

    public abstract void touchSend(Player player, List<TouchFrame> touchFrames);

    public abstract void judgeSend(Player player, List<JudgeEvent> judgeEvents);

    public abstract void abort(Player player);

    public abstract void played(Player player, int recordId);

}
