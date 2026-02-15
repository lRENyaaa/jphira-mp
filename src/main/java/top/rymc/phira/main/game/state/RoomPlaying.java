package top.rymc.phira.main.game.state;

import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.message.AbortMessage;
import top.rymc.phira.protocol.data.message.GameEndMessage;
import top.rymc.phira.protocol.data.message.PlayedMessage;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.Playing;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RoomPlaying extends RoomGameState {
    public RoomPlaying(Consumer<RoomGameState> stateUpdater) {
        super(stateUpdater);
    }

    public RoomPlaying(Consumer<RoomGameState> stateUpdater, ChartInfo chart){
        super(stateUpdater, chart);
    }

    private final Set<Player> donePlayers = ConcurrentHashMap.newKeySet();

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
        } finally {
            updateState(player,players,monitors);
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
        } finally {
            updateState(player,players,monitors);
        }
    }

    private void updateState(Player player, Set<Player> players, Set<Player> monitors) {
        donePlayers.add(player);
        if (isAllOnlinePlayersDone(players)) {
            RoomSelectChart state = new RoomSelectChart(stateUpdater, chart);
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
