package top.rymc.phira.main.redis;

import lombok.Getter;

/**
 * Redis 连接配置。测试默认：127.0.0.1:6379，数据库 3。
 */
@Getter
public class RedisConfig {
    private final String host;
    private final int port;
    private final int database;
    private final String serverId;

    public RedisConfig(String host, int port, int database, String serverId) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.serverId = serverId;
    }

    public static RedisConfig defaultConfig() {
        return new RedisConfig("127.0.0.1", 6379, 3, "jphira-1");
    }
}
