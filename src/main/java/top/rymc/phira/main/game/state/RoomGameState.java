package top.rymc.phira.main.game.state;

import lombok.Getter;
import lombok.Setter;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.network.ProtocolConvertible;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundChangeStatePacket;

import java.util.Set;
import java.util.function.Consumer;

public abstract sealed class RoomGameState implements ProtocolConvertible<GameState> permits RoomPlaying, RoomWaitForReady, RoomSelectChart {

    protected final Consumer<RoomGameState> stateUpdater;
    @Setter
    @Getter
    protected ChartInfo chart;

    public RoomGameState(Consumer<RoomGameState> stateUpdater) {
        this(stateUpdater, null);
    }

    protected RoomGameState(Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        this.stateUpdater = stateUpdater;
        this.chart = chart;
    }

    protected void updateGameState(RoomGameState newRoomGameState, Set<Player> players, Set<Player> monitors) {
        stateUpdater.accept(newRoomGameState);
        broadcast(players, monitors, new ClientBoundChangeStatePacket(newRoomGameState.toProtocol()));
    }

    protected void broadcast(Set<Player> players, Set<Player> monitors, ClientBoundPacket packet) {
        Consumer<Player> broadcastProcessor = p -> {
            if (p.isOnline()) p.getConnection().send(packet);
        };

        players.forEach(broadcastProcessor);
        monitors.forEach(broadcastProcessor);
    }


    public abstract void handleJoin(Player player);

    public abstract void handleLeave(Player player);

    public abstract void requireStart(Player player, Set<Player> players, Set<Player> monitors);

    public abstract void ready(Player player, Set<Player> players, Set<Player> monitors);

    public abstract void cancelReady(Player player, Set<Player> players, Set<Player> monitors);

    public abstract void abort(Player player, Set<Player> players, Set<Player> monitors);

    public abstract void played(Player player, int recordId, Set<Player> players, Set<Player> monitors);

}
