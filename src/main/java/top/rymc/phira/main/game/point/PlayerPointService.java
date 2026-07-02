package top.rymc.phira.main.game.point;

import com.google.gson.reflect.TypeToken;
import top.rymc.phira.main.game.player.Player;
import top.rymc.phira.main.util.GsonUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPointService {

    private static final Path POINT_FILE = Path.of("data", "player-points.json");
    private static final long RANKING_CACHE_MILLIS = 60_000;
    private static final Type POINT_DATA_TYPE = new TypeToken<Map<Integer, PointData>>() {
    }.getType();
    private static final Map<Integer, PointData> POINTS = new ConcurrentHashMap<>();
    private static volatile long rankingCacheTime;
    private static volatile List<String> rankingCache = List.of();

    static {
        load();
    }

    private PlayerPointService() {
    }

    public static synchronized PointSummary getSummary(Player player) {
        PointData data = touch(player);
        saveUnchecked();
        return new PointSummary(data.points, getRank(player.getId()));
    }

    public static synchronized int addPoints(Player player, int points) {
        PointData data = touch(player);
        data.points = Math.max(0, data.points + points);
        rankingCacheTime = 0;
        saveUnchecked();
        return data.points;
    }

    public static synchronized List<String> getTopRankingLines(int limit) {
        long now = System.currentTimeMillis();
        if (now - rankingCacheTime <= RANKING_CACHE_MILLIS) {
            return rankingCache;
        }

        List<String> lines = getRanking().stream()
                .limit(limit)
                .map(entry -> String.format("#%d %s：%d 分", getRank(entry.getKey()), entry.getValue().name, entry.getValue().points))
                .toList();
        rankingCache = lines;
        rankingCacheTime = now;
        return lines;
    }

    private static PointData touch(Player player) {
        PointData data = POINTS.computeIfAbsent(player.getId(), id -> new PointData());
        data.name = player.getName();
        return data;
    }

    private static int getRank(int playerId) {
        List<Map.Entry<Integer, PointData>> ranking = getRanking();

        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).getKey() == playerId) {
                return i + 1;
            }
        }
        return ranking.size() + 1;
    }

    private static List<Map.Entry<Integer, PointData>> getRanking() {
        return POINTS.entrySet().stream()
                .sorted(Map.Entry.<Integer, PointData>comparingByValue(
                        Comparator.comparingInt(PointData::getPoints).reversed()
                ).thenComparingInt(Map.Entry::getKey))
                .toList();
    }

    private static void load() {
        if (!Files.exists(POINT_FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(POINT_FILE)) {
            Map<Integer, PointData> data = GsonUtil.getGson().fromJson(reader, POINT_DATA_TYPE);
            if (data != null) {
                POINTS.putAll(data);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load player points", e);
        }
    }

    private static void saveUnchecked() {
        try {
            Files.createDirectories(POINT_FILE.getParent());
            Map<Integer, PointData> sorted = POINTS.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
            try (Writer writer = Files.newBufferedWriter(POINT_FILE)) {
                GsonUtil.getGson().toJson(sorted, writer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save player points", e);
        }
    }

    public record PointSummary(int points, int rank) {
    }

    private static final class PointData {
        private String name;
        private int points;

        private int getPoints() {
            return points;
        }
    }
}
