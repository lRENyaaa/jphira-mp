package top.rymc.phira.main.game.room.state;

import top.rymc.phira.main.Server;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.player.operations.PlayerOperations;
import top.rymc.phira.main.game.record.PhiraRecord;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.Playing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RoomPlaying extends RoomGameState {

    private final Set<Player> donePlayers = ConcurrentHashMap.newKeySet();
    private final Set<Player> debugLoggedPlayers = ConcurrentHashMap.newKeySet();

    private final Map<Player, GameRecord> gameRecords = new ConcurrentHashMap<>();
    private final Map<Player, PhiraRecord> playerRecords = new ConcurrentHashMap<>();

    private final Map<Player, List<TouchFrame>> touchFrames = new ConcurrentHashMap<>();
    private final Map<Player, List<JudgeEvent>> judgeEvents = new ConcurrentHashMap<>();

    public static RoomPlaying create(
            LocalRoom room,
            Consumer<RoomGameState> stateUpdater,
            ChartInfo chart,
            Set<Player> participants,
            Set<Player> ignoredMonitors
    ) {
        RoomPlaying state = new RoomPlaying(room, stateUpdater, chart);
        room.getPlayerManager().getPlayers().stream()
                .filter(player -> !participants.contains(player))
                .forEach(state.donePlayers::add);
        return state;
    }

    public RoomPlaying(LocalRoom room, Consumer<RoomGameState> stateUpdater) {
        super(room, stateUpdater);
    }

    public RoomPlaying(LocalRoom room, Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        super(room, stateUpdater, chart);
    }

    @Override
    public void handleJoin(Player player) {
        if (room.containsPlayer(player)) {
            donePlayers.add(player);
        }
    }

    @Override
    public void handleLeave(Player player) {
        if (room.containsPlayer(player)) {
            donePlayers.add(player);
            checkEnd();
        }
    }

    @Override
    public void requestStart(Player player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void forceRequestStart() {
        throw GameOperationException.invalidState();
    }

    @Override
    public void forceStart() {
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
    public boolean touchSend(Player player, List<TouchFrame> touchFrames) {
        if (!canSubmitPlayingData(player)) {
            logIgnoredPlayingData(player);
            return false;
        }

        this.touchFrames.computeIfAbsent(player, p -> new CopyOnWriteArrayList<>()).addAll(touchFrames);
        return true;
    }

    @Override
    public boolean judgeSend(Player player, List<JudgeEvent> judgeEvents) {
        if (!canSubmitPlayingData(player)) {
            logIgnoredPlayingData(player);
            return false;
        }

        this.judgeEvents.computeIfAbsent(player, p -> new CopyOnWriteArrayList<>()).addAll(judgeEvents);
        return true;
    }

    @Override
    public void abort(Player player) {
        if (room.containsMonitor(player)) {
            throw GameOperationException.invalidState();
        }

        if (donePlayers.contains(player)) {
            logIgnoredPlayingData(player);
            throw GameOperationException.playAlreadyDone();
        }

        abortInternal(player);
    }

    @Override
    public void played(Player player, int recordId) {
        if (room.containsMonitor(player)) {
            throw GameOperationException.invalidState();
        }

        if (donePlayers.contains(player)) {
            logIgnoredPlayingData(player);
            throw GameOperationException.playAlreadyDone();
        }

        GameRecord record;
        try {
            record = PhiraFetcher.GET_RECORD_INFO.toIntFunction(e -> {
                throw GameOperationException.recordFetchFailed(recordId);
            }).apply(recordId);
        } catch (Exception e) {
            abortInternal(player);
            throw GameOperationException.recordFetchFailed(recordId);
        }

        try {
            gameRecords.put(player, record);

            List<TouchFrame> playerTouchFrames = touchFrames.getOrDefault(player, List.of());
            List<JudgeEvent> playerJudgeEvents = judgeEvents.getOrDefault(player, List.of());

            if (!playerTouchFrames.isEmpty() || !playerJudgeEvents.isEmpty()) {
                PhiraRecord phiraRecord = new PhiraRecord(
                        record.getId(),
                        record.getTime().toInstant().toEpochMilli(),
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

            broadcast(op -> op.gamePlayed(id, score, accuracy, fullCombo));
        } finally {
            updateState(player);
            player.operations().ifPresent(operations -> operations.enterState(new top.rymc.phira.protocol.data.state.SelectChart(chart == null ? null : chart.getId())));
        }
    }

    private boolean canSubmitPlayingData(Player player) {
        return room.containsPlayer(player) && !donePlayers.contains(player);
    }

    private void logIgnoredPlayingData(Player player) {
        if (debugLoggedPlayers.add(player)) {
            Server.getLogger().debug("Ignoring playing packet from player {}, room {}", player.getId(), room.getRoomId());
        }
    }

    private void abortInternal(Player player) {
        try {
            broadcast(op -> op.gameAbort(player.getId()));
        } finally {
            updateState(player);
        }
    }

    private void updateState(Player player) {
        donePlayers.add(player);
        checkEnd();
    }

    public void finishEmptyGame() {
        finishGame();
    }

    private void checkEnd() {
        if (isAllOnlinePlayersDone()) {
            finishGame();
        }
    }

    private void finishGame() {
        RoomSelectChart state = new RoomSelectChart(room, stateUpdater, chart);
        stateUpdater.accept(state);
        broadcast(PlayerOperations::gameEnd);
        broadcast(operations -> operations.enterState(state.toProtocol()));
        if (room.getSetting().isCycle()) {
            room.getPlayerManager().transferHostToNextPlayer();
        }
    }

    private boolean isAllOnlinePlayersDone() {
        Set<Player> onlinePlayers = room.getPlayerManager().getPlayers().stream()
                .filter(Player::isOnline)
                .collect(Collectors.toSet());

        return donePlayers.containsAll(onlinePlayers);
    }

    @Override
    public GameState toProtocol() {
        return new Playing();
    }
}
