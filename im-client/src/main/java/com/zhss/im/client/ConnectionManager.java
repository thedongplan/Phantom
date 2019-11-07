package com.zhss.im.client;


import com.zhss.im.protocol.Constants;
import com.zhss.im.protocol.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 连接管理器
 *
 * @author Jianfeng Wang
 * @since 2019/11/7 18:29
 */
@Slf4j
public class ConnectionManager {

    private static ConnectionManager connectionManager = new ConnectionManager();
    private volatile SocketChannel channel;
    private ArrayBlockingQueue<Message> messages = new ArrayBlockingQueue<>(1000);
    private ThreadPoolExecutor threadPool = null;
    private volatile boolean shutdown = false;
    private EventLoopGroup connectThreadGroup = null;

    private ConnectionManager() {
    }

    public void connect(String ip, int port) {
        log.info("开始和系统发起连接....");
        connectThreadGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(connectThreadGroup)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(4096,
                                    Unpooled.copiedBuffer(Constants.DELIMITER)));
                            ch.pipeline().addLast(new ImClientHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
            channelFuture.sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        threadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), r -> new Thread(r, "Connect-IO-Thread"));
        threadPool.execute(() -> {
            while (!shutdown) {
                try {
                    Message msg = messages.poll(10, TimeUnit.SECONDS);
                    while (channel == null) {
                        Thread.sleep(1000);
                    }
                    if (channel != null && msg != null) {
                        channel.writeAndFlush(msg.getBuffer());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static ConnectionManager getInstance() {
        return connectionManager;
    }


    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    long s = 0;

    public void sendMessage(Message message) {
        s = System.nanoTime();
        messages.add(message);
    }

    public void shutdown() {
        this.shutdown = true;
        if (threadPool != null) {
            this.threadPool.shutdown();
            this.threadPool = null;
        }
        log.info("线程池停止");
        if (this.connectThreadGroup != null) {
            this.connectThreadGroup.shutdownGracefully();
            this.connectThreadGroup = null;
        }
        log.info("停止NIO eventloop");
    }

    public void success() {
        long e = System.nanoTime();

        log.info("认证用时：{}ms", (e - s) / 1000000.0d);

    }
}