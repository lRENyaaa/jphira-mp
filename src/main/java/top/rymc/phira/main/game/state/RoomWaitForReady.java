package top.rymc.phira.main.game.state;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.event.game.PlayerReadyEvent;
import top.rymc.phira.main.event.game.GamePlayingStartEvent;
import top.rymc.phira.main.event.game.PlayerCancelReadyEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.protocol.data.message.CancelReadyMessage;
import top.rymc.phira.protocol.data.message.ReadyMessage;
import top.rymc.phira.protocol.data.message.StartPlayingMessage;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.WaitForReady;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RoomWaitForReady extends RoomGameState {

    private final Set<Player> readyPlayers = ConcurrentHashMap.newKeySet();

    public RoomWaitForReady(Room room, Consumer<RoomGameState> stateUpdater) {
        super(room, stateUpdater);
    }

    public RoomWaitForReady(Room room, Consumer<RoomGameState> stateUpdater, ChartInfo chart, Player initiator) {
        this(room, stateUpdater, chart);
        readyPlayers.add(initiator);
    }

    public RoomWaitForReady(Room room, Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        super(room, stateUpdater, chart);
    }

    @Override
    public void handleJoin(Player player) {

    }

    @Override
    public void handleLeave(Player player) {
        readyPlayers.remove(player);
    }

    @Override
    public void requireStart(Player player, Set<Player> players, Set<Player> monitors) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void ready(Player player, Set<Player> players, Set<Player> monitors) {
        readyPlayers.add(player);
        broadcast(players, monitors, ClientBoundMessagePacket.create(new ReadyMessage(player.getId())));
        updateState(players, monitors);

        PlayerReadyEvent event = new PlayerReadyEvent(player, room);
        Server.postEvent(event);
    }

    @Override
    public void cancelReady(Player player, Set<Player> players, Set<Player> monitors) {
        readyPlayers.remove(player);
        broadcast(players, monitors, ClientBoundMessagePacket.create(new CancelReadyMessage(player.getId())));

        PlayerCancelReadyEvent event = new PlayerCancelReadyEvent(player, room);
        Server.postEvent(event);
    }

    @Override
    public void abort(Player player, Set<Player> players, Set<Player> monitors) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void played(Player player, int recordId, Set<Player> players, Set<Player> monitors) {
        throw GameOperationException.invalidState();
    }

    private void updateState(Set<Player> players, Set<Player> monitors) {
        if (isAllOnlinePlayersDone(players, monitors)) {
            GamePlayingStartEvent event = new GamePlayingStartEvent(room, chart, Set.copyOf(players), Set.copyOf(monitors));
            Server.postEvent(event);

            RoomPlaying state = new RoomPlaying(room, stateUpdater, chart);
            updateGameState(state, players, monitors);
            broadcast(players, monitors, ClientBoundMessagePacket.create(StartPlayingMessage.INSTANCE));
        }

    }

    private boolean isAllOnlinePlayersDone(Set<Player> players, Set<Player> monitors) {
        Set<Player> allPlayers = new HashSet<>(players);
        allPlayers.addAll(monitors);

        Set<Player> onlinePlayers = allPlayers.stream()
                .filter(Player::isOnline)
                .collect(Collectors.toSet());

        return readyPlayers.containsAll(onlinePlayers);
    }

    @Override
    public GameState toProtocol() {
        return new WaitForReady();
    }
}
