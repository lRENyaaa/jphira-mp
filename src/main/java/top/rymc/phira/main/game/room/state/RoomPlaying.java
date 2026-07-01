package top.rymc.phira.main.game.room.state;

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
import top.rymc.phira.protocol.data.state.SelectChart;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RoomPlaying extends RoomGameState {

    private static final int FORCE_FINISH_SECONDS = 120;
    private static final int FORCE_FINISH_NOTICE_SECONDS = 10;

    private final Set<Player> activePlayers = ConcurrentHashMap.newKeySet();
    private final Set<Player> donePlayers = ConcurrentHashMap.newKeySet();

    private final Map<Player, GameRecord> gameRecords = new ConcurrentHashMap<>();
    private final Map<Player, PhiraRecord> playerRecords = new ConcurrentHashMap<>();

    private final Map<Player, List<TouchFrame>> touchFrames = new ConcurrentHashMap<>();
    private final Map<Player, List<JudgeEvent>> judgeEvents = new ConcurrentHashMap<>();
    private final Set<ScheduledFuture<?>> forceFinishTasks = ConcurrentHashMap.newKeySet();
    private volatile boolean forceFinishCountdownStarted;

    public RoomPlaying(LocalRoom room, Consumer<RoomGameState> stateUpdater) {
        super(room, stateUpdater);
    }

    public RoomPlaying(LocalRoom room, Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        super(room, stateUpdater, chart);
    }

    public RoomPlaying(LocalRoom room, Consumer<RoomGameState> stateUpdater, ChartInfo chart, Set<Player> activePlayers) {
        super(room, stateUpdater, chart);
        this.activePlayers.addAll(activePlayers);
    }

    @Override
    public void handleJoin(Player player) {
        player.operations().ifPresent(op -> op.enterState(new SelectChart(chart.getId())));
    }

    @Override
    public void handleLeave(Player player) {
        if (activePlayers.contains(player)) {
            finishPlayer(player, false);
        }
    }

    @Override
    public void requireStart(Player player) {
        throw GameOperationException.permissionDenied();
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
        if (!activePlayers.contains(player) || donePlayers.contains(player)) {
            return;
        }

        try {
            broadcast(op -> op.gameAbort(player.getId()));
        } finally {
            finishPlayer(player, true);
        }
    }

    @Override
    public void played(Player player, int recordId) {
        if (!activePlayers.contains(player) || donePlayers.contains(player)) {
            return;
        }

        try {
            GameRecord record = PhiraFetcher.GET_RECORD_INFO.toIntFunction(e -> {
                throw GameOperationException.recordNotFound();
            }).apply(recordId);

            gameRecords.put(player, record);
            savePhiraRecord(player, record);

            broadcast(op -> op.gamePlayed(player.getId(), record.getScore(), record.getAccuracy(), record.isFullCombo()));
            startForceFinishCountdown();
        } catch (GameOperationException e) {
            player.operations().ifPresent(op -> op.receiveChat(SYSTEM_PLAYER_ID, "网络错误导致成绩提交失败，本轮将视为放弃。"));
            broadcastSystemMessage("因网络错误导致 " + player.getName() + " 成绩提交失败，已视为放弃本轮。");
            broadcast(op -> op.gameAbort(player.getId()));
            throw GameOperationException.recordSubmitFailed();
        } finally {
            finishPlayer(player, true);
        }
    }

    private void savePhiraRecord(Player player, GameRecord record) {
        List<TouchFrame> playerTouchFrames = touchFrames.getOrDefault(player, List.of());
        List<JudgeEvent> playerJudgeEvents = judgeEvents.getOrDefault(player, List.of());

        if (playerTouchFrames.isEmpty() && playerJudgeEvents.isEmpty()) {
            return;
        }

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

    private void startForceFinishCountdown() {
        if (forceFinishCountdownStarted) {
            return;
        }

        forceFinishCountdownStarted = true;
        forceFinishTasks.add(TIMER.schedule(
                () -> broadcastSystemMessage("本轮游戏将在 10 秒后强制结束。"),
                FORCE_FINISH_SECONDS - FORCE_FINISH_NOTICE_SECONDS,
                TimeUnit.SECONDS
        ));
        forceFinishTasks.add(TIMER.schedule(this::forceFinishGame, FORCE_FINISH_SECONDS, TimeUnit.SECONDS));
    }

    private void forceFinishGame() {
        RoomSelectChart state = new RoomSelectChart(room, stateUpdater, chart);
        forceFinishTasks.clear();
        activePlayers.stream()
                .filter(Player::isOnline)
                .forEach(player -> player.operations().ifPresent(op -> op.updateHostStatus(true)));
        broadcast(PlayerOperations::gameEnd);
        updateGameState(state);
        state.broadcastVoteBoard();
        state.activate();
    }

    private void cancelForceFinishCountdown() {
        forceFinishTasks.forEach(task -> task.cancel(false));
        forceFinishTasks.clear();
    }

    private void finishPlayer(Player player, boolean updateClientState) {
        donePlayers.add(player);

        if (updateClientState && player.isOnline()) {
            player.operations().ifPresent(op -> {
                op.updateHostStatus(true);
                op.enterState(new SelectChart(chart.getId()));
            });
        }

        if (isAllOnlineActivePlayersDone()) {
            cancelForceFinishCountdown();
            RoomSelectChart state = new RoomSelectChart(room, stateUpdater, chart);
            broadcast(PlayerOperations::gameEnd);
            updateGameState(state);
            state.broadcastVoteBoard();
            state.activate();
        }
    }

    private boolean isAllOnlineActivePlayersDone() {
        Set<Player> onlineActivePlayers = activePlayers.stream()
                .filter(Player::isOnline)
                .collect(Collectors.toSet());

        return donePlayers.containsAll(onlineActivePlayers);
    }

    @Override
    public GameState toProtocol() {
        return new Playing();
    }
}
