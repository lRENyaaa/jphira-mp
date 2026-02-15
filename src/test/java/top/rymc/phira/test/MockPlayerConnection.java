package top.rymc.phira.test;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import top.rymc.phira.main.network.PlayerConnection;

import java.net.InetSocketAddress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MockPlayerConnection {

    private MockPlayerConnection() {}

    public static PlayerConnection createMock() {
        return createMock(true);
    }

    public static PlayerConnection createMock(boolean active) {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(active);

        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        when(channel.pipeline()).thenReturn(pipeline);

        return new PlayerConnection(channel, new InetSocketAddress("127.0.0.1", 12345));
    }

    public static PlayerConnection createMock(String host, int port) {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);

        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        when(channel.pipeline()).thenReturn(pipeline);

        return new PlayerConnection(channel, new InetSocketAddress(host, port));
    }
}
