package top.rymc.phira.main.game.room.chart;

import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.util.PhiraFetcher;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ChartPool {

    private static final int[][] POOL_CHART_IDS = {
            {30474, 42058},
            {33530, 52087}
    };

    private static final Map<Integer, ChartInfo> CHART_INFOS = new ConcurrentHashMap<>();

    private ChartPool() {
    }

    public static void preload() throws IOException {
        for (int id : getAllChartIds()) {
            CHART_INFOS.put(id, PhiraFetcher.GET_CHART_INFO.apply(id));
        }
    }

    public static List<Integer> getCurrentPoolChartIds() {
        int index = Math.floorMod(Instant.now().atZone(ZoneOffset.UTC).getHour(), POOL_CHART_IDS.length);
        return Arrays.stream(POOL_CHART_IDS[index]).boxed().toList();
    }

    public static List<ChartInfo> getCurrentPool() {
        return getCurrentPoolChartIds().stream()
                .map(ChartPool::requireChartInfo)
                .toList();
    }

    public static ChartInfo getChartInfo(int chartId) {
        return requireChartInfo(chartId);
    }

    private static ChartInfo requireChartInfo(int chartId) {
        ChartInfo info = CHART_INFOS.get(chartId);
        if (info == null) {
            throw new IllegalStateException("Chart info is not loaded: " + chartId);
        }
        return info;
    }

    public static boolean contains(int chartId, List<ChartInfo> pool) {
        return pool.stream().anyMatch(chart -> chart.getId() == chartId);
    }

    private static List<Integer> getAllChartIds() {
        return Arrays.stream(POOL_CHART_IDS)
                .flatMapToInt(Arrays::stream)
                .boxed()
                .distinct()
                .collect(Collectors.toList());
    }
}
