package top.rymc.phira.main.game.state;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.event.game.GameAbortEvent;
import top.rymc.phira.main.event.game.GameEndEvent;
import top.rymc.phira.main.event.game.PlayerPlayedEvent;
import top.rymc.phira.main.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.record.PhiraRecord;
import top.rymc.phira.main.game.room.Room;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.message.AbortMessage;
import top.rymc.phira.protocol.data.message.GameEndMessage;
import top.rymc.phira.protocol.data.message.PlayedMessage;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.Playing;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RoomPlaying extends RoomGameState {

    private final Set<Player> donePlayers = ConcurrentHashMap.newKeySet();

    private final Map<Player, GameRecord> gameRecords = new ConcurrentHashMap<>();
    private final Map<Player, PhiraRecord> playerRecords = new ConcurrentHashMap<>();

    private final Map<Player, List<TouchFrame>> touchFrames = new ConcurrentHashMap<>();
    private final Map<Player, List<JudgeEvent>> judgeEvents = new ConcurrentHashMap<>();

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
    public void requireStart(Player player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void ready(Player player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void cancelReady(Player player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void touchSend(Player player, List<TouchFrame> touchFrames) {
        this.touchFrames.computeIfAbsent(player, p -> new CopyOnWriteArrayList<>()).addAll(touchFrames);
    }

    @Override
    public void judgeSend(Player player, List<JudgeEvent> judgeEvents) {
        this.judgeEvents.computeIfAbsent(player, p -> new CopyOnWriteArrayList<>()).addAll(judgeEvents);
    }

    @Override
    public void abort(Player player) {
        try {
            broadcast(ClientBoundMessagePacket.create(new AbortMessage(player.getId())));

            GameAbortEvent event = new GameAbortEvent(room, player, chart);
            Server.postEvent(event);
        } finally {
            updateState(player);
        }
    }

    @Override
    public void played(Player player, int recordId) {

        try {
            GameRecord record = PhiraFetcher.GET_RECORD_INFO.toIntFunction(e -> {
                throw GameOperationException.recordNotFound();
            }).apply(recordId);

            gameRecords.put(player, record);

            List<TouchFrame> playerTouchFrames = touchFrames.getOrDefault(player, List.of());
            List<JudgeEvent> playerJudgeEvents = judgeEvents.getOrDefault(player, List.of());

            if (!playerTouchFrames.isEmpty() || !playerJudgeEvents.isEmpty()) {

                PhiraRecord phiraRecord = new PhiraRecord(
                        record.getId(),
                        chart.getId(),
                        chart.getName(),
                        player.getId(),
                        player.getName(),
                        playerTouchFrames,
                        playerJudgeEvents
                );

                playerRecords.put(player, phiraRecord);
            }

            int id = player.getId();
            int score = record.getScore();
            float accuracy = record.getAccuracy();
            boolean fullCombo = record.isFullCombo();

            broadcast(ClientBoundMessagePacket.create(new PlayedMessage(id, score, accuracy, fullCombo)));

            PlayerPlayedEvent event = new PlayerPlayedEvent(player, room, recordId, score, accuracy, fullCombo);
            Server.postEvent(event);
        } finally {
            updateState(player);
        }
    }

    private void updateState(Player player) {
        donePlayers.add(player);

        if (isAllOnlinePlayersDone()) {
            GameEndEvent event = new GameEndEvent(room, chart, Map.copyOf(gameRecords), Map.copyOf(playerRecords));
            Server.postEvent(event);

            RoomSelectChart state = new RoomSelectChart(room, stateUpdater, chart);
            updateGameState(state);
            broadcast(ClientBoundMessagePacket.create(GameEndMessage.INSTANCE));
        }
    }

    private boolean isAllOnlinePlayersDone() {
        Set<Player> onlinePlayers = room.getPlayers().stream()
                .filter(Player::isOnline)
                .collect(Collectors.toSet());

        return donePlayers.containsAll(onlinePlayers);
    }

    @Override
    public GameState toProtocol() {
        return new Playing();
    }
}
