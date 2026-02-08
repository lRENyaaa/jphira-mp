package top.rymc.phira.main.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import top.rymc.phira.main.network.handler.AuthenticateHandler;
import top.rymc.phira.protocol.codec.decoder.FrameDecoder;
import top.rymc.phira.protocol.codec.decoder.HandshakeDecoder;
import top.rymc.phira.protocol.codec.decoder.ServerPacketDecoder;
import top.rymc.phira.protocol.codec.encoder.FrameEncoder;
import top.rymc.phira.protocol.codec.encoder.ServerPacketEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ServerChannelInitializer extends ChannelInitializer<Channel> {
    @Override
    protected void initChannel(Channel channel) {

        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
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
                    .addLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                    .addLast(new ServerPacketDecoder())
                    .addLast(new ServerPacketEncoder());

            PlayerConnection connection = new PlayerConnection(channel, remoteAddress);
            connection.setPacketHandler(new AuthenticateHandler(connection));
            channel.pipeline().addLast(connection);
        });

    }
}
