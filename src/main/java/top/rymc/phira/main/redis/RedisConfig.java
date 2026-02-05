package top.rymc.phira.main.redis;

import lombok.Getter;
import lombok.Setter;

/**
 * Redis 连接配置，默认 127.0.0.1:6379，数据库 3
 */
@Getter
@Setter
public class RedisConfig {

    private String host = "127.0.0.1";
    private int port = 6379;
    private int database = 3;
    private String serverId = "server-1";

    public static RedisConfig defaultConfig() {
        return new RedisConfig();
    }
}
