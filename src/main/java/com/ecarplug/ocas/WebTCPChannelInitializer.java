package com.ecarplug.ocas;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class WebTCPChannelInitializer extends ChannelInitializer<SocketChannel> {

	protected void initChannel(SocketChannel socketChannel) throws Exception
	{
		ChannelPipeline pipeline = socketChannel.pipeline();

		// // 출처: https://balhae79.tistory.com/346 [조영's lab Dev]
		// pipeline.addLast(new ByteToMessageDecoder() {
		// 	@Override
		// 	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
		// 	{
		// 		out.add(in.readBytes(in.readableBytes()));
		// 	}
		// });
        //
		// // pipeline.addLast(new DatagramPacketDecoder(new ByteArrayDecoder()));
		// // pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
		pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
		// // pipeline.addLast(new StringEncoder());
		// // pipeline.addLast(new StringDecoder());

		pipeline.addLast(new WebTCPChannelHandler());
	}
}