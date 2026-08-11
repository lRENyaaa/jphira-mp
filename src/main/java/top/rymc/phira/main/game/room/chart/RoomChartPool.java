package top.rymc.phira.main.game.room.chart;

import java.util.Comparator;
import java.util.List;

/**
 * 房间内的固化谱池运行时状态。
 *
 * 房间创建时从全局 pool 定义拷贝选定 pool 的快照形成固化列表，之后不再受全局定义增删影响。
 * 每个房间持有一个实例，负责：
 * - 固化池列表（创建后不可修改）
 * - 当前池、pending pool、已完成轮次计数
 * - 固化列表内按 id 排序循环轮换
 *
 * 状态不持久化，服务重启后房间不保留。
 */
public final class RoomChartPool {

    private final List<ChartPool.PoolSnapshot> pools;
    private ChartPool.PoolSnapshot currentPool;
    private Integer pendingPoolId;
    private int finishedRoundsSinceRefresh;

    public RoomChartPool(List<ChartPool.PoolSnapshot> pools) {
        if (pools == null || pools.isEmpty()) {
            throw new IllegalArgumentException("Room chart pool list cannot be empty");
        }
        this.pools = pools.stream()
                .sorted(Comparator.comparingInt(ChartPool.PoolSnapshot::id))
                .toList();
        this.currentPool = this.pools.get(0);
    }

    /**
     * 固化池列表（按 id 排序），只读。
     */
    public synchronized List<ChartPool.PoolSnapshot> getPools() {
        return List.copyOf(pools);
    }

    public synchronized ChartPool.PoolSnapshot getCurrentPoolSnapshot() {
        return currentPool;
    }

    public synchronized Integer getPendingPoolId() {
        return pendingPoolId;
    }

    public synchronized int getFinishedRoundsSinceRefresh() {
        return finishedRoundsSinceRefresh;
    }

    /**
     * 设置 pending pool。目标必须在该房间固化池列表内，不影响当前轮，下一次刷新时生效。
     */
    public synchronized void switchPool(int poolId) {
        if (poolById(poolId) == null) {
            throw new IllegalArgumentException("Pool not in room chart pool list: " + poolId);
        }
        pendingPoolId = poolId;
    }

    /**
     * 覆盖当前池的 favorite 展示，不写回全局定义。下一轮 SelectChart 快照生效。
     */
    public synchronized void setFavorite(Integer favoriteId) {
        currentPool = new ChartPool.PoolSnapshot(currentPool.id(), favoriteId, currentPool.defaultFlag(), currentPool.chartIds());
    }

    /**
     * Playing 结束时调用。达到刷新间隔后在固化列表内推进当前池。
     */
    public synchronized void finishPlayingRound(int refreshIntervalRounds) {
        finishedRoundsSinceRefresh++;
        if (finishedRoundsSinceRefresh >= refreshIntervalRounds) {
            refreshCurrentPool();
        }
    }

    private void refreshCurrentPool() {
        if (pendingPoolId != null) {
            currentPool = poolById(pendingPoolId);
            pendingPoolId = null;
        } else {
            int index = poolIndex(currentPool.id());
            currentPool = pools.get((index + 1) % pools.size());
        }
        finishedRoundsSinceRefresh = 0;
    }

    private ChartPool.PoolSnapshot poolById(int poolId) {
        return pools.stream()
                .filter(pool -> pool.id() == poolId)
                .findFirst()
                .orElse(null);
    }

    private int poolIndex(int poolId) {
        for (int i = 0; i < pools.size(); i++) {
            if (pools.get(i).id() == poolId) {
                return i;
            }
        }
        throw new IllegalStateException("Current pool not in room chart pool list: " + poolId);
    }
}
