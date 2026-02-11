package top.rymc.phira.main.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import top.rymc.phira.main.Server;
import top.rymc.phira.main.network.handler.AuthenticateHandler;
import top.rymc.phira.main.network.haproxy.HAProxyHandshakeHandler;
import top.rymc.phira.protocol.codec.decoder.FrameDecoder;
import top.rymc.phira.protocol.codec.decoder.HandshakeDecoder;
import top.rymc.phira.protocol.codec.decoder.ServerPacketDecoder;
import top.rymc.phira.protocol.codec.encoder.FrameEncoder;
import top.rymc.phira.protocol.codec.encoder.ServerPacketEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ServerChannelInitializer extends ChannelInitializer<Channel> {
    private final ChannelGroup allChannels;

    public ServerChannelInitializer(ChannelGroup allChannels) {
        this.allChannels = allChannels;
    }

    @Override
    protected void initChannel(Channel channel) {
        allChannels.add(channel);

        InetSocketAddress originalRemoteAddress = (InetSocketAddress) channel.remoteAddress();
        if (!Server.getInstance().getArgs().isProxyProtocol()) {
            initChannel0(channel, originalRemoteAddress);
            return;
        }

        channel.pipeline().addLast(new HAProxyMessageDecoder());

        HAProxyHandshakeHandler haProxyHandler = new HAProxyHandshakeHandler();
        channel.pipeline().addLast(haProxyHandler);

        haProxyHandler.getRealAddress().whenComplete((remoteAddress, throwable) -> {
            if (throwable != null) {
                System.out.printf("Disconnecting %s: %s%n", originalRemoteAddress, throwable.getMessage());
                if (channel.isActive()) {
                    channel.close();
                }
                return;
            }

            initChannel0(channel, remoteAddress);
        });

    }

    private void initChannel0(Channel channel, InetSocketAddress remoteAddress) {
        String ipPort = remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();

        System.out.printf("Establishing a connection from %s%n",ipPort);

        HandshakeDecoder handshake = new HandshakeDecoder();
        channel.pipeline().addLast(handshake);

        handshake.getClientProtocolVersion().whenComplete((version,throwable) -> {
            if (throwable != null) {
                System.out.printf("Disconnecting %s: %s%n",ipPort,throwable.getMessage());
                if (channel.isActive()) {
                    channel.close();
                }
                return;
            }

            System.out.printf("Receive client version %s from %s%n",version,ipPort);

            channel.pipeline()
                    .addLast(new FrameDecoder())
                    .addLast(new FrameEncoder())
                    .addLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                    .addLast(new ServerPacketDecoder())
                    .addLast(new ServerPacketEncoder());

            PlayerConnection connection = new PlayerConnection(channel, remoteAddress);
            connection.setPacketHandler(new AuthenticateHandler(connection));
            channel.pipeline().addLast(connection);
        });
    }
}
