package top.rymc.phira.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.Getter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import top.rymc.phira.main.game.PlayerManager;
import top.rymc.phira.main.game.RoomManager;
import top.rymc.phira.main.network.ServerChannelInitializer;
import top.rymc.phira.main.redis.RedisConfig;
import top.rymc.phira.main.redis.RedisEventDispatcher;
import top.rymc.phira.main.redis.RedisHolder;
import top.rymc.phira.main.redis.RedisManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

public class Main {

    @Getter
    private static final Logger logger = LogManager.getLogger(Main.class);

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream());
    }

    private static Runnable shutdown = () -> {};

    private static CompletableFuture<Void> serverFuture = null;

    public static boolean isRunning() {
        return serverFuture != null && !serverFuture.isDone();
    }

    public static void main(String[] args) {
        logger.info("Phira Server is starting...");

        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown, "shutdown-hook"));

        OptionParser parser = new OptionParser();
        parser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(12346);

        OptionSet options = parser.parse(args);
        int port = (Integer) options.valueOf("port");

        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            RedisManager redis = new RedisManager(RedisConfig.defaultConfig());
            RedisHolder.setInstance(redis);
            RedisEventDispatcher.start(redis);
            logger.info("Redis connected: {}:{}, db={}", redis.getConfig().getHost(), redis.getConfig().getPort(), redis.getConfig().getDatabase());

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerChannelInitializer());

            InetAddress ipv4 = InetAddress.getByName("0.0.0.0");
            Channel ipv4Channel = serverBootstrap.bind(new InetSocketAddress(ipv4, port)).sync().channel();

            shutdown = () -> {
                if (RedisHolder.isAvailable()) {
                    RedisManager r = RedisHolder.get();
                    try {
                        PlayerManager.getAllPlayers().forEach(p -> p.getRoom().ifPresent(room -> room.leave(p)));
                        for (String uid : r.findPlayerIdsByServer()) {
                            r.removePlayerSession(Integer.parseInt(uid));
                        }
                    } finally {
                        r.close();
                        RedisHolder.setInstance(null);
                    }
                }
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            };

            logger.info("Listening on: {}:{}", ipv4.getHostAddress(), port);
            serverFuture = CompletableFuture.runAsync(() -> {
                try {
                    ipv4Channel.closeFuture().sync();
                } catch (InterruptedException e) {
                    logger.error("Server interrupted", e);
                }
            });

            // 还没写完
            // new CommandService(LogManager.getLogger("cmd-service")).start();
            serverFuture.join();

        } catch (InterruptedException | UnknownHostException e) {
            e.printStackTrace();
        } finally {
            shutdown.run();
        }
    }

    public static void shutdown() {
        shutdown.run();
    }

}
