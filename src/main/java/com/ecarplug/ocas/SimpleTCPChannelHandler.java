package com.ecarplug.ocas;

import com.ecarplug.ocas.model.ChargerModel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

@SuppressWarnings("ResultOfMethodCallIgnored")
@Slf4j
public class SimpleTCPChannelHandler extends SimpleChannelInboundHandler<ByteBuf>
{
    private final Object _mutexWriteable = new Object();

    private CustomApplication _app = null;
    private ChargerCommand _cmd = null;

    public SimpleTCPChannelHandler()
    {
        if (_app == null) { _app = Main.getApplication(); }
        if (_cmd == null) { _cmd = _app.getCommand(); }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        log.info("[" + ctx.channel().remoteAddress() + "] " + "Channel Active, Concurrent Channel : " + (_app.getClientMap().size() + 1));

		if (_app == null) { _app = Main.getApplication(); }
		if (_cmd == null) { _cmd = _app.getCommand(); }

		String channelID = ctx.channel().id().toString();
		ChargerModel charger = null;

		if (_app.getClientMap().containsKey(channelID))
		{
			charger = _app.getClientMap().get(channelID);
		}
		else
		{
			charger = new ChargerModel(ctx.channel());

			// sbNote 연결 시간
			charger.setConnectedTime(System.currentTimeMillis());

			// InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
			// InetAddress inetaddress = socketAddress.getAddress();
			// String ipAddress = inetaddress.getHostAddress(); // IP address of client
			// charger.setRemoteAddress(ipAddress);
			charger.setRemoteAddress(ctx.channel().remoteAddress().toString().replace("/", ""));
			_app.getClientMap().put(channelID, charger);
		}
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        // sbTodo _app.getClientMap() 정리하는 코드 추가
		String channelID = ctx.channel().id().toString();
		_app.getClientMap().remove(channelID);

		log.info("[" + ctx.channel().remoteAddress() + "] " + "Channel Inactive, Concurrent Channel : " + _app.getClientMap().size());
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
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception
    {
        if (_app == null) { _app = Main.getApplication(); }
        if (_cmd == null) { _cmd = _app.getCommand(); }

        String channelID = ctx.channel().id().toString();
        ChargerModel charger = null;

        if (_app.getClientMap().containsKey(channelID))
        {
            charger = _app.getClientMap().get(channelID);
        }
        else
        {
            charger = new ChargerModel(ctx.channel());

			// InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
			// InetAddress inetaddress = socketAddress.getAddress();
			// String ipAddress = inetaddress.getHostAddress(); // IP address of client
            // charger.setRemoteAddress(ipAddress);
			charger.setRemoteAddress(ctx.channel().remoteAddress().toString().replace("/", ""));

            _app.getClientMap().put(channelID, charger);
        }

        byte[] dataValueByte;
        int length = buf.readableBytes();

        if (buf.hasArray())
        {
            dataValueByte = buf.array();
        }
        else
        {
            dataValueByte = new byte[length];
            buf.getBytes(buf.readerIndex(), dataValueByte);
        }

        // int i = 0;

        synchronized (_mutexWriteable)
        {
            /**
             * 수신된 패킷이 SOH로 시작해서 EOT로 끝나도록 tmpBuffer에 데이터를 저장
             * 완성된 패킷검사 (Checksum, 데이터 길이, 최근 명령어와 동일한지, 충전소, 충전기 ID 비교)
             * 검사완료된 패킷에서 명령어 추출하여 해당되는 수신함수를 호출
             */
            if (charger.tmpBuffer == null)
            {
                charger.tmpBuffer = new byte[(1024 * 4)];   // MAX 4K
            }

            if (_app.isDebugLog())
            {
                ChargerCommandUtil.printByte(dataValueByte, String.format("[READ Len] %d, [Temp Buffer Len] %d", dataValueByte.length, charger.tmpBufferIdx));
            }

            int intML = -1;

            // [INS] f, [복사중인 패킷의 ML Length] : 13
            // Next Buffer find : 25
            // [임시버퍼], index : 25, recv : 70 ===> 01, 01, D9, 53, E5, 01, 66, 00, 0D, 00, 00, 01, 00, 00, 00, 73, 0A, 1E, 0D, 2C, 00, 1F, 4A, 2C, 04, 01, 01, D9, 53, E5, 01, 68, 00, 21, 00, 00, 01, 00, 00, 00, 73, F0, F0, F0, F0, 0F, 0F, 0F, 0F, 70, 01, 01, 00, 2D, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, E8, C0, 04
            // [완성된 패킷] 길이 : 25 ===>           01, 01, D9, 53, E5, 01, 66, 00, 0D, 00, 00, 01, 00, 00, 00, 73, 0A, 1E, 0D, 2C, 00, 1F, 4A, 2C, 04
            // 다음 패킷 조립을 준비합니다. ===> 01, 01, D9, 53, E5, 01, 68, 00, 21, 00, 00, 01, 00, 00, 00, 73, F0, F0, F0, F0, 0F, 0F, 0F, 0F, 70, 01, 01, 00, 2D, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, E8, C0, 04
            //
            //
            // [임시버퍼], index : 90, recv : 45 ===> 01, 01, D9, 53, E5, 01, 68, 00, 21, 00, 00, 01, 00, 00, 00, 73, F0, F0, F0, F0, 0F, 0F, 0F, 0F, 70, 01, 01, 00, 2D, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, E8, C0, 04
            // [패킷 조립중] tmpBufferIdx : 90, 수신된 버퍼 크기 : 45
            // [INS] h, [조립중인 패킷의 ML Length] : 33
            // [임시버퍼], index : 135, recv : 45 ===> 01, 01, D9, 53, E5, 01, 68, 00, 21, 00, 00, 01, 00, 00, 00, 73, F0, F0, F0, F0, 0F, 0F, 0F, 0F, 70, 01, 01, 00, 2D, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, E8, C0, 04
            // [패킷 조립중] tmpBufferIdx : 135, 수신된 버퍼 크기 : 45
            //

            for (int idx = 0; idx < dataValueByte.length; idx++)
            {
                // sbNote 수신 데이터를 임시 버퍼에 저장
                charger.tmpBuffer[charger.tmpBufferIdx++] = dataValueByte[idx];

                // sbNote 수신받은 패킷의 페이로드 크기 확인
                if (intML == -1 && charger.tmpBufferIdx >= 9)
                {
                    byte[] ML = Arrays.copyOfRange(charger.tmpBuffer, 7, 9);
                    intML = ChargerCommandUtil.hex2Number(ML);

                    if (_app.isDebugLog())
                    {
                        log.trace(String.format("[INS] %c, [Packet Length] %d, [ML] %d", charger.tmpBuffer[6], (intML + ChargerCommand.HEADER_TAIL_LEN), intML));
                    }
                }

                if ((intML + ChargerCommand.HEADER_TAIL_LEN) == charger.tmpBufferIdx
                        && charger.tmpBuffer[charger.tmpBufferIdx - 1] == ChargerCommand.EOT
                        && charger.tmpBuffer[0] == ChargerCommand.SOH)
                {
                    // sbNote 패킷 처리하기 위한 전송
                    byte[] recvPacket = Arrays.copyOf(charger.tmpBuffer, intML + ChargerCommand.HEADER_TAIL_LEN);

                    if (_app.isDebugLog())
                    {
                        ChargerCommandUtil.printByte(recvPacket, String.format("[INS] %c, [완성된 패킷] 길이 %d", charger.tmpBuffer[6], (intML + ChargerCommand.HEADER_TAIL_LEN)));
                    }

                    long packetGroupId = System.currentTimeMillis();

                    // sbNote 마지막 패킷 수신 시간 변경
                    charger.setLastPacketTime(packetGroupId);

                    _cmd.processRecvPacket(charger, recvPacket, packetGroupId).subscribe(success -> {
                    }, reject -> {
                        log.error(reject.getMessage());
                    });

                    // sbNote 변수 초기화
                    charger.tmpBufferIdx = 0;
                    charger.tmpBuffer = new byte[(1024 * 4)];   // MAX 4K
                    intML = -1;
                }
            }
        }
    }

    // @Override
    // protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception
    // {
    //     if (_app == null) { _app = Main.getApplication(); }
    //     if (_cmd == null) { _cmd = _app.getCommand(); }
    //
    //
    //     String channelID = ctx.channel().id().toString();
    //     ChargerModel charger = null;
    //
    //     if (_app.getClientMap().containsKey(channelID))
    //     {
    //         charger = _app.getClientMap().get(channelID);
    //     }
    //     else
    //     {
    //         charger = new ChargerModel(ctx.channel());
    //         _app.getClientMap().put(channelID, charger);
    //     }
    //
    //     byte[] dataValueByte;
    //     // int offset;
    //     int length = buf.readableBytes();
    //
    //     if (buf.hasArray())
    //     {
    //         dataValueByte = buf.array();
    //         // offset = buf.arrayOffset();
    //     }
    //     else
    //     {
    //         dataValueByte = new byte[length];
    //         buf.getBytes(buf.readerIndex(), dataValueByte);
    //         // offset = 0;
    //     }
    //
    //     int i = 0;
    //
    //     //
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
    //     synchronized (_mutexWriteable)
    //     {
    //         /**
    //          * 수신된 패킷이 SOH로 시작해서 EOT로 끝나도록 tmpBuffer에 데이터를 저장
    //          * 완성된 패킷검사 (Checksum, 데이터 길이, 최근 명령어와 동일한지, 충전소, 충전기 ID 비교)
    //          * 검사완료된 패킷에서 명령어 추출하여 해당되는 수신함수를 호출
    //          */
    //         if (charger.tmpBuffer == null)
    //         {
    //             charger.tmpBuffer = new byte[(1024 * 4)];   // MAX 4K
    //         }
    //
    //         int intML = 0;
    //         if (charger.tmpBufferIdx >= 9)
    //         {
    //             byte[] ML = Arrays.copyOfRange(charger.tmpBuffer, 7, 9);
    //             intML = ChargerCommandUtil.hex2Number(ML);
    //             if (_app.isDebugLog()) { log.debug(String.format("[INS] %c, ", charger.tmpBuffer[6]) + "[조립중인 패킷의 ML Length] : " + intML); }
    //         }
    //
    //         int nextPacketIdx = -1;
    //         for (int idx = 0; idx < dataValueByte.length; idx++)
    //         {
    //             if (charger.tmpBufferIdx > 12
    //                     && (intML + 12) == charger.tmpBufferIdx
    //                     && idx > 0 && dataValueByte[idx - 1] == ChargerCommand.EOT
    //                     && dataValueByte[idx] == ChargerCommand.SOH)
    //             {
    //                 log.debug("Next Buffer find : " + idx);
    //                 nextPacketIdx = idx;
    //                 break;
    //             }
    //             else
    //             {
    //                 charger.tmpBuffer[charger.tmpBufferIdx++] = dataValueByte[idx];
    //
    //                 if (intML == 0 && charger.tmpBufferIdx >= 9)
    //                 {
    //                     byte[] ML = Arrays.copyOfRange(charger.tmpBuffer, 7, 9);
    //                     intML = ChargerCommandUtil.hex2Number(ML);
    //                     if (_app.isDebugLog()) { log.debug(String.format("[INS] %c, ", charger.tmpBuffer[6]) + "[복사중인 패킷의 ML Length] : " + intML); }
    //                 }
    //             }
    //         }
    //
    //         if (_app.isDebugLog())
    //         {
    //             // ChargerCommandUtil.printByte(dataValueByte, "tmpBuffer assemble" + ", index : " + charger.tmpBufferIdx + ", read : " + dataValueByte.length);
    //             ChargerCommandUtil.printByte(dataValueByte, "[임시버퍼], index : " + charger.tmpBufferIdx + ", recv : " + dataValueByte.length);
    //         }
    //
    //         // if (intML == 0 && charger.tmpBufferIdx >= 9)
    //         // {
    //         //     byte[] ML = Arrays.copyOfRange(charger.tmpBuffer, 7, 9);
    //         //     intML = ChargerCommandUtil.hex2Number(ML);
    //         //     if (_app.isDebugLog()) { log.debug(String.format("[INS] %c, ", charger.tmpBuffer[6]) + "[복사된 패킷의 ML Length] : " + intML); }
    //         // }
    //
    //         // if ((intML + 12) == charger.tmpBufferIdx
    //         //         && charger.tmpBuffer[0] == ChargerCommand.SOH
    //         //         && charger.tmpBuffer[charger.tmpBufferIdx - 1] == ChargerCommand.EOT)
    //         // {
    //         //     charger.setCompletedPacket(true);
    //         // }
    //
    //         if ((intML + 12) == charger.tmpBufferIdx
    //                 && charger.tmpBuffer[0] == ChargerCommand.SOH
    //                 && charger.tmpBuffer[charger.tmpBufferIdx - 1] == ChargerCommand.EOT)
    //         {
    //             charger.setCompletedPacket(true);
    //         }
    //
    //         if (charger.isCompletedPacket())
    //         {
    //             // Message Length와 실제 길이 비교
    //             int recvML = (charger.tmpBuffer[8] & 0x000000FF) + ((charger.tmpBuffer[7] << 8) & 0x0000FF00);   // + ((buffer[3] << 16) & 0x00FF0000) + ((buffer[4] << 24) & 0xFF000000);
    //             int packetLength = 9 + 3 + recvML;
    //
    //             byte[] recvPacket = Arrays.copyOf(charger.tmpBuffer, packetLength);
    //             charger.tmpBufferIdx = 0;
    //             charger.tmpBuffer = null;
    //             charger.setCompletedPacket(false);
    //
    //             if (_app.isDebugLog())
    //             {
    //                 ChargerCommandUtil.printByte(recvPacket, String.format("[INS] %c, ", charger.tmpBuffer[6]) + "[완성된 패킷] 길이 : " + packetLength);
    //             }
    //
    //             if (nextPacketIdx != -1)
    //             {
    //                 charger.tmpBuffer = new byte[(1024 * 4)];   // MAX 4K
    //
    //                 for (int idx = nextPacketIdx; idx < dataValueByte.length; idx++)
    //                 {
    //                     // if (dataValueByte[idx] != SOH && dataValueByte[idx] != EOT)
    //                     charger.tmpBuffer[charger.tmpBufferIdx++] = dataValueByte[idx];
    //                 }
    //
    //                 ChargerCommandUtil.printByte(Arrays.copyOf(charger.tmpBuffer, charger.tmpBufferIdx), "다음 패킷 조립을 준비합니다.");
    //             }
    //
    //             long packetGroupId = System.currentTimeMillis();
    //
    //             charger.setLastPacketTime(packetGroupId);
    //
    //             _cmd.processRecvPacket(charger, recvPacket, packetGroupId).subscribe(success -> {
    //             }, reject -> {
    //                 log.error(reject.getMessage());
    //             });
    //         }
    //         else
    //         {
    //             log.debug("[패킷 조립중] tmpBufferIdx : " + charger.tmpBufferIdx + ", 수신된 버퍼 크기 : " + dataValueByte.length);
    //         }
    //     }
    // }
}