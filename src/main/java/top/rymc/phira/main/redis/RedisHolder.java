package top.rymc.phira.main.redis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 全局 Redis 实例持有者，在 Main 启动时初始化，关闭时释放。
 */
public final class RedisHolder {

    private static final Logger LOG = LogManager.getLogger(RedisHolder.class);

    private static RedisService instance;

    public static synchronized RedisService get() {
        if (instance == null) {
            RedisConfig config = RedisConfig.defaultConfig();
            instance = new RedisService(config);
            instance.start();
        }
        return instance;
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            try {
                instance.stop();
            } catch (Exception e) {
                LOG.warn("Redis shutdown error", e);
            }
            instance = null;
        }
    }

    private RedisHolder() {}
}
