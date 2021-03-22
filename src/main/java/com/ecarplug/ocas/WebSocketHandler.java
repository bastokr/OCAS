package com.ecarplug.ocas;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// import com.netty.singletone.UserWebSocketMultiMap;
// import com.netty.singletone.UserWithChannelIdMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame>
{
	private CustomApplication _app = null;

	public WebSocketHandler()
	{
		if (_app == null) { _app = Main.getApplication(); }
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception
	{
		// String initUserId = ((ByteBuf) msg.content()).toString(Charset.defaultCharset());
		//
		// if (this.registryUserChannel(initUserId, ctx))
		// {
		// 	System.out.println("registry");
		// }
		//
		// System.out.println(initUserId + " : " + ctx);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
	{
		ctx.flush();
	}

	// @Override
	// public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
	// {
	// 	ConcurrentMap<String, String> channelIdMap = UserWithChannelIdMap.getInstance();
	// 	ConcurrentMap<String, ConcurrentMap<String, ChannelHandlerContext>> userChannelMap = _app.getUserChannelMap(); // UserWebSocketMultiMap.getInsatance();
	//
	// 	String channelId = ctx.channel().id().toString();
	// 	String userId = "";
	//
	// 	if (channelIdMap.containsKey(channelId))
	// 	{
	// 		userId = channelIdMap.get(channelId);
	// 	}
	//
	// 	if (userChannelMap.containsKey(userId))
	// 	{
	// 		ConcurrentMap<String, ChannelHandlerContext> channelMap = userChannelMap.get(userId);
	//
	// 		channelMap.remove(channelId);
	// 		System.out.println("remove channel");
	// 	}
	//
	// 	super.channelUnregistered(ctx);
	// }
	//
	// /**
	//  * @param userId
	//  * @param chc
	//  * @return boolean
	//  */
	// private boolean registryUserChannel(String userId, ChannelHandlerContext chc)
	// {
	// 	String channelId = chc.channel().id().toString();
	//
	// 	ConcurrentMap<String, String> channelIdMap = UserWithChannelIdMap.getInstance();
	// 	ConcurrentMap<String, ConcurrentMap<String, ChannelHandlerContext>> userChannelMap = _app.getUserChannelMap(); // UserWebSocketMultiMap.getInsatance();
	//
	// 	ConcurrentMap<String, ChannelHandlerContext> channelMap = null;
	//
	// 	if (userChannelMap.get(userId) == null)
	// 	{
	// 		channelMap = new ConcurrentHashMap<>();
	// 	}
	// 	else
	// 	{
	// 		channelMap = userChannelMap.get(userId);
	// 	}
	//
	// 	channelIdMap.put(channelId, userId);
	// 	channelMap.put(channelId, chc);
	//
	// 	userChannelMap.put(userId, channelMap);
	//
	// 	return true;
	// }
	//
	// @Override
	// public void channelRegistered(ChannelHandlerContext ctx) throws Exception
	// {
	// 	return;
	// }
}
