package com.ecarplug.ocas;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class SimpleNettyServerBootstrap
{
	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();

    // sbNote 충전기만 통신하던 원본 코드
    void start(int port) throws InterruptedException
    {
        try
        {
			log.info("Starting server at : " + port);

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new SimpleTCPChannelInitializer())
					// .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();
            if (f.isSuccess())
            {
                log.info("Server started successfully");
            }


            int pushPort = 8084;
			log.info("Starting push server at : " + pushPort);

			// sbNote reference : https://jodu.tistory.com/14
			ServerBootstrap bPush = new ServerBootstrap();
			bPush.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					// .handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(new HttpServerInit());

			ChannelFuture fPush = bPush.bind(pushPort).sync();
			if (fPush.isSuccess())
			{
				log.info("Server push started successfully");
			}

            f.channel().closeFuture().sync();
			fPush.channel().closeFuture().sync();
        }
        finally
        {
            log.info("Stopping server all");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    // void startPush(int port) throws InterruptedException
	// {
	// 	// EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	// 	// EventLoopGroup workerGroup = new NioEventLoopGroup();
	// 	// ChannelFuture channelFuture = null;
	//
	// 	log.info("Starting push server at : " + port);
	//
	// 	try
	// 	{
	// 		ServerBootstrap b = new ServerBootstrap();
	// 		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
	// 				// .handler(new LoggingHandler(LogLevel.INFO))
	// 				.childHandler(new HttpServerInit());
	//
	// 		Channel ch = b.bind(port).sync().channel();
	//
	// 		ChannelFuture channelFuture = ch.closeFuture();
	// 		channelFuture.sync();
	// 	}
	// 	catch (Exception e)
	// 	{
	// 		e.printStackTrace();
	// 	}
	// 	finally
	// 	{
	// 		log.info("Stopping push server");
	// 		bossGroup.shutdownGracefully();
	// 		workerGroup.shutdownGracefully();
	// 	}
	// }
}