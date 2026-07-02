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

public final class ChartPool {

    private static final Path POOL_FILE = Path.of("data", "chart-pools.json");
    private static final Path CACHE_FILE = Path.of("data", "chart-info-cache.json");
    private static final int DEFAULT_REFRESH_INTERVAL_ROUNDS = 5;
    private static final int DEFAULT_SELECT_CHART_COUNTDOWN_SECONDS = 150;
    private static final int[][] DEFAULT_POOL_CHART_IDS = {
            {30474, 42058},
            {33530, 52087}
    };

    private static final Type CHART_CACHE_TYPE = new TypeToken<Map<Integer, ChartInfo>>() {
    }.getType();

    private static final Map<Integer, ChartInfo> CHART_INFOS = new ConcurrentHashMap<>();
    private static PoolConfig config;

    private ChartPool() {
    }

    public static synchronized void preload() throws IOException {
        Files.createDirectories(POOL_FILE.getParent());
        loadCache();
        loadOrCreateConfig();
        normalizeConfig();
        validateConfig();
        for (int id : getAllChartIds()) {
            loadChartInfo(id);
        }
        saveCache();
        saveConfig();
    }

    public static synchronized List<PoolSnapshot> listPools() {
        return sortedPools().stream()
                .map(ChartPool::snapshotOf)
                .toList();
    }

    public static synchronized PoolSnapshot getCurrentPoolSnapshot() {
        return snapshotOf(config.currentPool);
    }

    public static synchronized PoolStatus getStatus() {
        return new PoolStatus(
                config.currentPool.id,
                config.pendingPoolId,
                config.refreshIntervalRounds,
                config.finishedRoundsSinceRefresh,
                config.selectChartCountdownSeconds
        );
    }

    public static synchronized void switchPool(int poolId) {
        PoolDefinition pool = requirePool(poolId);
        validateNonEmpty(pool);
        config.pendingPoolId = poolId;
        saveConfigUnchecked();
    }

    public static synchronized void setRefreshInterval(int rounds) {
        if (rounds <= 0) {
            throw new IllegalArgumentException("Refresh interval must be positive");
        }

        config.refreshIntervalRounds = rounds;
        saveConfigUnchecked();
    }

    public static synchronized int getSelectChartCountdownSeconds() {
        return config.selectChartCountdownSeconds;
    }

    public static synchronized void setSelectChartCountdownSeconds(int seconds) {
        if (seconds < 10) {
            throw new IllegalArgumentException("Countdown seconds must be at least 10");
        }

        config.selectChartCountdownSeconds = seconds;
        saveConfigUnchecked();
    }

    public static synchronized void finishPlayingRound() {
        config.finishedRoundsSinceRefresh++;
        if (config.finishedRoundsSinceRefresh >= config.refreshIntervalRounds) {
            refreshCurrentPool();
        }
        saveConfigUnchecked();
    }

    public static synchronized void addPool(int poolId, List<Integer> chartIds) throws IOException {
        if (findPool(poolId) != null) {
            throw new IllegalArgumentException("Pool already exists: " + poolId);
        }
        if (chartIds.isEmpty()) {
            throw new IllegalArgumentException("Pool cannot be empty");
        }

        List<Integer> distinctChartIds = distinct(chartIds);
        for (int chartId : distinctChartIds) {
            loadChartInfo(chartId);
        }

        config.pools.add(new PoolDefinition(poolId, null, distinctChartIds));
        saveCache();
        saveConfig();
    }

    public static synchronized void removePool(int poolId) {
        PoolDefinition pool = requirePool(poolId);
        if (config.pools.size() == 1) {
            throw new IllegalArgumentException("Cannot remove the last pool");
        }

        config.pools.remove(pool);
        if (config.pendingPoolId != null && config.pendingPoolId == poolId) {
            config.pendingPoolId = null;
        }
        validateConfig();
        saveConfigUnchecked();
    }

    public static synchronized void addChart(int poolId, int chartId) throws IOException {
        PoolDefinition pool = requirePool(poolId);
        ChartInfo info = loadChartInfo(chartId);
        if (pool.chartIds.contains(info.getId())) {
            return;
        }

        pool.chartIds.add(info.getId());
        saveCache();
        saveConfig();
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
        saveConfigUnchecked();
    }

    public static synchronized void setFavoriteId(int poolId, Integer favoriteId) {
        PoolDefinition pool = requirePool(poolId);
        pool.favoriteId = favoriteId;
        saveConfigUnchecked();
    }

    public static synchronized void setCurrentFavoriteId(Integer favoriteId) {
        config.currentPool.favoriteId = favoriteId;
        saveConfigUnchecked();
    }

    public static synchronized List<ChartInfo> getCurrentPool() {
        return config.currentPool.chartIds.stream()
                .map(ChartPool::requireChartInfo)
                .toList();
    }

    public static ChartInfo getChartInfo(int chartId) {
        return requireChartInfo(chartId);
    }

    public static boolean contains(int chartId, List<ChartInfo> pool) {
        return pool.stream().anyMatch(chart -> chart.getId() == chartId);
    }

    private static void refreshCurrentPool() {
        if (config.pendingPoolId != null) {
            config.currentPool = copyOf(requirePool(config.pendingPoolId));
            config.pendingPoolId = null;
        } else {
            List<PoolDefinition> pools = sortedPools();
            int index = 0;
            for (int i = 0; i < pools.size(); i++) {
                if (pools.get(i).id == config.currentPool.id) {
                    index = i;
                    break;
                }
            }
            config.currentPool = copyOf(pools.get((index + 1) % pools.size()));
        }
        config.finishedRoundsSinceRefresh = 0;
    }

    private static void loadOrCreateConfig() throws IOException {
        if (Files.exists(POOL_FILE)) {
            try (Reader reader = Files.newBufferedReader(POOL_FILE)) {
                config = GsonUtil.getGson().fromJson(reader, PoolConfig.class);
            }
            return;
        }

        config = new PoolConfig();
        config.refreshIntervalRounds = DEFAULT_REFRESH_INTERVAL_ROUNDS;
        config.selectChartCountdownSeconds = DEFAULT_SELECT_CHART_COUNTDOWN_SECONDS;
        config.finishedRoundsSinceRefresh = 0;
        for (int i = 0; i < DEFAULT_POOL_CHART_IDS.length; i++) {
            List<Integer> chartIds = new ArrayList<>();
            for (int id : DEFAULT_POOL_CHART_IDS[i]) {
                chartIds.add(id);
            }
            config.pools.add(new PoolDefinition(i, null, chartIds));
        }
        config.currentPool = copyOf(config.pools.get(0));
        saveConfig();
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
        PoolDefinition pool = findPool(poolId);
        if (pool == null) {
            throw new IllegalArgumentException("Pool not found: " + poolId);
        }
        return pool;
    }

    private static PoolDefinition findPool(int poolId) {
        return config.pools.stream()
                .filter(pool -> pool.id == poolId)
                .findFirst()
                .orElse(null);
    }

    private static List<PoolDefinition> sortedPools() {
        return config.pools.stream()
                .sorted(Comparator.comparingInt(pool -> pool.id))
                .toList();
    }

    private static void normalizeConfig() {
        if (config == null) {
            config = new PoolConfig();
        }
        if (config.pools == null) {
            config.pools = new ArrayList<>();
        }
        if (config.refreshIntervalRounds <= 0) {
            config.refreshIntervalRounds = DEFAULT_REFRESH_INTERVAL_ROUNDS;
        }
        if (config.selectChartCountdownSeconds < 10) {
            config.selectChartCountdownSeconds = DEFAULT_SELECT_CHART_COUNTDOWN_SECONDS;
        }
        if (config.finishedRoundsSinceRefresh < 0) {
            config.finishedRoundsSinceRefresh = 0;
        }
        for (PoolDefinition pool : config.pools) {
            normalizePool(pool);
        }
        if (config.currentPool == null && !config.pools.isEmpty()) {
            config.currentPool = copyOf(sortedPools().get(0));
        }
        if (config.currentPool != null) {
            normalizePool(config.currentPool);
        }
    }

    private static void normalizePool(PoolDefinition pool) {
        if (pool.chartIds == null) {
            pool.chartIds = new ArrayList<>();
        }
        pool.chartIds = distinct(pool.chartIds);
    }

    private static void validateConfig() {
        if (config.pools.isEmpty()) {
            throw new IllegalStateException("No chart pools configured");
        }

        for (PoolDefinition pool : config.pools) {
            validateNonEmpty(pool);
        }

        validateNonEmpty(config.currentPool);
        if (config.pendingPoolId != null) {
            requirePool(config.pendingPoolId);
        }
    }

    private static void validateNonEmpty(PoolDefinition pool) {
        if (pool == null || pool.chartIds == null || pool.chartIds.isEmpty()) {
            throw new IllegalStateException("Pool cannot be empty");
        }
    }

    private static PoolDefinition copyOf(PoolDefinition pool) {
        return new PoolDefinition(pool.id, pool.favoriteId, List.copyOf(pool.chartIds));
    }

    private static PoolSnapshot snapshotOf(PoolDefinition pool) {
        return new PoolSnapshot(pool.id, pool.favoriteId, List.copyOf(pool.chartIds));
    }

    private static List<Integer> distinct(List<Integer> chartIds) {
        return chartIds.stream()
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Integer> getAllChartIds() {
        List<Integer> chartIds = new ArrayList<>(config.currentPool.chartIds);
        config.pools.stream()
                .flatMap(pool -> pool.chartIds.stream())
                .forEach(chartIds::add);
        return chartIds.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private static void saveConfig() throws IOException {
        Files.createDirectories(POOL_FILE.getParent());
        try (Writer writer = Files.newBufferedWriter(POOL_FILE)) {
            GsonUtil.getGson().toJson(config, writer);
        }
    }

    private static void saveConfigUnchecked() {
        try {
            saveConfig();
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

    public record PoolSnapshot(int id, Integer favoriteId, List<Integer> chartIds) {
    }

    public record PoolStatus(int currentPoolId, Integer pendingPoolId, int refreshIntervalRounds, int finishedRoundsSinceRefresh, int selectChartCountdownSeconds) {
    }

    private static final class PoolConfig {
        private PoolDefinition currentPool;
        private Integer pendingPoolId;
        private int refreshIntervalRounds;
        private int finishedRoundsSinceRefresh;
        private int selectChartCountdownSeconds;
        private List<PoolDefinition> pools = new ArrayList<>();
    }

    private static final class PoolDefinition {
        private int id;
        private Integer favoriteId;
        private List<Integer> chartIds = new ArrayList<>();

        private PoolDefinition(int id, Integer favoriteId, List<Integer> chartIds) {
            this.id = id;
            this.favoriteId = favoriteId;
            this.chartIds = new ArrayList<>(chartIds);
        }
    }
}
