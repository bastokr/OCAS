package com.ecarplug.ocas;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class HttpServerInit extends ChannelInitializer<SocketChannel>
{
	private static final CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();

	@Override
	protected void initChannel(SocketChannel ch) throws Exception
	{
		ChannelPipeline pipeLine = ch.pipeline();

		pipeLine.addLast(new HttpRequestDecoder());
		pipeLine.addLast(new HttpObjectAggregator(65536));
		pipeLine.addLast(new CorsHandler(corsConfig));
		pipeLine.addLast(new HttpResponseEncoder());
		pipeLine.addLast(new WebSocketServerProtocolHandler("/"));
		pipeLine.addLast(new HttpHandler());
		// pipeLine.addLast(new WebSocketHandler());
	}

}
