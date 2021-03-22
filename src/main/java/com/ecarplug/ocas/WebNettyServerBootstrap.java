package com.ecarplug.ocas;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class WebNettyServerBootstrap
{
    // void start(int port) throws InterruptedException
    // {
    //     log.info("[for web] Starting server at : " + port);
    //
    //     EventLoopGroup bossGroup = new NioEventLoopGroup();
    //     EventLoopGroup workerGroup = new NioEventLoopGroup();
    //
    //     try
    //     {
    //         ServerBootstrap b = new ServerBootstrap();
    //         b.group(bossGroup, workerGroup)
    //                 .channel(NioServerSocketChannel.class)
    //                 .childHandler(new WebTCPChannelInitializer())
    //                 .childOption(ChannelOption.SO_KEEPALIVE, true);
    //
    //
    //         // Bind and start to accept incoming connections.
    //         ChannelFuture f = b.bind(port).sync();
    //         if (f.isSuccess())
    //         {
    //             log.info("[for web] Server started successfully");
    //         }
    //
    //         f.channel().closeFuture().sync();
    //
    //         // List<Integer> ports = Arrays.asList(8080, 8081);
    //         // Collection<Channel> channels = new ArrayList<>(ports.size());
    //         // for (int port : ports) {
    //         //     Channel serverChannel = bootstrap.bind(port).sync().channel();
    //         //     channels.add(serverChannel);
    //         // }
    //         // for (Channel ch : channels) {
    //         //     ch.closeFuture().sync();
    //         // }
    //     }
    //     finally
    //     {
    //         log.info("[for web] Stopping server");
    //         workerGroup.shutdownGracefully();
    //         bossGroup.shutdownGracefully();
    //     }
    // }
}