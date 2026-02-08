package top.rymc.phira.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.Getter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import top.rymc.phira.main.network.ServerChannelInitializer;
import top.rymc.phira.plugin.core.PluginManager;
import top.rymc.phira.plugin.event.CancellableEvent;
import top.rymc.phira.plugin.event.Event;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final Logger logger = LogManager.getLogger("Server");

    @Getter
    private PluginManager pluginManager;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel serverChannel;

    private final AtomicBoolean running = new AtomicBoolean(false);
    @Getter
    private int port = 12346;
    private Path pluginsDir = Paths.get("plugins");

    public static void main(String[] args) {
        Server server = Server.getInstance();

        try {
            server.parseArgs(args);
            server.start();
        } catch (Exception e) {
            server.getLogger().fatal("Server failed to start", e);
            System.exit(1);
        }

        server.awaitShutdown();
    }

    private void parseArgs(String[] args) {
        OptionParser parser = new OptionParser();
        parser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(12346);
        parser.accepts("plugins").withRequiredArg().ofType(String.class).defaultsTo("plugins");

        OptionSet options = parser.parse(args);
        this.port = (Integer) options.valueOf("port");
        this.pluginsDir = Paths.get((String) options.valueOf("plugins"));
    }

    public void start() throws Exception {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already running");
        }

        logger.info("Phira Server is starting...");

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));

        initPlugins();
        initNetwork();

        logger.info("Server started successfully on port {}", port);
    }

    private void initPlugins() {
        logger.info("Loading plugins from: {}", pluginsDir.toAbsolutePath());

        pluginManager = new PluginManager(logger, pluginsDir);
        pluginManager.loadAll();

        logger.info("Loaded {} plugin(s)", pluginManager.getPluginCount());
    }

    private void initNetwork() throws InterruptedException, UnknownHostException {
        // 修正：使用 Netty 的 DefaultThreadFactory，创建 FastThreadLocalThread，支持守护线程
        bossGroup = new NioEventLoopGroup(
                1,
                new DefaultThreadFactory("netty-boss", true)  // true = daemon thread
        );

        // 0 表示使用 CPU 核心数 * 2
        workerGroup = new NioEventLoopGroup(
                0,
                new DefaultThreadFactory("netty-worker", true)
        );

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerChannelInitializer());

        InetAddress address = InetAddress.getByName("0.0.0.0");
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(address, port)).sync();

        serverChannel = future.channel();
        logger.info("Listening on: {}:{}", address.getHostAddress(), port);
    }

    public void awaitShutdown() {
        if (serverChannel == null) return;

        try {
            serverChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Server interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        if (!running.compareAndSet(true, false)) return;

        logger.info("Shutting down server...");

        if (pluginManager != null) {
            logger.info("Disabling plugins...");
            pluginManager.disableAll();
        }

        if (serverChannel != null) {
            logger.info("Closing listener...");
            serverChannel.close();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        logger.info("Server stopped");
    }

    public boolean isRunning() {
        return running.get();
    }

    public static void postEvent(Event event){
        getInstance().pluginManager.getEventBus().post(event);
    }

    public static boolean postEvent(CancellableEvent event){
        return getInstance().pluginManager.getEventBus().post(event).isCancelled();
    }

}