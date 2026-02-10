package top.rymc.phira.main.network.haproxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HAProxyHandshakeHandler extends ChannelInboundHandlerAdapter {

    private final CompletableFuture<InetSocketAddress> realAddressPromise = new CompletableFuture<>();
    private final long timeout;
    private final TimeUnit timeUnit;

    private ScheduledFuture<?> timeoutTask;

    public HAProxyHandshakeHandler() {
        this(5000, TimeUnit.MILLISECONDS);
    }

    public HAProxyHandshakeHandler(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.timeUnit = unit;
    }

    public CompletableFuture<InetSocketAddress> getRealAddress() {
        return realAddressPromise;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        timeoutTask = context.executor().schedule(() -> {
            if (!realAddressPromise.isDone()) {
                realAddressPromise.completeExceptionally(new TimeoutException("Proxy protocol handshake timeout"));
                context.close();
            }
        }, timeout, timeUnit);

        super.channelActive(context);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) {
        if (!(message instanceof HAProxyMessage)) {
            context.fireChannelRead(message);
            return;
        }

        HAProxyMessage haproxyMessage = (HAProxyMessage) message;

        try {
            if (haproxyMessage.sourceAddress() == null) {
                realAddressPromise.completeExceptionally(
                        new IllegalStateException("HAProxyMessage missing source address")
                );
                context.close();
                return;
            }

            InetSocketAddress realAddress = new InetSocketAddress(
                    haproxyMessage.sourceAddress(),
                    haproxyMessage.sourcePort()
            );

            realAddressPromise.complete(realAddress);
        } finally {
            cancelTimeout();
            context.pipeline().remove(this);
            ReferenceCountUtil.release(haproxyMessage);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        cancelTimeout();
        if (!realAddressPromise.isDone()) {
            realAddressPromise.completeExceptionally(new IOException("Connection closed before proxy protocol handshake"));
        }
        super.channelInactive(context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        cancelTimeout();
        if (!realAddressPromise.isDone()) {
            realAddressPromise.completeExceptionally(throwable);
        }
        context.close();
    }

    private void cancelTimeout() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }
    }
}