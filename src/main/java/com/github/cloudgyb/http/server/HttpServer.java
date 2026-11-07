package com.github.cloudgyb.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class HttpServer {
    private final static Logger logger = LoggerFactory.getLogger(HttpServer.class);

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 80;
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossEventLoopGroup = new NioEventLoopGroup();
        NioEventLoopGroup workEventLoopGroup = new NioEventLoopGroup();

        serverBootstrap.channel(NioServerSocketChannel.class)
                .group(bossEventLoopGroup, workEventLoopGroup)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) {
                        nioSocketChannel.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpServerHandler())
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        if (msg instanceof FullHttpRequest request) {
                                            logger.info(request.toString());
                                        } else if (msg instanceof HttpPostRequestDecoder requestDecoder) {
                                            logger.info(requestDecoder.toString());
                                            requestDecoder.destroy();
                                        }
                                        ctx.close();
                                    }
                                });
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 1024)
                .bind(host, port)
                .addListener(future -> {
                    if (future.isSuccess()) {
                        logger.info("Http Server listen at {}:{}", host, port);
                    } else {
                        Throwable cause = future.cause();
                        logger.error("Http Server start failed!", cause);
                        System.exit(1);
                    }
                });
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String next = scanner.next();
            if (next.equals("shutdown")) {
                CountDownLatch countDownLatch = new CountDownLatch(2);
                workEventLoopGroup
                        .shutdownGracefully()
                        .addListener(future -> {
                            if (future.isDone()) {
                                countDownLatch.countDown();
                            }
                        });
                bossEventLoopGroup
                        .shutdownGracefully()
                        .addListener(future -> {
                            if (future.isDone()) {
                                countDownLatch.countDown();
                            }
                        });
                try {
                    countDownLatch.await();
                    logger.info("Http Server stopped!");
                    break;
                } catch (Exception ignore) {
                }
            }
        }
    }
}
