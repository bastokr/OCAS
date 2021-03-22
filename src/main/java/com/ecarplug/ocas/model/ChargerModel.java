package com.ecarplug.ocas.model;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ChargerModel
{
    // @formatter:off
    @Getter @Setter private ChannelHandlerContext ctx;
    @Getter @Setter private Channel channel;
    @Getter @Setter private String channelID;
    @Getter @Setter private String remoteAddress;

    @Getter @Setter private long httpHandlerPacketGroupId = 0;

    // sbNote 채널을 통햇 수신중인 패킷의 임시 저장용 버퍼
    public byte[] tmpBuffer = null;
    public int tmpBufferIdx = 0;
    @Getter @Setter private boolean completedPacket = false;

    @Getter @Setter private byte[] byteStationId;
    @Getter @Setter private byte[] byteChargerId;
    @Getter @Setter private String strStationId = "";
    @Getter @Setter private String strChargerId = "";
    @Getter @Setter private int intStationId = 0;
    @Getter @Setter private int intChargerId = 0;
    @Getter @Setter private String strEnvStationId = "";             // 환경부 충전소 아이디

    @Getter @Setter private String strFirmwareInsb = "";          	// b 명령어에서 수신된 펌웨어 정보
    @Getter @Setter private String strMDNInsb = "";          		// b 명령어에서 수신된 모뎀번호


    @Getter private boolean certificationCompleted = false;

    @Getter @Setter private boolean send2OcasInsL = false;		// OCAS 서버로 l 명령어 전송 여부, 한번에 한개만 전송 가능함

    // sbNote 서버로부터 가져온 정보
    @Getter @Setter private boolean successChargerInfo;
    @Getter @Setter private long recvChargerInfoLastTime = 0;           // 충전기의 서버 정보를 마지막으로 불러온 시간, 주기적으로 업데이트가 필요할듯...

	@Getter @Setter private int recvOpen = 1;							// 충전기 공개/비공개/???

    @Getter @Setter private String recvLastFirmwareVersion;
    @Getter @Setter private String recvLastFirmwareLink;
    @Getter @Setter private String recvLastFirmwareName;
    @Getter @Setter private int recvLastFirmwareSize;
	@Getter @Setter private String recvLastFirmwareFilename;

	// @Getter @Setter private int firmwareCompletedOffset = 0;	// sbNote 추가 200901 처리완료된 패킷 오프셋
	// @Getter @Setter private int firmwareLastOffset = 0;			// sbNote 추가 200901 처리완료된 패킷 오프셋

	@Getter @Setter private int firmwarePacketCurIndex = 0;
	@Getter @Setter private int firmwarePacketCount = 0;

    @Getter @Setter private String recvChargerType;
    @Getter @Setter private boolean recvQuickTypeDcCombo;
    @Getter @Setter private boolean recvQuickTypeCardemo;
    @Getter @Setter private boolean recvQuickTypeAC3;

    @Getter @Setter private String recvManufacturer;
    @Getter @Setter private String recvModelCode;
    @Getter @Setter private int recvConcurrentChannel;
    @Getter @Setter private String recvFirmwareVersion;
    @Getter @Setter private int recvSound = 3;
    @Getter @Setter private int recvChannel = 0;            // 충전기 채널
    @Getter @Setter private int recvKwh;                    // 충전 요규 전력량 (CMD H)
    @Getter @Setter private int recvAmperage;               // 충전 설정 전류량 (CMD H)
    @Getter @Setter private int recvAmperageChannel;        // 채널 설정 전류량 (CMD C)

    @Getter @Setter private ArrayList<Integer> recvUnitPrice;
    @Getter @Setter private int recvNonMemberUnitPrice = 200;
	@Getter @Setter private int recvEnvUnitPrice = 200;			// sbNote 210128 추가
    @Getter @Setter private int userPoint = 999999;

	// @Getter @Setter private boolean recvUseNHistory = true;
	// @Getter @Setter private boolean recvUseDebugLog = false;

    @Getter @Setter private String meSid;   // 환경부 충전소 ID
    @Getter @Setter private String meCid;   // 환경부 충전기 ID

    @Getter @Setter private int status;     // 충전기 상태
    @Getter @Setter private int active;     // 충전기 활성 상태

    // sbNote 런타임
	// @Getter @Setter private Boolean writeFirmwareData = true;
    @Getter @Setter private int updateFirmwareReadLength = 0;
    @Getter @Setter private byte[] updateFirmwareData = null;
    @Getter @Setter private long lastPacketTime = -1;
	@Getter @Setter private long connectedTime = -1;

	// // sbNote 추가 200901
	// @Getter @Setter private LinkedList<FirmwareData> updateFirmwareDataList = null;

    @Getter @Setter private String userMemID;
    @Getter @Setter private String userCardNo;

    @Getter @Setter private ArrayList<byte[]> pendingSendData;

	@Getter @Setter private ChannelHandlerContext httpCtx;
	@Getter @Setter private Boolean httpResponseAvailable = false;
	@Getter @Setter private String httpResponseIns;
	@Getter @Setter private String httpResponseBody;

    // @formatter:on

	public static class FirmwareData implements Comparable<FirmwareData>
	{
		// @Getter @Setter private int    packetIndex = 0;
		@Getter @Setter private int    dataIndex   = 0;
		@Getter @Setter private byte[] data        = null;

		public FirmwareData(int dataIndex) {
			// this.packetIndex = packetIndex;
			this.dataIndex = dataIndex;
		}

		@Override
		public int compareTo(@NotNull FirmwareData firmwareData)
		{
			if (this.dataIndex < firmwareData.getDataIndex())
			{
				return -1;
			}
			else if (this.dataIndex > firmwareData.getDataIndex())
			{
				return 1;
			}
			return 0;
		}
	}

    public ChargerModel(Channel channel)
    {
        this.channel = channel;
        this.channelID = channel.id().toString();
    }

    // public void tmpBufferIdxPlusOne()
    // {
    //     tmpBufferIdx = tmpBufferIdx + 1;
    // }
}
