package top.rymc.phira.main.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.event.PacketReceiveEvent;
import top.rymc.phira.main.event.PacketSendEvent;
import top.rymc.phira.main.event.PlayerSwitchPacketHandlerEvent;
import top.rymc.phira.protocol.data.message.ChatMessage;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ClientBoundPacket;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.clientbound.ClientBoundMessagePacket;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Getter
public class PlayerConnection extends ChannelInboundHandlerAdapter {

    private final Channel channel;
    private final InetSocketAddress remoteAddress;

    private ServerBoundPacketHandler packetHandler;

    public void setPacketHandler(ServerBoundPacketHandler packetHandler) {
        PlayerSwitchPacketHandlerEvent event = new PlayerSwitchPacketHandlerEvent(this, this.packetHandler, packetHandler);
        Server.postEvent(event);
        this.packetHandler = event.getNewHandler();
    }

    private final List<Consumer<ChannelHandlerContext>> closeHandlers = new ArrayList<>();

    public PlayerConnection(Channel channel, InetSocketAddress remoteAddress) {
        this.channel = channel;
        this.remoteAddress = remoteAddress;
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

        packet.handle(packetHandler);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!ctx.channel().isActive()) {
            return;
        }

        ctx.close();

        Logger logger = Server.getLogger();

        if (cause instanceof ReadTimeoutException) {
            logger.error("{}: read timed out", getRemoteAddressAsString());
            return;
        }

        if (cause instanceof SocketException) {
            logger.info("{}: {}", getRemoteAddressAsString(), cause.getMessage());
            return;
        }

        logger.atError().withThrowable(cause).log("{}: exception encountered", getRemoteAddressAsString());


    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Server.getLogger().info("Client disconnected: {}", getRemoteAddressAsString());
        for (Consumer<ChannelHandlerContext> handler : closeHandlers) {
            handler.accept(ctx);
        }

        super.channelInactive(ctx);
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
}
