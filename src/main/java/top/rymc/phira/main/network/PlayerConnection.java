package top.rymc.phira.main.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.network.PlayerSwitchPacketHandlerEvent;
import top.rymc.phira.main.event.player.PlayerDisconnectEvent;
import top.rymc.phira.main.event.network.PacketReceiveEvent;
import top.rymc.phira.main.event.network.PacketSendEvent;
import top.rymc.phira.main.game.player.PlayerManager;
import top.rymc.phira.main.util.ThreadFactoryCompat;
import top.rymc.phira.protocol.data.message.ChatMessage;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Getter
public class PlayerConnection extends ChannelInboundHandlerAdapter {

    private final ExecutorService packetExecutor;

    private final Channel channel;
    private final InetSocketAddress remoteAddress;

    private volatile ServerBoundPacketHandler packetHandler;
    private volatile ConnectState connectState = ConnectState.ACTIVE;

    public void setPacketHandler(ServerBoundPacketHandler packetHandler) {
        PlayerSwitchPacketHandlerEvent event = new PlayerSwitchPacketHandlerEvent(this, this.packetHandler, packetHandler);
        Server.postEvent(event);
        this.packetHandler = event.getNewHandler();
    }

    private final List<Consumer<ChannelHandlerContext>> closeHandlers = new CopyOnWriteArrayList<>();

    public PlayerConnection(Channel channel, InetSocketAddress remoteAddress) {
        this.channel = channel;
        this.remoteAddress = remoteAddress;

        this.packetExecutor = Executors.newSingleThreadExecutor(
                ThreadFactoryCompat.THREAD_FACTORY_CREATOR.apply("LocalPlayer-Worker-" + getRemoteAddressAsString())
        );
    }

    public void onClose(Consumer<ChannelHandlerContext> handler) {
        closeHandlers.add(handler);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (packetHandler == null) {
            ctx.close();
            return;
        }

        if (this.isClosed()) {
            return;
        }

        ServerBoundPacket packet = (ServerBoundPacket) msg;

        PacketReceiveEvent event = new PacketReceiveEvent(this, packet);

        if (Server.postEvent(event)) {
            return;
        }

        packetExecutor.execute(() -> handle(ctx, packet));
    }

    @SuppressWarnings("resource")
    private void handle(ChannelHandlerContext ctx, ServerBoundPacket packet) {
        try {
            packet.handle(packetHandler);
        } catch (Throwable t) {
            ctx.channel().eventLoop().execute(() -> ctx.fireExceptionCaught(t));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!ctx.channel().isActive()) {
            return;
        }

        Logger logger = Server.getLogger();

        if (cause instanceof ReadTimeoutException) {
            logger.error("{}: read timed out", getRemoteAddressAsString());
            connectState = ConnectState.TIMEOUT;
        } else if (cause instanceof SocketException) {
            logger.info("{}: {}", getRemoteAddressAsString(), cause.getMessage());
            connectState = ConnectState.ERROR;
        } else {
            logger.atError().withThrowable(cause).log("{}: exception encountered", getRemoteAddressAsString());
            connectState = ConnectState.ERROR;
        }

        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        packetExecutor.shutdownNow();

        Server.getLogger().info("Client disconnected: {}", getRemoteAddressAsString());

        PlayerManager.getPlayer(this).ifPresent(player -> {
            PlayerDisconnectEvent event = new PlayerDisconnectEvent(player, determineDisconnectReason());
            Server.postEvent(event);
        });

        for (Consumer<ChannelHandlerContext> handler : closeHandlers) {
            try {
                handler.accept(ctx);
            } catch (Exception e) {
                Server.getLogger().error("Exception in close handler for connection {}", getRemoteAddressAsString(), e);
            }
        }

        super.channelInactive(ctx);
    }

    private PlayerDisconnectEvent.DisconnectReason determineDisconnectReason() {
        return switch (connectState) {
            case ACTIVE -> PlayerDisconnectEvent.DisconnectReason.QUIT;
            case KICK -> PlayerDisconnectEvent.DisconnectReason.KICK;
            case TIMEOUT -> PlayerDisconnectEvent.DisconnectReason.TIMEOUT;
            case DUPLICATE -> PlayerDisconnectEvent.DisconnectReason.DUPLICATE;
            case ERROR -> PlayerDisconnectEvent.DisconnectReason.ERROR;
        };
    }

    public Optional<ChannelFuture> send(ClientBoundPacket packet) {
        if (this.isClosed()) {
            return Optional.empty();
        }

        PacketSendEvent event = new PacketSendEvent(this, packet);
        if (Server.postEvent(event)) {
            return Optional.empty();
        }

        return Optional.ofNullable(channel.writeAndFlush(packet));
    }

    public void sendChat(String message) {
        this.send(ClientBoundMessagePacket.create(new ChatMessage(-1,message)));
    }

    public boolean isClosed() {
        return !channel.isActive();
    }

    public void close() {
        if (!this.isClosed()) {
            channel.close();
        }
    }

    public String getRemoteAddressAsString() {
        return remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();
    }

    public void markDuplicateLogin() {
        this.connectState = ConnectState.DUPLICATE;
        this.close();
    }

    public void markAsKicked() {
        this.connectState = ConnectState.KICK;
        this.close();
    }

    private enum ConnectState {
        ACTIVE,
        KICK,
        TIMEOUT,
        DUPLICATE,
        ERROR
    }
}
