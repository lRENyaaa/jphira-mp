package top.rymc.phira.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Getter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import top.rymc.phira.main.command.CommandService;
import top.rymc.phira.main.config.ServerArgs;
import top.rymc.phira.main.game.Player;
import top.rymc.phira.main.game.PlayerManager;
import top.rymc.phira.main.network.ServerChannelInitializer;
import top.rymc.phira.plugin.core.PluginManager;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.plugin.event.Event;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Server {

    private static class Holder {
        private static final Server INSTANCE = new Server();
    }

    public static Server getInstance() {
        return Holder.INSTANCE;
    }

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setOut(IoBuilder.forLogger(LogManager.getLogger("STDOUT")).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(LogManager.getLogger("STDERR")).setLevel(Level.ERROR).buildPrintStream());
    }

    @Getter
    private static final Logger logger = LogManager.getLogger("Server");
    @Getter
    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    @Getter
    private PluginManager pluginManager;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel serverChannel;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong startTime = new AtomicLong(0);

    @Getter
    private ServerArgs args;

    public static void main(String[] args) {
        long bootStart = System.nanoTime();

        ServerArgs serverArgs = new ServerArgs(args);
        Server server = Server.getInstance();

        try {
            server.start(serverArgs, bootStart);
        } catch (Exception e) {
            getLogger().error("Failed to start server", e);
            System.exit(1);
        }

        server.awaitShutdown();
    }

    public void start(ServerArgs args, long bootStart) throws Exception {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already running");
        }

        this.args = args;
        startTime.set(System.currentTimeMillis());

        logger.info("Booting up Phira Server...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("ShutdownThread");
            if (isRunning()) {
                shutdown();
            }
        }));

        logger.info("Loading plugins from: {}", args.getPluginsDir());
        pluginManager = new PluginManager(logger, args.getPluginsDir());
        pluginManager.loadAll();
        logger.info("Loaded {} plugin(s)", pluginManager.getPluginCount());

        logger.info("Initializing network...");

        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("Netty-Boss", true));
        workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("Netty-Worker", true));

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerChannelInitializer(allChannels));

        ChannelFuture future = bootstrap.bind(InetAddress.getByName(args.getHost()), args.getPort()).sync();
        serverChannel = future.channel();
        allChannels.add(serverChannel);

        logger.info("Listening on {}:{}", args.getHost(), args.getPort());

        new CommandService(logger).start();

        long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - bootStart);
        logger.info("Done ({}s)!", String.format("%.3f", totalTime / 1000.0));
    }

    public void awaitShutdown() {
        if (serverChannel == null) return;

        try {
            logger.info("Server is running. Type 'stop' to stop.");
            serverChannel.closeFuture().await();
        } catch (InterruptedException e) {
            logger.info("Server thread interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        if (!running.compareAndSet(true, false)) return;

        long shutdownStart = System.nanoTime();
        int onlineCount = PlayerManager.getOnlinePlayers().size();
        int channelCount = allChannels.size();

        logger.info("Shutting down...");

        if (onlineCount > 0) {
            logger.info("Kicking {} player(s)...", onlineCount);
            PlayerManager.getOnlinePlayers().forEach(Player::kick);
        }

        if (pluginManager != null) {
            logger.info("Disabling {} plugin(s)...", pluginManager.getPluginCount());
            pluginManager.disableAll();
        }

        if (channelCount > 0) {
            logger.info("Closing {} channel(s)...", channelCount);
            allChannels.close().awaitUninterruptibly(10, TimeUnit.SECONDS);
            logger.info("Channels closed.");
        }

        if (workerGroup != null) {
            logger.info("Shutting down worker group...");
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
        }
        if (bossGroup != null) {
            logger.info("Shutting down boss group...");
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
        }

        long uptime = System.currentTimeMillis() - startTime.get();
        long shutdownTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - shutdownStart);

        logger.info("Uptime: {}m {}s",
                TimeUnit.MILLISECONDS.toMinutes(uptime),
                TimeUnit.MILLISECONDS.toSeconds(uptime) % 60);
        logger.info("Shutdown completed in {}ms. Goodbye!", shutdownTime);

        LogManager.shutdown();
    }

    public boolean isRunning() {
        return running.get();
    }

    public static void postEvent(Event event) {
        getInstance().pluginManager.getEventBus().post(event);
    }

    public static boolean postEvent(CancellableEvent event) {
        return getInstance().pluginManager.getEventBus().post(event).isCancelled();
    }
}