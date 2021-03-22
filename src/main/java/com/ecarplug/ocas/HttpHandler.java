package com.ecarplug.ocas;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.netty.singletone.UserWebSocketMultiMap;

import com.ecarplug.ocas.model.ChargerModel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

import static com.ecarplug.ocas.ChargerCommand.*;

@Slf4j
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpMessage>
{
	private CustomApplication _app = null;
	private ChargerCommand _cmd = null;

	public HttpHandler()
	{
		if (_app == null) { _app = Main.getApplication(); }
		if (_cmd == null) { _cmd = _app.getCommand(); }
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws Exception
	{
		ConcurrentMap<String, ConcurrentMap<String, ChannelHandlerContext>> userChannelMap = _app.getUserChannelMap(); // UserWebSocketMultiMap.getInsatance();

		HttpRequest httpRequest = null;

		if (msg instanceof HttpRequest)
		{
			httpRequest = (HttpRequest) msg;
		}

		String uri = null;
		if (httpRequest != null)
		{
			uri = httpRequest.uri();
			Map<String, String> paramMap = paramGetter(uri);

			String targetStationId = paramMap.get("sid");
			String targetChargerId = paramMap.get("cid");
			ChargerModel findCharger = null;

			Set<Map.Entry<String, ChargerModel>> entrySet = _app.getClientMap().entrySet();

			for (Map.Entry<String, ChargerModel> entry : entrySet)
			{
				String key = entry.getKey();
				ChargerModel valueCharger = (ChargerModel) entry.getValue();

				if (targetStationId.equals(valueCharger.getStrStationId()) && targetChargerId.equals(valueCharger.getStrChargerId()))
				{
					findCharger = valueCharger;
					break;
				}
			}

			if (findCharger != null)
			{
				// http://office.sbsoft.net:8084?
				// sid=31020005&
				// cid=01&
				// channel=0&
				// card=2222333344445555&
				// mtype=0&
				// me_price=400&
				// point=0
				// p2=0
				// p3=0&
				// p4=0&
				// p5=0&
				// p6=0&

				if (!findCharger.getChannel().isOpen())
				{
					response(ctx, "{\"result\":0, \"msg\":\"충전기 정보를 찾을수 없습니다\"}");
					return;
				}

				// sbTodo 충전기로 패킷 전송
				int resp_code = RESP_0;
				byte[] payload;
				byte[] sendPacket;
				int payloadIdx = 0;
				try
				{
					// Date currentDT = new Date();
					// SimpleDateFormat transFormat = new SimpleDateFormat("yyMMddHHmmss");
					// String strCurrentDT = transFormat.format(currentDT);

					String ins = paramMap.get("ins");

					if (ins.equals("G"))
					{
						int channel = Integer.parseInt(paramMap.get("channel"));

						// payload
						payload = new byte[1];

						// sbNote 채널
						payload[0] = ChargerCommandUtil.number2Hex(channel, 2)[1];

						sendPacket = _cmd.assemblePacket(findCharger.getByteStationId(), findCharger.getByteChargerId(), payload, INS_OCAS_2_CHARGER_G);

						String sendPayloadParsing = String.format("채널 : %s\n", channel);

						if (_app.isDebugLog())
						{
							log.debug("[" + findCharger.getRemoteAddress() + "] " + String.format("[%s-%s] ", findCharger.getStrStationId(), findCharger.getStrChargerId()) + "[Parsing SEND G]\n" + sendPayloadParsing);
						}

						// sbNote 충전기 상태를 관리서버로 전송
						// _cmd.sendChargerState(findCharger, "G", chargerStatus, -1, -1, chargerKWH, -1, -1);

						int final_resp_code = resp_code;

						// sbNote 충전기로부터 전달받은 응답을 웹서버로 전송해야함...
						findCharger.setHttpCtx(ctx);

						ChargerModel finalFindCharger = findCharger;

						long packetGroupId = System.currentTimeMillis();
						finalFindCharger.setHttpHandlerPacketGroupId(packetGroupId);

						// // sbNote 마지막 패킷 수신 시간 변경
						// charger.setLastPacketTime(packetGroupId);

						// finalFindCharger.setHttpResponseAvailable(false);
						// finalFindCharger.setHttpCtx(null);

						_cmd.sendChargerToOcas(findCharger, sendPacket).subscribe(sendSuccess -> {
							// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
							// sbTodo 수신된 패킷의 정보를 서버로 전송
							// String sendPayloadParsing = String.format("응답코드 : %s\n", final_resp_code);
							//
							// if (_app.isDebugLog()) { log.debug(String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND h]\n" + sendPayloadParsing); }

							// sbTodo 수신된 패킷의 정보를 서버로 전송
							_cmd.sendPacketLog(finalFindCharger, Ocas2Charger, SEND, packetGroupId, "G", sendPayloadParsing, sendPacket)
									.subscribeOn(Schedulers.io())
									.subscribe();

						}, sendError -> {
							// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리

							response(ctx, "{\"result\":0, \"msg\":\"정보 전송 실패\"}");
						});
					}
					else if (ins.equals("H"))
					{
						int channel = Integer.parseInt(paramMap.get("channel"));
						String orderNo = paramMap.get("card");
						int memberType = Integer.parseInt(paramMap.get("mtype"));
						int nonMemberPrice = Integer.parseInt(paramMap.get("me_price"));
						int point = Integer.parseInt(paramMap.get("point"));
						int p2 = Integer.parseInt(paramMap.get("p2"));
						int p3 = Integer.parseInt(paramMap.get("p3"));
						int p4 = Integer.parseInt(paramMap.get("p4"));
						int p5 = Integer.parseInt(paramMap.get("p5"));
						int p6 = Integer.parseInt(paramMap.get("p6"));

						// payload
						payload = new byte[38];

						// sbNote 채널
						payload[0] = ChargerCommandUtil.number2Hex(channel, 2)[1];
						payloadIdx++;

						// sbNote 회원번호 주문번호
						byte[] byteOrderNo = ChargerCommandUtil.orderNo2Byte(orderNo);
						payload[payloadIdx++] = byteOrderNo[0];
						payload[payloadIdx++] = byteOrderNo[1];
						payload[payloadIdx++] = byteOrderNo[2];
						payload[payloadIdx++] = byteOrderNo[3];
						payload[payloadIdx++] = byteOrderNo[4];
						payload[payloadIdx++] = byteOrderNo[5];
						payload[payloadIdx++] = byteOrderNo[6];
						payload[payloadIdx++] = byteOrderNo[7];

						// sbNote 충전 요구 시작 시간
						// Date currentDT = new Date();
						// SimpleDateFormat transFormat = new SimpleDateFormat("yyMMddHHmmss");
						// TimeZone tz;
						// tz = TimeZone.getTimeZone("Asia/Seoul");
						// transFormat.setTimeZone(tz);
						// String strCurrentDT = transFormat.format(currentDT);
						// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(0, 2), 16);
						// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(2, 4), 16);
						// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(4, 6), 16);
						// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(6, 8), 16);
						// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(8, 10), 16);
						// payload[payloadIdx++] = Byte.parseByte(strCurrentDT.substring(10, 12), 16);

						// payload[0] = 0x0;
						// payload[1] = 0x0;
						// payload[2] = 0x0;
						// payload[3] = 0x0;
						// payload[4] = 0x0;
						// payload[5] = 0x0;

						payload[payloadIdx++] = 0x0; // Byte.parseByte("0", 16);
						payload[payloadIdx++] = 0x0; // Byte.parseByte("0", 16);
						payload[payloadIdx++] = 0x0; // Byte.parseByte("0", 16);
						payload[payloadIdx++] = 0x0; // Byte.parseByte("0", 16);
						payload[payloadIdx++] = 0x0; // Byte.parseByte("0", 16);
						payload[payloadIdx++] = 0x0; // Byte.parseByte("0", 16);

						// sbNote 충전요구 전력량
						byte[] byteP3 = ChargerCommandUtil.number2Hex(p3, 4);
						System.arraycopy(byteP3, 0, payload, payloadIdx, byteP3.length);
						payloadIdx += byteP3.length;

						// sbNote 충전요구 시간
						byte[] byteP4 = ChargerCommandUtil.number2Hex(p4, 4);
						System.arraycopy(byteP4, 0, payload, payloadIdx, byteP4.length);
						payloadIdx += byteP4.length;

						// sbNote 충전요구 금액
						byte[] byteP5 = ChargerCommandUtil.number2Hex(p5, 4);
						System.arraycopy(byteP5, 0, payload, payloadIdx, byteP5.length);
						payloadIdx += byteP5.length;

						// sbNote 충전요구 전류량
						byte[] byteP6 = ChargerCommandUtil.number2Hex(p6, 4);
						System.arraycopy(byteP6, 0, payload, payloadIdx, byteP6.length);
						payloadIdx += byteP6.length;

						// sbNote 회원 구분
						payload[payloadIdx++] = ChargerCommandUtil.number2Hex(memberType, 2)[1];

						// sbNote 비회원 충전 단가
						byte[] byteNonMemberPrice = ChargerCommandUtil.number2Hex(nonMemberPrice, 2);
						System.arraycopy(byteNonMemberPrice, 0, payload, payloadIdx, byteNonMemberPrice.length);
						payloadIdx += byteNonMemberPrice.length;

						// sbNote 회원 잔액
						byte[] byteUserPoint = ChargerCommandUtil.number2Hex(point, 4);
						System.arraycopy(byteUserPoint, 0, payload, payloadIdx, byteUserPoint.length);
						payloadIdx += byteUserPoint.length;

						sendPacket = _cmd.assemblePacket(findCharger.getByteStationId(), findCharger.getByteChargerId(), payload, INS_OCAS_2_CHARGER_H);

						String sendPayloadParsing = String.format("채널 : %s\n", channel) +
								String.format("회원번호 : %s\n", orderNo) +
								String.format("충전 요구 시작 시간 : %s\n", "0") +
								String.format("충전 요구 전력량 : %s\n", "0") +
								String.format("충전 요구 시간 : %s\n", "0") +
								String.format("충전 요구 금액 : %s\n", "0") +
								String.format("충전 요구 전력량 : %s\n", "0") +
								String.format("회원구분 : %s\n", memberType) +
								String.format("비회원 단가 : %s\n", nonMemberPrice) +
								String.format("회원 잔액 : %s\n", point);

						if (_app.isDebugLog())
						{
							log.debug("[" + findCharger.getRemoteAddress() + "] " + String.format("[%s-%s] ", findCharger.getStrStationId(), findCharger.getStrChargerId()) + "[Parsing SEND H]\n" + sendPayloadParsing);
						}

						// sbNote 충전기 상태를 관리서버로 전송
						// _cmd.sendChargerState(charger, "h", chargerStatus, -1, -1, chargerKWH, -1, -1);

						int final_resp_code = resp_code;

						// sbNote 충전기로부터 전달받은 응답을 웹서버로 전송해야함...
						findCharger.setHttpCtx(ctx);

						ChargerModel finalFindCharger = findCharger;

						long packetGroupId = System.currentTimeMillis();
						finalFindCharger.setHttpHandlerPacketGroupId(packetGroupId);

						_cmd.sendChargerToOcas(findCharger, sendPacket).subscribe(sendSuccess -> {
							// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
							// sbTodo 수신된 패킷의 정보를 서버로 전송
							// String sendPayloadParsing = String.format("응답코드 : %s\n", final_resp_code);
							//
							// if (_app.isDebugLog()) { log.debug(String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND h]\n" + sendPayloadParsing); }

							// sbTodo 수신된 패킷의 정보를 서버로 전송
							_cmd.sendPacketLog(finalFindCharger, Ocas2Charger, SEND, packetGroupId, "H", sendPayloadParsing, sendPacket)
									.subscribeOn(Schedulers.io())
									.subscribe();

						}, sendError -> {
							// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
							// response(ctx, "{\"result\":0, \"msg\":\"정보 전송 실패\"}");
						});

						// // sbNote 충전시작 명령어 전송 성공
						// response(ctx, "{\"result\":100, \"msg\":\"정상처리\"}");
					}
				}
				catch (Exception ex)
				{
					log.info("sendChargerToOcasH : " + ex.getMessage());
				}
			}
			else
			{
				response(ctx, "{\"result\":0, \"msg\":\"충전기 정보를 찾을수 없습니다\"}");
			}
		}

		// String userId = paramMap.get("userId");

		// ObjectMapper om = new ObjectMapper();
		// String jsonResult = URLDecoder.decode(om.writeValueAsString(paramMap), "UTF-8");

		// if (userChannelMap.containsKey(userId))
		// {
		// 	ConcurrentMap<String, ChannelHandlerContext> channelMap = userChannelMap.get(userId);
		//
		// 	Set<String> keySets = channelMap.keySet();
		//
		// 	for (String key : keySets)
		// 	{
		// 		WebSocketFrame wsf = new TextWebSocketFrame(jsonResult);
		//
		// 		ChannelHandlerContext chc = channelMap.get(key);
		// 		// Event send to next event handler
		// 		chc.fireChannelActive();
		//
		// 		chc.channel().writeAndFlush(wsf);
		//
		// 	}
		// }
	}


	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
	{
		ctx.flush();
	}


	private void response(ChannelHandlerContext ctx, String responseJsonBody)
	{
		// ByteBuf content = Unpooled.copiedBuffer("{\"result\":100, \"msg\":\"정상처리\"}", CharsetUtil.UTF_8);
		ByteBuf content = Unpooled.copiedBuffer(responseJsonBody, CharsetUtil.UTF_8);

		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.OK,
				content/*Unpooled.copiedBuffer("success", CharsetUtil.UTF_8)*/);

		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8");
		response.headers().set("Access-Control-Allow-Origin", "null");
		response.headers().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
		response.headers().set("Access-Control-Max-Age", "3600");
		response.headers().set("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With, remember-me");
		response.headers().set("Access-Control-Allow-Credentials", "true");
		response.headers().set("Access-Control-Expose-Headers", "Access-Control-Allow-Origin,Access-Control-Allow-Credentials");

		// http 통신을 종료 하려면 ChannelFutureListener의 Close로 연결을 종료 시켜주어야 한다.
		ctx.write(response).addListener(ChannelFutureListener.CLOSE);
	}


	private Map<String, String> paramGetter(String uri) throws UnsupportedEncodingException
	{
		Map<String, String> paramMap = new HashMap<>();
		String params = uri.substring(uri.indexOf("?") + 1, uri.length());
		String[] paramsArray = params.split("&");

		for (String param : paramsArray)
		{
			String[] paramArray = param.split("=");

			if (paramArray[0].equals("content"))
			{
				String content = URLDecoder.decode(paramArray[1], "UTF-8");

				paramMap.put(paramArray[0], content);
			}

			paramMap.put(paramArray[0], paramArray[1]);
		}

		return paramMap;
	}
}
