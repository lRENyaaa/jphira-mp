package top.rymc.phira.main.game.state;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.event.game.GameAbortEvent;
import top.rymc.phira.main.event.game.GameEndEvent;
import top.rymc.phira.main.event.game.PlayerPlayedEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.Room;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.message.AbortMessage;
import top.rymc.phira.protocol.data.message.GameEndMessage;
import top.rymc.phira.protocol.data.message.PlayedMessage;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.Playing;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RoomPlaying extends RoomGameState {

    private final Set<Player> donePlayers = ConcurrentHashMap.newKeySet();
    private final Map<Player, Integer> playerRecords = new HashMap<>();

    public RoomPlaying(Room room, Consumer<RoomGameState> stateUpdater) {
        super(room, stateUpdater);
    }

    public RoomPlaying(Room room, Consumer<RoomGameState> stateUpdater, ChartInfo chart){
        super(room, stateUpdater, chart);
    }

    @Override
    public void handleJoin(Player player) {

    }

    @Override
    public void handleLeave(Player player) {

    }

    @Override
    public void requireStart(Player player, Set<Player> players, Set<Player> monitors) {
        throw GameOperationException.invalidState();
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
        try {
            broadcast(players, monitors, ClientBoundMessagePacket.create(new AbortMessage(player.getId())));

            GameAbortEvent event = new GameAbortEvent(room, player, chart);
            Server.postEvent(event);
        } finally {
            updateState(player, -1, players, monitors);
        }
    }

    @Override
    public void played(Player player, int recordId, Set<Player> players, Set<Player> monitors) {

        try {
            GameRecord record = PhiraFetcher.GET_RECORD_INFO.toIntFunction(e -> {
                throw GameOperationException.recordNotFound();
            }).apply(recordId);

            int id = player.getId();
            int score = record.getScore();
            float accuracy = record.getAccuracy();
            boolean fullCombo = record.isFullCombo();

            broadcast(players, monitors, ClientBoundMessagePacket.create(new PlayedMessage(id, score, accuracy, fullCombo)));

            PlayerPlayedEvent event = new PlayerPlayedEvent(player, room, recordId, score, accuracy, fullCombo);
            Server.postEvent(event);
        } finally {
            updateState(player, recordId, players, monitors);
        }
    }

    private void updateState(Player player, int recordId, Set<Player> players, Set<Player> monitors) {
        donePlayers.add(player);
        playerRecords.put(player, recordId);

        if (isAllOnlinePlayersDone(players)) {
            GameEndEvent event = new GameEndEvent(room, chart, Map.copyOf(playerRecords));
            Server.postEvent(event);

            RoomSelectChart state = new RoomSelectChart(room, stateUpdater, chart);
            updateGameState(state, players, monitors);
            broadcast(players, monitors, ClientBoundMessagePacket.create(GameEndMessage.INSTANCE));
        }
    }

    private boolean isAllOnlinePlayersDone(Set<Player> players) {
        Set<Player> onlinePlayers = players.stream()
                .filter(Player::isOnline)
                .collect(Collectors.toSet());

        return donePlayers.containsAll(onlinePlayers);
    }

    @Override
    public GameState toProtocol() {
        return new Playing();
    }
}
