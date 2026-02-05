package top.rymc.phira.main.redis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 全局 Redis 实例持有者，由 Main 在启动时设置。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RedisHolder {
    @Setter
    private static RedisManager instance;

    public static RedisManager get() {
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null && !instance.isClosed();
    }
}
