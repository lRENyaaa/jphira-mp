package top.rymc.phira.main.game.room.state;

import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.game.exception.GameOperationException;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.game.room.chart.ChartPool;
import top.rymc.phira.main.game.room.local.LocalRoom;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.data.state.GameState;
import top.rymc.phira.protocol.data.state.SelectChart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RoomSelectChart extends RoomGameState {

    private static final int COUNTDOWN_SECONDS = 90;
    private static final List<Integer> NOTICE_SECONDS = List.of(90, 60, 30, 10, 5, 3, 2, 1);
    private static final Random RANDOM = new Random();

    private final Map<Player, Integer> voteByPlayer = new ConcurrentHashMap<>();
    private final Set<ScheduledFuture<?>> countdownTasks = ConcurrentHashMap.newKeySet();
    private final ChartPool.PoolSnapshot currentPoolInfo;
    private final List<ChartInfo> currentPool;
    private volatile boolean countdownRunning;
    private volatile ChartInfo lockedChart;

    public RoomSelectChart(LocalRoom room, Consumer<RoomGameState> stateUpdater) {
        this(room, stateUpdater, null);
    }

    public RoomSelectChart(LocalRoom room, Consumer<RoomGameState> stateUpdater, ChartInfo chart) {
        super(room, stateUpdater, chart);
        this.currentPoolInfo = ChartPool.getCurrentPoolSnapshot();
        this.currentPool = currentPoolInfo.chartIds().stream()
                .map(ChartPool::getChartInfo)
                .toList();
    }

    @Override
    public void handleJoin(Player player) {
        sendVoteBoard(player);
        updateCountdownState();
    }

    @Override
    public void handleLeave(Player player) {
        voteByPlayer.remove(player);
        broadcastVoteBoard();
        updateCountdownState();
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

    }

    @Override
    public void judgeSend(Player player, List<JudgeEvent> judgeEvents) {

    }

    @Override
    public void abort(Player player) {
        throw GameOperationException.invalidState();
    }

    @Override
    public void played(Player player, int recordId) {
        throw GameOperationException.invalidState();
    }

    public void vote(Player player, int chartId) {
        if (room.containsMonitor(player)) {
            throw GameOperationException.permissionDenied();
        }

        if (lockedChart != null) {
            sendSystemMessage(player, "本轮曲目已锁定，无法继续改票。");
            throw GameOperationException.invalidState();
        }

        if (!ChartPool.contains(chartId, currentPool)) {
            throw GameOperationException.chartNotFound();
        }

        ChartInfo chart = ChartPool.getChartInfo(chartId);
        voteByPlayer.put(player, chartId);
        broadcastSystemMessage(player.getName() + " 已投票：" + formatChartName(chart));
        broadcastVoteBoard();
    }

    public void broadcastVoteBoard() {
        broadcast(op -> {
            op.receiveChat(SYSTEM_PLAYER_ID, MESSAGE_SEPARATOR);
            op.receiveChat(SYSTEM_PLAYER_ID, "zenith 本轮谱池 #" + currentPoolInfo.id());
            if (currentPoolInfo.favoriteId() != null) {
                op.receiveChat(SYSTEM_PLAYER_ID, "谱面收藏夹 ID：" + currentPoolInfo.favoriteId());
                op.receiveChat(SYSTEM_PLAYER_ID, "你可以通过导入收藏夹来一键导入谱池");
            }
            op.receiveChat(SYSTEM_PLAYER_ID, "使用选谱操作投票，只能选择下列歌曲。票数最高者开局。");
            for (String line : buildVoteBoardLines()) {
                op.receiveChat(SYSTEM_PLAYER_ID, line);
            }
            op.receiveChat(SYSTEM_PLAYER_ID, MESSAGE_SEPARATOR);
        });
    }

    private void sendVoteBoard(Player player) {
        sendSystemMessage(player, MESSAGE_SEPARATOR);
        sendSystemMessage(player, "zenith 本轮谱池 #" + currentPoolInfo.id());
        if (currentPoolInfo.favoriteId() != null) {
            sendSystemMessage(player, "谱面收藏夹 ID：" + currentPoolInfo.favoriteId());
            sendSystemMessage(player, "你可以通过导入收藏夹来一键导入谱池");
        }
        sendSystemMessage(player, "使用选谱操作投票，只能选择下列歌曲。票数最高者开局。");
        for (String line : buildVoteBoardLines()) {
            sendSystemMessage(player, line);
        }
        sendSystemMessage(player, MESSAGE_SEPARATOR);
    }

    private List<String> buildVoteBoardLines() {
        Map<Integer, Long> votes = voteByPlayer.values().stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        return currentPool.stream()
                .map(chart -> String.format("[%d票] %s  |  Lv.%s  |  ID:%d",
                        votes.getOrDefault(chart.getId(), 0L), chart.getName(), chart.getLevel(), chart.getId()))
                .toList();
    }

    private String formatChartName(ChartInfo chart) {
        return chart.getName() + " | Lv." + chart.getLevel() + " | ID:" + chart.getId();
    }

    public void activate() {
        updateCountdownState();
    }

    private void updateCountdownState() {
        if (countOnlinePlayers() >= MIN_PLAYER) {
            startCountdown();
        } else {
            cancelCountdown();
        }
    }

    private void startCountdown() {
        if (countdownRunning) {
            return;
        }

        countdownRunning = true;
        broadcastSystemMessage("已达到开局人数，90 秒后锁定投票并进入准备阶段。人数不足会取消倒计时。");

        for (int seconds : NOTICE_SECONDS) {
            countdownTasks.add(TIMER.schedule(() -> noticeCountdown(seconds), COUNTDOWN_SECONDS - seconds, TimeUnit.SECONDS));
        }
        countdownTasks.add(TIMER.schedule(this::finishCountdown, COUNTDOWN_SECONDS, TimeUnit.SECONDS));
    }

    private void cancelCountdown() {
        if (!countdownRunning) {
            return;
        }

        countdownRunning = false;
        countdownTasks.forEach(task -> task.cancel(false));
        countdownTasks.clear();
        broadcastSystemMessage("在线玩家不足，开局倒计时已取消。");
    }

    private void noticeCountdown(int seconds) {
        if (countdownRunning && countOnlinePlayers() >= MIN_PLAYER) {
            broadcastSystemMessage("投票锁定倒计时：" + seconds + " 秒。可继续改票。");
            if (seconds == 1) {
                lockedChart = selectWinningChart();
                broadcastSystemMessage("本轮曲目已锁定，无法继续改票。");
                broadcastSystemMessage(MESSAGE_SEPARATOR);
                broadcastSelectedChartState(lockedChart);
            }
        }
    }

    private void broadcastSelectedChartState(ChartInfo selectedChart) {
        broadcast(operations -> operations.updateHostStatus(false));
        broadcast(operations -> operations.enterState(new SelectChart(selectedChart.getId())));
    }

    private void finishCountdown() {
        if (!countdownRunning) {
            return;
        }

        countdownRunning = false;
        countdownTasks.clear();
        if (countOnlinePlayers() < MIN_PLAYER) {
            broadcastSystemMessage("在线玩家不足，本轮取消。");
            return;
        }

        ChartInfo selectedChart = lockedChart != null ? lockedChart : selectWinningChart();
        broadcastSystemMessage("投票结束，本轮曲目：" + formatChartName(selectedChart));

        RoomWaitForReady state = new RoomWaitForReady(room, stateUpdater, selectedChart);
        updateGameState(state);
        state.startCountdown();
    }

    private ChartInfo selectWinningChart() {
        Map<Integer, Long> votes = voteByPlayer.values().stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        long highestVotes = currentPool.stream()
                .map(chart -> votes.getOrDefault(chart.getId(), 0L))
                .max(Comparator.naturalOrder())
                .orElse(0L);

        List<ChartInfo> candidates = currentPool.stream()
                .filter(chart -> votes.getOrDefault(chart.getId(), 0L) == highestVotes)
                .collect(Collectors.toCollection(ArrayList::new));

        if (candidates.size() > 1) {
            broadcastSystemMessage("最高票出现并列，将随机抽选本轮曲目。");
        }

        return candidates.get(RANDOM.nextInt(candidates.size()));
    }

    @Override
    public GameState toProtocol() {
        Integer id = chart == null ? null : chart.getId();
        return new SelectChart(id);
    }
}
