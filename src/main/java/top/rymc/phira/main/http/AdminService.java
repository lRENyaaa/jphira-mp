package top.rymc.phira.main.http;

import top.rymc.phira.main.util.GsonUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理员名单。由独立配置文件 data/admins.json 决定（整数数组，即 Phira 用户 ID）。
 * 文件不存在时自动创建为空列表。
 */
public final class AdminService {

    private static final Path ADMIN_FILE = Path.of("data", "admins.json");
    private static final Set<Integer> ADMIN_IDS = ConcurrentHashMap.newKeySet();

    static {
        load();
    }

    private AdminService() {
    }

    public static boolean isAdmin(int userId) {
        return ADMIN_IDS.contains(userId);
    }

    private static synchronized void load() {
        ADMIN_IDS.clear();
        if (!Files.exists(ADMIN_FILE)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(ADMIN_FILE)) {
            Integer[] ids = GsonUtil.getGson().fromJson(reader, Integer[].class);
            if (ids != null) {
                for (int id : ids) {
                    ADMIN_IDS.add(id);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load admin list", e);
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(ADMIN_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(ADMIN_FILE)) {
                GsonUtil.getGson().toJson(ADMIN_IDS.stream().sorted().toList(), writer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save admin list", e);
        }
    }
}
