package com.ecarplug.ocas;

import com.ecarplug.ocas.model.ChargerModel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

@SuppressWarnings("ResultOfMethodCallIgnored")
@Slf4j
public class WebTCPChannelHandler extends SimpleChannelInboundHandler<String>
{
    private final Object _mutexWriteable = new Object();

    private CustomApplication _app = null;
    private ChargerCommand _cmd = null;

    public WebTCPChannelHandler()
    {
        if (_app == null) { _app = Main.getApplication(); }
        if (_cmd == null) { _cmd = _app.getCommand(); }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        // log.info("[" + ctx.channel().remoteAddress() + "] " + "Channel Active, Concurrent Channel : " + (_app.getClientMap().size() + 1));
        int i = 0;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        int i = 0;
        // // sbTodo _app.getClientMap() 정리하는 코드 추가
        // log.info("[" + ctx.channel().remoteAddress() + "] " + "Channel Inactive");
        //
        // String channelID = ctx.channel().id().toString();
        // _app.getClientMap().remove(channelID);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
        ctx.flush();
        // ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
    {
        log.info("[" + ctx.channel().remoteAddress() + "] " + "Channel " + cause.getMessage());
        // cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception
    {
        if (_app == null) { _app = Main.getApplication(); }
        if (_cmd == null) { _cmd = _app.getCommand(); }

        try
        {
            JSONObject res = new JSONObject(msg);
            int responseStatus = res.getInt("status");

            if (responseStatus == 200)
            {

            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // String channelID = ctx.channel().id().toString();
        // ChargerModel charger = null;
        //
        // if (_app.getClientMap().containsKey(channelID))
        // {
        //     charger = _app.getClientMap().get(channelID);
        // }
        // else
        // {
        //     charger = new ChargerModel(ctx.channel());
        //     _app.getClientMap().put(channelID, charger);
        // }
        //
        // byte[] dataValueByte;
        // int length = buf.readableBytes();
        //
        // if (buf.hasArray())
        // {
        //     dataValueByte = buf.array();
        // }
        // else
        // {
        //     dataValueByte = new byte[length];
        //     buf.getBytes(buf.readerIndex(), dataValueByte);
        // }
        //
        // // int i = 0;
        //
        // synchronized (_mutexWriteable)
        // {
        //     /**
        //      * 수신된 패킷이 SOH로 시작해서 EOT로 끝나도록 tmpBuffer에 데이터를 저장
        //      * 완성된 패킷검사 (Checksum, 데이터 길이, 최근 명령어와 동일한지, 충전소, 충전기 ID 비교)
        //      * 검사완료된 패킷에서 명령어 추출하여 해당되는 수신함수를 호출
        //      */
        //     if (charger.tmpBuffer == null)
        //     {
        //         charger.tmpBuffer = new byte[(1024 * 4)];   // MAX 4K
        //     }
        //
        //     if (_app.isDebugLog())
        //     {
        //         ChargerCommandUtil.printByte(dataValueByte, String.format("[READ Len] %d, [Temp Buffer Len] %d", dataValueByte.length, charger.tmpBufferIdx));
        //     }
        //
        //     int intML = -1;
        //
        //     // [INS] f, [복사중인 패킷의 ML Length] : 13
        //     // Next Buffer find : 25
        //     // [임시버퍼], index : 25, recv : 70 ===> 01, 01, D9, 53, E5, 01, 66, 00, 0D, 00, 00, 01, 00, 00, 00, 73, 0A, 1E, 0D, 2C, 00, 1F, 4A, 2C, 04, 01, 01, D9, 53, E5, 01, 68, 00, 21, 00, 00, 01, 00, 00, 00, 73, F0, F0, F0, F0, 0F, 0F, 0F, 0F, 70, 01, 01, 00, 2D, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, E8, C0, 04
        //     // [완성된 패킷] 길이 : 25 ===>           01, 01, D9, 53, E5, 01, 66, 00, 0D, 00, 00, 01, 00, 00, 00, 73, 0A, 1E, 0D, 2C, 00, 1F, 4A, 2C, 04
        //     // 다음 패킷 조립을 준비합니다. ===> 01, 01, D9, 53, E5, 01, 68, 00, 21, 00, 00, 01, 00, 00, 00, 73, F0, F0, F0, F0, 0F, 0F, 0F, 0F, 70, 01, 01, 00, 2D, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, E8, C0, 04
        //     //
        //     //
        //     // [임시버퍼], index : 90, recv : 45 ===> 01, 01, D9, 53, E5, 01, 68, 00, 21, 00, 00, 01, 00, 00, 00, 73, F0, F0, F0, F0, 0F, 0F, 0F, 0F, 70, 01, 01, 00, 2D, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, E8, C0, 04
        //     // [패킷 조립중] tmpBufferIdx : 90, 수신된 버퍼 크기 : 45
        //     // [INS] h, [조립중인 패킷의 ML Length] : 33
        //     // [임시버퍼], index : 135, recv : 45 ===> 01, 01, D9, 53, E5, 01, 68, 00, 21, 00, 00, 01, 00, 00, 00, 73, F0, F0, F0, F0, 0F, 0F, 0F, 0F, 70, 01, 01, 00, 2D, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, E8, C0, 04
        //     // [패킷 조립중] tmpBufferIdx : 135, 수신된 버퍼 크기 : 45
        //     //
        //
        //     for (int idx = 0; idx < dataValueByte.length; idx++)
        //     {
        //         // sbNote 수신 데이터를 임시 버퍼에 저장
        //         charger.tmpBuffer[charger.tmpBufferIdx++] = dataValueByte[idx];
        //
        //         // sbNote 수신받은 패킷의 페이로드 크기 확인
        //         if (intML == -1 && charger.tmpBufferIdx >= 9)
        //         {
        //             byte[] ML = Arrays.copyOfRange(charger.tmpBuffer, 7, 9);
        //             intML = ChargerCommandUtil.hex2Number(ML);
        //
        //             if (_app.isDebugLog())
        //             {
        //                 log.trace(String.format("[INS] %c, [Packet Length] %d, [ML] %d", charger.tmpBuffer[6], (intML + ChargerCommand.HEADER_TAIL_LEN), intML));
        //             }
        //         }
        //
        //         if ((intML + ChargerCommand.HEADER_TAIL_LEN) == charger.tmpBufferIdx
        //                 && charger.tmpBuffer[charger.tmpBufferIdx - 1] == ChargerCommand.EOT
        //                 && charger.tmpBuffer[0] == ChargerCommand.SOH)
        //         {
        //             // sbNote 패킷 처리하기 위한 전송
        //             byte[] recvPacket = Arrays.copyOf(charger.tmpBuffer, intML + ChargerCommand.HEADER_TAIL_LEN);
        //
        //             if (_app.isDebugLog())
        //             {
        //                 ChargerCommandUtil.printByte(recvPacket, String.format("[INS] %c, [완성된 패킷] 길이 %d", charger.tmpBuffer[6], (intML + ChargerCommand.HEADER_TAIL_LEN)));
        //             }
        //
        //             long packetGroupId = System.currentTimeMillis();
        //
        //             // sbNote 마지막 패킷 수신 시간 변경
        //             charger.setLastPacketTime(packetGroupId);
        //
        //             _cmd.processRecvPacket(charger, recvPacket, packetGroupId).subscribe(success -> {
        //             }, reject -> {
        //                 log.error(reject.getMessage());
        //             });
        //
        //             // sbNote 변수 초기화
        //             charger.tmpBufferIdx = 0;
        //             charger.tmpBuffer = new byte[(1024 * 4)];   // MAX 4K
        //             intML = -1;
        //         }
        //     }
        // }
    }
}