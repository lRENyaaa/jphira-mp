package top.rymc.phira.main.game.room.chart;

import com.google.gson.reflect.TypeToken;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.util.GsonUtil;
import top.rymc.phira.main.util.PhiraFetcher;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 全局谱面池定义与 ChartInfo 缓存。
 *
 * 只负责：
 * - pool 定义（chart-pools.json，存 id / favorite_id / chart_ids / default）
 * - ChartInfo 持久化缓存（chart-info-cache.json）与预拉取
 * - 池定义的新增/删除/修改
 *
 * 运行时状态（当前池、pending、轮次计数、favorite 覆盖等）由每个房间自己的 {@link RoomChartPool} 维护，
 * 房间创建时固化池列表，之后不受全局定义变化影响；服务重启后房间不保留，运行时状态不持久化。
 */
public final class ChartPool {

    private static final Path POOL_FILE = Path.of("data", "chart-pools.json");
    private static final Path CACHE_FILE = Path.of("data", "chart-info-cache.json");
    private static final int[][] DEFAULT_POOL_CHART_IDS = {
            {30474, 42058},
            {33530, 52087}
    };

    private static final Type CHART_CACHE_TYPE = new TypeToken<Map<Integer, ChartInfo>>() {
    }.getType();

    private static final Map<Integer, ChartInfo> CHART_INFOS = new ConcurrentHashMap<>();
    private static List<PoolDefinition> pools = new ArrayList<>();

    private ChartPool() {
    }

    public static synchronized void preload() throws IOException {
        Files.createDirectories(POOL_FILE.getParent());
        loadCache();
        loadOrCreatePools();
        normalizePools();
        validatePools();
        for (int id : getAllChartIds()) {
            loadChartInfo(id);
        }
        saveCache();
        savePools();
    }

    public static synchronized List<PoolSnapshot> listPools() {
        return sortedPools().stream()
                .map(ChartPool::snapshotOf)
                .toList();
    }

    /**
     * 所有默认启用（default=true）的 pool 快照，按 id 排序。用于玩家创建房间时的固化池列表。
     */
    public static synchronized List<PoolSnapshot> getDefaultPools() {
        return sortedPools().stream()
                .filter(pool -> pool.defaultFlag)
                .map(ChartPool::snapshotOf)
                .toList();
    }

    /**
     * 查找全局 pool 定义快照，不存在返回 null。
     */
    public static synchronized PoolSnapshot findPool(int poolId) {
        PoolDefinition pool = findPoolDefinition(poolId);
        return pool == null ? null : snapshotOf(pool);
    }

    public static synchronized void addPool(int poolId, List<Integer> chartIds) throws IOException {
        if (findPoolDefinition(poolId) != null) {
            throw new IllegalArgumentException("Pool already exists: " + poolId);
        }
        if (chartIds.isEmpty()) {
            throw new IllegalArgumentException("Pool cannot be empty");
        }

        List<Integer> distinctChartIds = distinct(chartIds);
        for (int chartId : distinctChartIds) {
            loadChartInfo(chartId);
        }

        pools.add(new PoolDefinition(poolId, null, false, distinctChartIds));
        saveCache();
        savePools();
    }

    public static synchronized void removePool(int poolId) {
        PoolDefinition pool = requirePool(poolId);
        if (pools.size() == 1) {
            throw new IllegalArgumentException("Cannot remove the last pool");
        }
        if (pool.defaultFlag && countDefaultPools() == 1) {
            throw new IllegalArgumentException("Cannot remove the last default pool");
        }

        pools.remove(pool);
        validatePools();
        savePoolsUnchecked();
    }

    public static synchronized void addChart(int poolId, int chartId) throws IOException {
        PoolDefinition pool = requirePool(poolId);
        ChartInfo info = loadChartInfo(chartId);
        if (pool.chartIds.contains(info.getId())) {
            return;
        }

        pool.chartIds.add(info.getId());
        saveCache();
        savePools();
    }

    public static synchronized void removeChart(int poolId, int chartId) {
        PoolDefinition pool = requirePool(poolId);
        if (!pool.chartIds.contains(chartId)) {
            return;
        }
        if (pool.chartIds.size() == 1) {
            throw new IllegalArgumentException("Pool cannot be empty");
        }

        pool.chartIds.remove(Integer.valueOf(chartId));
        savePoolsUnchecked();
    }

    public static synchronized void setFavoriteId(int poolId, Integer favoriteId) {
        PoolDefinition pool = requirePool(poolId);
        pool.favoriteId = favoriteId;
        savePoolsUnchecked();
    }

    public static synchronized void setDefaultFlag(int poolId, boolean defaultFlag) {
        PoolDefinition pool = requirePool(poolId);
        if (pool.defaultFlag == defaultFlag) {
            return;
        }
        pool.defaultFlag = defaultFlag;
        savePoolsUnchecked();
    }

    public static ChartInfo getChartInfo(int chartId) {
        return requireChartInfo(chartId);
    }

    public static boolean contains(int chartId, List<ChartInfo> pool) {
        return pool.stream().anyMatch(chart -> chart.getId() == chartId);
    }

    private static void loadOrCreatePools() throws IOException {
        if (Files.exists(POOL_FILE)) {
            try (Reader reader = Files.newBufferedReader(POOL_FILE)) {
                PoolConfig config = GsonUtil.getGson().fromJson(reader, PoolConfig.class);
                if (config != null && config.pools != null) {
                    pools = config.pools;
                    return;
                }
            }
        }

        pools = new ArrayList<>();
        for (int i = 0; i < DEFAULT_POOL_CHART_IDS.length; i++) {
            List<Integer> chartIds = new ArrayList<>();
            for (int id : DEFAULT_POOL_CHART_IDS[i]) {
                chartIds.add(id);
            }
            // 默认配置中第一个池默认启用，保证玩家开箱即可创建房间。
            pools.add(new PoolDefinition(i, null, i == 0, chartIds));
        }
        savePools();
    }

    private static void loadCache() throws IOException {
        if (!Files.exists(CACHE_FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CACHE_FILE)) {
            Map<Integer, ChartInfo> cache = GsonUtil.getGson().fromJson(reader, CHART_CACHE_TYPE);
            if (cache != null) {
                CHART_INFOS.putAll(cache);
            }
        }
    }

    private static ChartInfo loadChartInfo(int chartId) throws IOException {
        ChartInfo info = CHART_INFOS.get(chartId);
        if (info != null) {
            return info;
        }

        info = PhiraFetcher.GET_CHART_INFO.apply(chartId);
        CHART_INFOS.put(chartId, info);
        return info;
    }

    private static ChartInfo requireChartInfo(int chartId) {
        ChartInfo info = CHART_INFOS.get(chartId);
        if (info == null) {
            throw new IllegalStateException("Chart info is not loaded: " + chartId);
        }
        return info;
    }

    private static PoolDefinition requirePool(int poolId) {
        PoolDefinition pool = findPoolDefinition(poolId);
        if (pool == null) {
            throw new IllegalArgumentException("Pool not found: " + poolId);
        }
        return pool;
    }

    private static PoolDefinition findPoolDefinition(int poolId) {
        return pools.stream()
                .filter(pool -> pool.id == poolId)
                .findFirst()
                .orElse(null);
    }

    private static long countDefaultPools() {
        return pools.stream().filter(pool -> pool.defaultFlag).count();
    }

    private static List<PoolDefinition> sortedPools() {
        return pools.stream()
                .sorted(Comparator.comparingInt(pool -> pool.id))
                .toList();
    }

    private static void normalizePools() {
        if (pools == null) {
            pools = new ArrayList<>();
        }
        for (PoolDefinition pool : pools) {
            if (pool.chartIds == null) {
                pool.chartIds = new ArrayList<>();
            }
            pool.chartIds = distinct(pool.chartIds);
        }
    }

    private static void validatePools() {
        if (pools.isEmpty()) {
            throw new IllegalStateException("No chart pools configured");
        }

        for (PoolDefinition pool : pools) {
            if (pool == null || pool.chartIds == null || pool.chartIds.isEmpty()) {
                throw new IllegalStateException("Pool cannot be empty");
            }
        }
    }

    private static PoolSnapshot snapshotOf(PoolDefinition pool) {
        return new PoolSnapshot(pool.id, pool.favoriteId, pool.defaultFlag, List.copyOf(pool.chartIds));
    }

    private static List<Integer> distinct(List<Integer> chartIds) {
        return chartIds.stream()
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Integer> getAllChartIds() {
        return pools.stream()
                .flatMap(pool -> pool.chartIds.stream())
                .distinct()
                .toList();
    }

    private static void savePools() throws IOException {
        Files.createDirectories(POOL_FILE.getParent());
        PoolConfig config = new PoolConfig();
        config.pools = pools;
        try (Writer writer = Files.newBufferedWriter(POOL_FILE)) {
            GsonUtil.getGson().toJson(config, writer);
        }
    }

    private static void savePoolsUnchecked() {
        try {
            savePools();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save chart pools", e);
        }
    }

    private static void saveCache() throws IOException {
        Files.createDirectories(CACHE_FILE.getParent());
        Map<Integer, ChartInfo> sortedCache = CHART_INFOS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        try (Writer writer = Files.newBufferedWriter(CACHE_FILE)) {
            GsonUtil.getGson().toJson(sortedCache, writer);
        }
    }

    public record PoolSnapshot(int id, Integer favoriteId, boolean defaultFlag, List<Integer> chartIds) {
    }

    private static final class PoolConfig {
        private List<PoolDefinition> pools = new ArrayList<>();
    }

    private static final class PoolDefinition {
        private int id;
        private Integer favoriteId;
        private boolean defaultFlag;
        private List<Integer> chartIds = new ArrayList<>();

        private PoolDefinition(int id, Integer favoriteId, boolean defaultFlag, List<Integer> chartIds) {
            this.id = id;
            this.favoriteId = favoriteId;
            this.defaultFlag = defaultFlag;
            this.chartIds = new ArrayList<>(chartIds);
        }
    }
}
