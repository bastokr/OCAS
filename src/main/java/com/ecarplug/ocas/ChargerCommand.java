package com.ecarplug.ocas;

import com.ecarplug.ocas.model.ChargerModel;
import com.ecarplug.ocas.network.DownloadInterface;
import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mariadb.jdbc.ClientSidePreparedStatement;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLType;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// import org.json.JSONException;
// import org.json.JSONObject;

@SuppressWarnings({"ResultOfMethodCallIgnored", "MalformedFormatString", "MismatchedQueryAndUpdateOfStringBuilder", "Duplicates"})
@Slf4j
public class ChargerCommand
{
	public static final int HEADER_TAIL_LEN = 12;

	// 서버 요청 => 충전기 응답 명령어
	public static final byte INS_OCAS_2_CHARGER_A = 0x41;         // 충전기 설정 조회
	public static final byte INS_OCAS_2_CHARGER_B = 0x42;         // 충전기 설정 변경
	public static final byte INS_OCAS_2_CHARGER_C = 0x43;         // 충전기 채널 설정 조회
	public static final byte INS_OCAS_2_CHARGER_D = 0x44;         // 충전기 채널 설정 변경
	public static final byte INS_OCAS_2_CHARGER_E = 0x45;         // 단가 정보 조회
	public static final byte INS_OCAS_2_CHARGER_F = 0x46;         // 단가 정보 변경
	public static final byte INS_OCAS_2_CHARGER_G = 0x47;         // 충전기 상태 조회
	public static final byte INS_OCAS_2_CHARGER_H = 0x48;         // 충전 시작 요청
	public static final byte INS_OCAS_2_CHARGER_I = 0x49;         // 충전 상태 조회
	public static final byte INS_OCAS_2_CHARGER_J = 0x4A;         // 충전 종료
	public static final byte INS_OCAS_2_CHARGER_K = 0x4B;         // 충전 이력 조회 (요청)
	public static final byte INS_OCAS_2_CHARGER_L = 0x4C;         // 충전 이력 전송
	public static final byte INS_OCAS_2_CHARGER_M = 0x4D;         // 다운로드 정보 조회
	public static final byte INS_OCAS_2_CHARGER_N = 0x4E;         // 다운로드 전송
	public static final byte INS_OCAS_2_CHARGER_O = 0x4F;         // 즉시 제어
	public static final byte INS_OCAS_2_CHARGER_P = 0x50;         // 제조사 모드
	public static final byte INS_OCAS_2_CHARGER_Q = 0x51;         // 통신인증
	public static final byte INS_OCAS_2_CHARGER_R = 0x52;         // 항목선택
	public static final byte INS_OCAS_2_CHARGER_S = 0x53;         // 항목선택

	// 충전기 요청 => 서버 응답 명령어
	public static final byte INS_CHARGER_TO_OCAS_B = 0x62;         // 충전기 설정 변경
	public static final byte INS_CHARGER_TO_OCAS_D = 0x64;         // 충전기 채널 설정 변경
	public static final byte INS_CHARGER_TO_OCAS_E = 0x65;         // 단가 정보 조회
	public static final byte INS_CHARGER_TO_OCAS_F = 0x66;         // 단가 정보 변경
	public static final byte INS_CHARGER_TO_OCAS_G = 0x67;         // 충전기 상태 조회
	public static final byte INS_CHARGER_TO_OCAS_H = 0x68;         // 충전 시작
	public static final byte INS_CHARGER_TO_OCAS_I = 0x69;         // 충전 상태 조회
	public static final byte INS_CHARGER_TO_OCAS_J = 0x6A;         // 충전 종료
	public static final byte INS_CHARGER_TO_OCAS_L = 0x6C;         // 충전 이력 전송
	public static final byte INS_CHARGER_TO_OCAS_M = 0x6D;         // 다운로드 조회
	public static final byte INS_CHARGER_TO_OCAS_N = 0x6E;         // 다운로드 전송
	public static final byte INS_CHARGER_TO_OCAS_Q = 0x71;         // 항목선택

	public static final byte SOH = 0x01;
	public static final byte STX = 0x02;
	public static final byte ETX = 0x03;
	public static final byte EOT = 0x04;

	// sbNote 충전상태
	public static final int STATE_CHARGE_READY = 0;                 // 대기
	public static final int STATE_CHARGE_RESERVED = 1;              // 예약대기
	public static final int STATE_CHARGE_CHARGING = 2;              // 충전
	public static final int STATE_CHARGE_STOP_USER = 3;             // 사용자 종료
	public static final int STATE_CHARGE_STOP_CAR = 4;              // 차량 종료
	public static final int STATE_CHARGE_STOP_CHARGER = 5;          // 충전기 종료
	public static final int STATE_CHARGE_STOP_COMM_ERROR = 6;       // 전기차 통신 오류 종료
	public static final int STATE_CHARGE_STOP_LOW_VOLTAGE = 7;      // 저전압 종료
	public static final int STATE_CHARGE_STOP_HIGH_VOLTAGE = 8;    // 과전압 종료
	public static final int STATE_CHARGE_STOP_OVERCURRENT = 9;      // 과전류 종료
	public static final int STATE_CHARGE_STOP_MC = 10;              // MC 융착 종료
	public static final int STATE_CHARGE_STOP_EMERGENCY = 11;       // 비상 스위치 종료
	public static final int STATE_CHARGE_STOP_RCD = 12;             // RCD 차단 종료
	public static final int STATE_CHARGE_STOP_POWER_OFF = 13;       // 전원 OFF 종료

	// sbNote 응답코드
	public static final byte RESP_0 = 0x00;        // 정상
	public static final byte RESP_1 = 0x01;        // 오류
	public static final byte RESP_2 = 0x02;        // 전문 오류
	public static final byte RESP_3 = 0x03;        // 충전시작 / 종료 오류
	public static final byte RESP_4 = 0x04;        // 다운로드 오류
	public static final byte RESP_5 = 0x05;        // 회원 잔액 부족
	public static final byte RESP_6 = 0x06;        // 서버 등록 오류

	// sbNote 회원 구분 코드
	public static final int MEMBER_POSTPAY = 0;         // 후불회원
	public static final int MEMBER_PREPAYMENT = 1;      // 선불회원
	public static final int MEMBER_NON_MEMBERS = 2;     // 비회원

	// sbNote 이벤트 코드
	public static final int EVENT_NONE = 0;
	public static final int EVENT_POWER_ON = 1;
	public static final int EVENT_READY = 2;
	public static final int EVENT_RFID = 3;                     // RFID 태깅
	public static final int EVENT_START_DOWNLOAD = 4;
	public static final int EVENT_STOP_DOWNLOAD = 5;
	public static final int EVENT_START_OUTPUT_CURRENT = 6;     // 충전 전류 출력 시작
	public static final int EVENT_STOP_OUTPUT_CURRENT = 7;      // 충전 전류 출력 중지

	// sbNote 다운로드 종류
	public static final byte DOWNLOAD_TYPE_1 = 0x00;        // 프로그램

	// sbNote 충전기 상태 정보 (환경부 코드 기준)
	public static final int ME_CHARGER_STATE_UNKNOWN = 0;
	public static final int ME_CHARGER_STATE_ERROR = 1;
	public static final int ME_CHARGER_STATE_READY = 2;
	public static final int ME_CHARGER_STATE_CHARGING = 3;
	public static final int ME_CHARGER_STATE_OUT_OF_SERVICE = 4;
	public static final int ME_CHARGER_STATE_CHECKING = 5;
	public static final int ME_CHARGER_STATE_RESERVED = 6;
	public static final int ME_CHARGER_STATE_UNCHECKED = 9;

	public static final String Charger2Ocas = "C2O";    // 충전기 질의, OCAS 응답
	public static final String Ocas2Charger = "O2C";    // OCAS 질의, 충전기 응답
	public static final String RECV = "recv";
	public static final String SEND = "send";

	// @formatter:off
    // @Getter @Setter private byte[] byteStationId;
    // @Getter @Setter private byte[] byteChargerId;
    // @Getter @Setter private String strStationId;
    // @Getter @Setter private String strChargerId;
    // @Getter @Setter private int intStationId;
    // @Getter @Setter private int intChargerId;
    //
    // @Getter private boolean certificationCompleted = false;

    private CustomApplication _app;

    @Getter @Setter private String orderOrUserNo = "";

    @Getter @Setter private String currentFirmwareFileName = "";
    @Getter @Setter private int currentFirmwareFileVer = 0;
    @Getter @Setter private int currentFirmwareFileSize = 0;

    @Getter @Setter private boolean needUpdateFirmware = false;
    @Getter @Setter private boolean needUpdateFirmwareBackground = false;
    @Getter @Setter private boolean sendUpdateFirmwareBackground = false;
    @Getter @Setter private boolean needCommandM = true;
    // private Single<JSONObject> _backgroundCmdN = null;

    // @Getter @Setter private String updateFirmwareFileName = "";
    // @Getter @Setter private int updateFirmwareFileVer = 0;
    // @Getter @Setter private int updateFirmwareFileSize = 0;
    // @Getter @Setter private int updateFirmwareReadLength = 0;
    // @Getter @Setter private byte[] updateFirmwareData = null;

    // @Getter @Setter private int lastIndex = 0;
    // @Getter @Setter private int lastLength = 0;
    // @Getter @Setter private boolean forceUpdateRestart = false;
    // @formatter:on

	// private byte[] currentChannel = new byte[1];
	// private boolean currentChannelActive = false;
	// private long oldUpdateStateTime = -1;    // 상태 변경이 일어난 시간
	private int oldStatusCharger = -1;          // 충전기 상태
	private int oldStatus = -1;                 // 충전 상태

	// private byte[] currentOrderNo = new byte[8];
	// private Timer timer = new Timer();
	// private TimerTask tt = new TimerTask() {
	//    @Override
	//    public void run() {
	//        if (currentChannelActive)
	//            sendCommandI();
	//    }
	// };

	public ChargerCommand(CustomApplication application/*, String macAddress, String chargerName*/)
	{
		_app = application;

		// this.macAddress = macAddress;
		// this.chargerName = chargerName;
		// if (!BuildConfig.IS_HOME_CHARGER) needUpdateFirmwareBackground = true;
	}

	// private boolean checkStationChargerId(byte[] packet)
	// {
	//     int stationId = ChargerCommandUtil.hex2Number(Arrays.copyOfRange(packet, 1, 5));
	//     int chargerId = ChargerCommandUtil.hex2Number(packet[5]);
	//
	//     return stationId == this.intStationId && chargerId == this.intChargerId;
	// }

	public Single<JSONObject> processRecvPacket(ChargerModel charger, byte[] recvPacket, long packetGroupId)
	{
		return Single.create(emitter -> {
			int validResult = validPacket("recv", recvPacket);

			charger.setByteStationId(Arrays.copyOfRange(recvPacket, 1, 5));

			charger.setByteChargerId(new byte[1]);
			charger.getByteChargerId()[0] = recvPacket[5];

			charger.setIntStationId(ChargerCommandUtil.hex2Number(charger.getByteStationId()));
			charger.setIntChargerId(ChargerCommandUtil.hex2Number(charger.getByteChargerId()[0]));
			// charger.setStrStationId(String.valueOf(charger.getIntStationId()));
			charger.setStrStationId(String.format("%08d", charger.getIntStationId()));
			charger.setStrChargerId(String.format("%02d", charger.getIntChargerId()));

			String strStationChargerID = charger.getStrStationId() + "_" + charger.getStrChargerId();
			String strRecvPacket = ChargerCommandUtil.byte2HexString(recvPacket);

			// sbNote 200514 충전기 재시작 또는 모뎀 리셋으로 인한 연결시 이전 연결이 종료되지 않고 연결되어져 있는 현상
			ChargerModel findCharger = null;
			Set<Map.Entry<String, ChargerModel>> entrySet = _app.getClientMap().entrySet();
			long now = System.currentTimeMillis();
			long checkDummyInterval = 31 * 60 * 1000;	// 31분

			for (Map.Entry<String, ChargerModel> entry : entrySet)
			{
				// String channelID = entry.getKey();
				ChargerModel valueCharger = (ChargerModel) entry.getValue();

				// sbNote 패킷을 받은 충전기는 제외
				if (!charger.getChannelID().equals(valueCharger.getChannelID()))
				{
					// sbNote 패킷을 받은 충전기와 동일한 아이디를 가진 다른 정보는 소켓 해제
					if (charger.getStrStationId().equals(valueCharger.getStrStationId())
							&& charger.getStrChargerId().equals(valueCharger.getStrChargerId()))
					{
						// valueCharger.getChannel().writeAndFlush("Connection closed").addListener(ChannelFutureListener.CLOSE);

						// sbNote 재연결시 b 명령어어서 수신된 모뎀번호와 펌웨어 정보 유지
						if (charger.getStrMDNInsb().isEmpty() && !valueCharger.getStrMDNInsb().isEmpty())
							charger.setStrMDNInsb(valueCharger.getStrMDNInsb());

						if (charger.getStrFirmwareInsb().isEmpty() && !valueCharger.getStrFirmwareInsb().isEmpty())
							charger.setStrFirmwareInsb(valueCharger.getStrFirmwareInsb());

						valueCharger.getChannel().close();
						_app.getClientMap().remove(valueCharger.getChannelID());
					}
					// sbNote 모뎀 리셋 이후에 연결이 여러번 되는 경우가 발생됨, 실제 데이터를 수신한 이력이 없는 소켓을 종료
					else if (valueCharger.getLastPacketTime() == -1 && (now - valueCharger.getConnectedTime()) > checkDummyInterval)
					{
						log.info("Dummy connection closed : " + valueCharger.getRemoteAddress());

						valueCharger.getChannel().close();
						_app.getClientMap().remove(valueCharger.getChannelID());
					}
				}
			}

			log.info("Concurrent Channel : " + _app.getClientMap().size());

			if (RESP_0 == validResult)
			{
				//
				// sbTodo 서버로 수신된 패킷 전송 로직 추가
				//
				boolean needChargerInfo = false;

				if (charger.getRecvChargerInfoLastTime() == 0) needChargerInfo = true;
				else
				{
					float diff = System.currentTimeMillis() - charger.getRecvChargerInfoLastTime();
					float sec = diff / 1000F;
					float minutes = sec / 60F;
					log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] 마지막 충전기 정보 가져온지 : %s", charger.getStrStationId(), charger.getStrChargerId(), minutes));

					// sbNote 지정된 시간 단위로 충전기 정보를 갱신
					if (minutes > _app.getRefreshCharger()) needChargerInfo = true;
				}

				// sbTodo 주기적으로 업데이트가 필요할듯...
				if (needChargerInfo)
				{
					loadChargerInfo(charger, charger.getStrStationId(), charger.getStrChargerId()).subscribe(success -> {
						processRecvPacket00(charger, recvPacket, packetGroupId);
					}, error -> {
						log.error(error.getMessage());
					});
				}
				else
				{
					processRecvPacket00(charger, recvPacket, packetGroupId);
				}
			}
			else
			{
				// payload
				byte[] payload = new byte[1];

				payload[0] = ChargerCommandUtil.number2Hex(validResult, 2)[1];

				byte[] sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, recvPacket[6]);
				if (RESP_0 == validPacket("send", sendPacket))
				{
					sendChargerToOcas(charger, sendPacket).subscribe(success -> {
					}, reject -> {
					});
				}
				else
				{

				}
			}

			// try
			// {
			//     SBCommonUtil.LogDebug(CustomApplication.TAG, "createSingle ==> action : " + action);
			//
			//     _plugin.execute(action, param, new SBCallbackContext(getCallbackId())
			//     {
			//         @Override
			//         public void handlePluginResult(PluginResult pluginResult)
			//         {
			//             if (resultValid(pluginResult.getStatus()))
			//             {
			//                 try
			//                 {
			//                     emitter.onSuccess(asJSONObject(pluginResult));
			//                 }
			//                 catch (Exception e)
			//                 {
			//                     e.printStackTrace();
			//                     SBCommonUtil.LogError(CustomApplication.TAG, "createSingle : " + action);
			//                     emitter.onError(e);
			//                 }
			//             }
			//             else
			//             {
			//                 emitter.onError(new Exception("Result Message : " + pluginResult.getMessage() + ", Status : " + String.valueOf(pluginResult.getStatus())));
			//             }
			//         }
			//     });
			// }
			// catch (JSONException e)
			// {
			//     e.printStackTrace();
			//     emitter.onError(e);
			// }
		});
	}

	private void processRecvPacket00(ChargerModel charger, byte[] recvPacket, long packetGroupId)
	{
		switch (recvPacket[6])
		{
			case ChargerCommand.INS_CHARGER_TO_OCAS_B:
				recvChargerToOcasB(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_D:
				recvChargerToOcasD(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_E:
				recvChargerToOcasE(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_F:
				recvChargerToOcasF(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_G:
				recvChargerToOcasG(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_H:
				recvChargerToOcasH(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_I:
				recvChargerToOcasI(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_J:
				recvChargerToOcasJ(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_L:
				recvChargerToOcasL(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_M:
				recvChargerToOcasM(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_N:
				recvChargerToOcasN(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_CHARGER_TO_OCAS_Q:
				recvChargerToOcasQ(charger, recvPacket, packetGroupId);
				break;

			case ChargerCommand.INS_OCAS_2_CHARGER_A:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_B:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_C:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_D:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_F:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_G:
				recvOcasToChargerG(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_H:
				recvOcasToChargerH(charger, recvPacket, packetGroupId);
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_I:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_J:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_K:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_L:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_M:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_N:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_O:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_P:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_Q:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_R:
				break;
			case ChargerCommand.INS_OCAS_2_CHARGER_S:
				break;
		}
	}

	public Single<JSONObject> sendChargerToOcas(@NonNull ChargerModel charger, @NonNull byte[] sendPacket)
	{
		return Single.create(emitter -> {
			if (RESP_0 == validPacket("send", sendPacket))
			{
				ByteBuf writebuf = Unpooled.directBuffer();
				writebuf.writeBytes(sendPacket);

				ChannelFuture cf = charger.getChannel().writeAndFlush(writebuf);
				cf.addListener((ChannelFutureListener) future -> {
					if (future.isSuccess())
					{
						// log.debug("sendChargerToOcas 전송 성공");
						emitter.onSuccess(new JSONObject());
					}
					else
					{
						//
						// sbTodo 충전기 재연결되어 데이터를 전송하지 못하는 경우 리스트에 추가 ???
						//
						// ArrayList<Integer> unitPrice = new ArrayList<Integer>(24);
						// if (charger.getPendingSendData() ==

						log.error("sendChargerToOcas 전송 실패");
						emitter.onError(future.cause());
					}
				});
			}
			else
			{
				log.error("전송 패킷의 형식이 올바르지 않습니다.");
				emitter.onError(new Throwable("전송 패킷의 형식이 올바르지 않습니다."));
			}
		});
	}

	/**
	 * 충전기 설정 전송
	 */
	@SuppressWarnings("JoinDeclarationAndAssignmentJava")
	public void recvChargerToOcasB(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 1, 1, 10, 6, 1, 11});
		if (result != null)
		{
			int manufacture = ChargerCommandUtil.hex2Number(result[0]);
			int modelcode = ChargerCommandUtil.hex2Number(result[1]);
			int concurrentChannel = ChargerCommandUtil.hex2Number(result[2]);
			String strCurrentVer = new String(result[3], StandardCharsets.UTF_8).trim();
			String strCurrentTime = ChargerCommandUtil.byte2DateTimeBCD(result[4]);
			int volume = ChargerCommandUtil.hex2Number(result[5]);
			String strMDN = ChargerCommandUtil.padLeftZeros(new String(result[6], StandardCharsets.UTF_8).trim(), 11);

			String recvPayloadParsing = String.format("제조사 코드 : %s\n", manufacture) +
					String.format("모델코드 : %s\n", modelcode) +
					String.format("동시 충전 가능 채널수 : %s\n", concurrentChannel) +
					String.format("프로그램 버전정보 : %s\n", strCurrentVer) +
					String.format("현재시간 : %s\n", strCurrentTime) +
					String.format("사운드 볼륨 : %s\n", volume) +
					String.format("모뎀 번호(MDN) : %s\n", strMDN);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV b]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "b", recvPayloadParsing, packet)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbNote 충전기 상태를 관리서버로 전송
			charger.setStrFirmwareInsb(strCurrentVer);
			charger.setStrMDNInsb(strMDN);
			sendChargerState(charger, "b", -1, -1, -1, -1, -1, -1);

			// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
			byte[] payload;
			byte[] sendPacket;

			try
			{
				// TimeZone tz;
				// tz = TimeZone.getTimeZone("Asia/Seoul");
				// Date date = new Date();
				// DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (z Z)");
				// df.setTimeZone(tz);
				// System.out.format("%s%n%s%n%n", tz.getDisplayName(), df.format(date));

				Date currentDT = new Date();
				SimpleDateFormat transFormat = new SimpleDateFormat("yyMMddHHmmss");
				TimeZone tz;
				tz = TimeZone.getTimeZone("Asia/Seoul");
				transFormat.setTimeZone(tz);
				String strCurrentDT = transFormat.format(currentDT);

				// payload
				payload = new byte[8];

				byte responseCode = charger.isSuccessChargerInfo() ? RESP_0 : RESP_6;

				payload[0] = ChargerCommandUtil.number2Hex(responseCode, 2)[1];

				payload[1] = Byte.parseByte(strCurrentDT.substring(0, 2), 16);
				payload[2] = Byte.parseByte(strCurrentDT.substring(2, 4), 16);
				payload[3] = Byte.parseByte(strCurrentDT.substring(4, 6), 16);
				payload[4] = Byte.parseByte(strCurrentDT.substring(6, 8), 16);
				payload[5] = Byte.parseByte(strCurrentDT.substring(8, 10), 16);
				payload[6] = Byte.parseByte(strCurrentDT.substring(10, 12), 16);

				payload[7] = ChargerCommandUtil.number2Hex(charger.getRecvSound(), 2)[1];

				sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_B);
				sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
					// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
					// sbTodo 수신된 패킷의 정보를 서버로 전송
					String sendPayloadParsing = String.format("응답코드 : %s\n", responseCode) +
							String.format("현재시간 : %s\n", strCurrentDT) +
							String.format("사운드볼륨 : %s\n", charger.getRecvSound());

					if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND b]\n" + sendPayloadParsing); }

					sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "b", sendPayloadParsing, sendPacket)
							.subscribeOn(Schedulers.io())
							.subscribe();
				}, sendError -> {
					// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
				});
			}
			catch (Exception ex)
			{
				log.info("sendChargerToOcasB : " + ex.getMessage());
			}
		}
	}

	public void recvChargerToOcasD(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 2, 4, 4, 4});
		if (result != null)
		{
			int channel = ChargerCommandUtil.hex2Number(result[0]);
			int chargerStatus = ChargerCommandUtil.hex2Number(result[1]);
			int chargerKWH = ChargerCommandUtil.hex2Number(result[2]);
			int chargerSupplyAmpere = ChargerCommandUtil.hex2Number(result[3]);
			int chargerChannelAmpere = ChargerCommandUtil.hex2Number(result[4]);

			String recvPayloadParsing = String.format("채널 : %s\n", channel) +
					String.format("충전기 상태 : %s\n", chargerStatus) +
					String.format("충천기 사용 전력량 : %s\n", chargerKWH) +
					String.format("공급 가능한 전류량 : %s\n", chargerSupplyAmpere) +
					String.format("채널 설정 전류량 : %s\n", chargerChannelAmpere);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV d]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "d", recvPayloadParsing, packet)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbNote 충전기 상태를 관리서버로 전송
			sendChargerState(charger, "d", chargerStatus, -1, -1, chargerKWH, -1, -1);

			// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
			byte[] payload;
			byte[] sendPacket;
			int payloadIdx = 0;
			try
			{
				// payload
				payload = new byte[4];

				byte[] byteChannelAmpere = ChargerCommandUtil.number2Hex(charger.getRecvAmperageChannel(), 4);
				System.arraycopy(byteChannelAmpere, 0, payload, payloadIdx, byteChannelAmpere.length);
				payloadIdx += byteChannelAmpere.length;

				sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_D);
				sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
					// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
					// sbTodo 수신된 패킷의 정보를 서버로 전송
					String sendPayloadParsing = String.format("채널 설정 전류량 : %s\n", charger.getRecvAmperageChannel());

					if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND d]\n" + sendPayloadParsing); }

					sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "d", sendPayloadParsing, sendPacket)
							.subscribeOn(Schedulers.io())
							.subscribe();
				}, sendError -> {
					// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
				});
			}
			catch (Exception ex)
			{
				log.info("sendChargerToOcasD : " + ex.getMessage());
			}
		}
	}

	public void recvChargerToOcasE(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		String recvPayloadParsing = "N/A";

		if (_app.isDebugLog())
		{
			log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV e]\n" + recvPayloadParsing);
		}

		// sbTodo 수신된 패킷의 정보를 서버로 전송
		sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "e", recvPayloadParsing, packet)
				.subscribeOn(Schedulers.io())
				.subscribe();

		// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
		byte[] payload;
		byte[] sendPacket;
		int payloadIdx = 0;
		try
		{
			int[] priceTable = Ints.toArray(charger.getRecvUnitPrice());

			// payload
			payload = new byte[48 + 2];
			StringBuilder price = new StringBuilder();

			for (int idx = 0; idx < priceTable.length; idx++)
			{
				if (idx > 0) price.append(", ");
				price.append(priceTable[idx]);

				byte[] bytePrice = ChargerCommandUtil.number2Hex(priceTable[idx], 2);
				payload[payloadIdx] = bytePrice[0];
				payload[payloadIdx + 1] = bytePrice[1];
				payloadIdx += 2;
			}

			// sbNote 추가 200724 >> 비회원 단가를 현장 결제 단가 정보로 전송
			byte[] byteNonmemberUnitPrice = ChargerCommandUtil.number2Hex(charger.getRecvNonMemberUnitPrice(), 2);
			System.arraycopy(byteNonmemberUnitPrice, 0, payload, payloadIdx, byteNonmemberUnitPrice.length);

			sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_E);
			sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
				// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
				// sbTodo 수신된 패킷의 정보를 서버로 전송

				String sendPayloadParsing = String.format("충전 단가 (시간별) : %s\n", price.toString());

				if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND e]\n" + sendPayloadParsing); }

				sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "e", sendPayloadParsing, sendPacket)
						.subscribeOn(Schedulers.io())
						.subscribe();
			}, sendError -> {
				// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
			});
		}
		catch (Exception ex)
		{
			log.info("sendChargerToOcasE : " + ex.getMessage());
		}
	}

	public void recvChargerToOcasF(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 2, 4, 2, 2, 1, 1});
		if (result != null)
		{
			int channel = ChargerCommandUtil.hex2Number(result[0]);
			int chargerStatus = ChargerCommandUtil.hex2Number(result[1]);
			int chargerKWH = ChargerCommandUtil.hex2Number(result[2]);
			// int chargerTemperature = ChargerCommandUtil.hex2Number(result[3]);
			// int chargerHumidity = ChargerCommandUtil.hex2Number(result[4]);
			int chargerEventCode = ChargerCommandUtil.hex2Number(result[5]);
			int chargerRSSI = ChargerCommandUtil.hex2Number(result[6]);

			float temperature = (float) (Math.round(ChargerCommandUtil.hex2Number(result[3]) * 100) / 10000.0);         // 온도
			float humidity = (float) (Math.round(ChargerCommandUtil.hex2Number(result[4]) * 100) / 10000.0);            // 습도

			String recvPayloadParsing = String.format("채널 : %s\n", channel) +
					String.format("충전기 상태 : %s\n", chargerStatus) +
					String.format("충천기 사용 전력량 : %s\n", chargerKWH) +
					String.format("온도 : %s\n", temperature) +
					String.format("습도 : %s\n", humidity) +
					String.format("이벤트 코드 : %s\n", chargerEventCode) +
					String.format("RSSI : %s\n", chargerRSSI);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV f]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "f", recvPayloadParsing, packet)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbNote 충전기 상태를 환경부 서버로 전송
			sendMEChargerStatus(charger, chargerStatus, -1, -1);

			// sbNote 충전기 상태를 관리서버로 전송
			sendChargerState(charger, "f", chargerStatus, -1, -1, chargerKWH, chargerEventCode, chargerRSSI);

			// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
			byte[] payload;
			byte[] sendPacket;
			int payloadIdx = 0;
			try
			{
				Date currentDT = new Date();
				SimpleDateFormat transFormat = new SimpleDateFormat("yyMMddHHmmss");
				TimeZone tz;
				tz = TimeZone.getTimeZone("Asia/Seoul");
				transFormat.setTimeZone(tz);
				String strCurrentDT = transFormat.format(currentDT);

				// sbNote 응답 패킷 길이가 7byte임
				// payload
				payload = new byte[7];

				payload[0] = ChargerCommandUtil.number2Hex(RESP_0, 2)[1];

				payload[1] = Byte.parseByte(strCurrentDT.substring(0, 2), 16);
				payload[2] = Byte.parseByte(strCurrentDT.substring(2, 4), 16);
				payload[3] = Byte.parseByte(strCurrentDT.substring(4, 6), 16);
				payload[4] = Byte.parseByte(strCurrentDT.substring(6, 8), 16);
				payload[5] = Byte.parseByte(strCurrentDT.substring(8, 10), 16);
				payload[6] = Byte.parseByte(strCurrentDT.substring(10, 12), 16);

				// payload[7] = ChargerCommandUtil.number2Hex(charger.getRecvSound(), 2)[1];

				sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_F);
				sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
					// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
					// sbTodo 수신된 패킷의 정보를 서버로 전송
					String sendPayloadParsing = String.format("응답코드 : %s\n", RESP_0) +
							String.format("현재 시간 : %s\n", strCurrentDT);

					if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND f]\n" + sendPayloadParsing); }

					sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "f", sendPayloadParsing, sendPacket)
							.subscribeOn(Schedulers.io())
							.subscribe();
				}, sendError -> {
					// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
				});
			}
			catch (Exception ex)
			{
				log.info("sendChargerToOcasF : " + ex.getMessage());
			}
		}
	}

	public void recvChargerToOcasG(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{8});
		if (result != null)
		{
			// int userNo = ChargerCommandUtil.hex2Number(result[0]);
			String userNo = ChargerCommandUtil.parseOrderNo(result[0]);

			String recvPayloadParsing = String.format("회원 번호1111 : %s\n", userNo);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV g]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "g", recvPayloadParsing, packet, "", userNo)
					.subscribeOn(Schedulers.io())
					.subscribe();

			sendOcasUserInfo("", userNo, "g").subscribe(availSuccess -> {
				int resp_code = RESP_0;

				int responseStatus = availSuccess.getInt("status");
				JSONObject data = availSuccess.getJSONObject("data");

				// 'data'    => ["result" => 1, "is_member" => 1, "point" => 0, "non_member_price" => 0, "msg" => ""],
				// data.getInt("is_member")

				int userPoint = data.getInt("point");
				int userType = data.getInt("user_type");
				int unitPrice = charger.getRecvNonMemberUnitPrice();   // data.getInt("non_member_price");

				// sbNote 210128 추가
				if (data.has("non_member_price_type") && data.getString("non_member_price_type").equals("env_price"))
				{
					unitPrice = charger.getRecvEnvUnitPrice();
				}

				int nonMemberPrice = unitPrice;

				if (data.has("ret_code"))
				{
					if (data.getString("ret_code").equals("1")) { resp_code = RESP_1; }
					else if (data.getString("ret_code").equals("2")) { resp_code = RESP_2; }
					else if (data.getString("ret_code").equals("3")) { resp_code = RESP_3; }
					else if (data.getString("ret_code").equals("4")) { resp_code = RESP_4; }
					else if (data.getString("ret_code").equals("5")) { resp_code = RESP_5; }
					else if (data.getString("ret_code").equals("6")) { resp_code = RESP_6; }
				}

				// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
				byte[] payload;
				byte[] sendPacket;
				int payloadIdx = 0;
				try
				{
					// 회원구분
					// 0 후불회원
					// 1 선불회원
					// 2 비회원

					// payload
					payload = new byte[12];

					payload[0] = ChargerCommandUtil.number2Hex(resp_code, 2)[1];
					payloadIdx++;

					byte[] byteChannelAmpere = ChargerCommandUtil.number2Hex(charger.getRecvAmperageChannel(), 4);
					System.arraycopy(byteChannelAmpere, 0, payload, payloadIdx, byteChannelAmpere.length);
					payloadIdx += byteChannelAmpere.length;

					// 회원 구분
					payload[payloadIdx++] = ChargerCommandUtil.number2Hex(userType, 2)[1];

					byte[] byteNonMemberPrice = ChargerCommandUtil.number2Hex(nonMemberPrice/*charger.getNonMemberDefaultPoint()*/, 2);
					System.arraycopy(byteNonMemberPrice, 0, payload, payloadIdx, byteNonMemberPrice.length);
					payloadIdx += byteNonMemberPrice.length;

					byte[] byteUserPoint = ChargerCommandUtil.number2Hex(userPoint/*charger.getUserPoint()*/, 4);
					System.arraycopy(byteUserPoint, 0, payload, payloadIdx, byteUserPoint.length);
					payloadIdx += byteUserPoint.length;

					sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_G);

					int final_resp_code = resp_code;

					sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
						// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
						// sbTodo 수신된 패킷의 정보를 서버로 전송
						String sendPayloadParsing = String.format("응답코드 : %s\n", final_resp_code) +
								String.format("충전 설정 전류량 : %s\n", charger.getRecvAmperageChannel()) +
								String.format("회원 구분 : %s\n", userType) +
								String.format("비회원 충전 단가 : %s\n", nonMemberPrice/*charger.getNonMemberDefaultPoint()*/) +
								String.format("회원 잔액 : %s\n", userPoint/*charger.getUserPoint()*/);

						if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND g]\n" + sendPayloadParsing); }

						sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "g", sendPayloadParsing, sendPacket, "", userNo)
								.subscribeOn(Schedulers.io())
								.subscribe();
					}, sendError -> {
						// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
					});
				}
				catch (Exception ex)
				{
					log.info("sendChargerToOcasG : " + ex.getMessage());
				}
			});
		}
	}

	@SuppressWarnings("Duplicates")
	public void recvChargerToOcasH(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 2, 4, 8, 6, 4, 4, 4});

		int ml = ChargerCommandUtil.getML(packet);
		boolean newPacketLength = ml > (1 + 2 + 4 + 8 + 6 + 4 + 4 + 4);

		if (newPacketLength)
			result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 2, 4, 8, 6, 4, 4, 4, 4, 6, 30, 20});

		if (result != null)
		{
			int channel = ChargerCommandUtil.hex2Number(result[0]);
			int chargerStatus = ChargerCommandUtil.hex2Number(result[1]);
			int chargerKWH = ChargerCommandUtil.hex2Number(result[2]);
			// int userNo = ChargerCommandUtil.hex2Number(result[3]);
			String userNo = ChargerCommandUtil.parseOrderNo(result[3]);
			String chargingStartTime = ChargerCommandUtil.byte2DateTimeBCD(result[4]);
			int chargingSupplyAmphere = ChargerCommandUtil.hex2Number(result[5]);
			int chargingTime = ChargerCommandUtil.hex2Number(result[6]);
			int chargingPrice = ChargerCommandUtil.hex2Number(result[7]);

			int prepay_amount = 0;
			String prepay_date = "n/a";
			String prepay_tran_no = "n/a";
			String prepay_auth_no = "n/a";

			if (newPacketLength)
			{
				prepay_amount = ChargerCommandUtil.hex2Number(result[8]);
				prepay_date = ChargerCommandUtil.byte2DateTimeBCD(result[9]);
				prepay_tran_no = new String(result[10], StandardCharsets.UTF_8).trim();
				prepay_auth_no = new String(result[11], StandardCharsets.UTF_8).trim();
			}

			String recvPayloadParsing = String.format("채널 : %s\n", channel) +
					String.format("충전기 상태 : %s\n", chargerStatus) +
					String.format("충천기 사용 전력량 : %s\n", chargerKWH) +
					String.format("회원 번호 (주문번호) : %s\n", userNo) +
					String.format("충전 요구 시작 시간 : %s\n", chargingStartTime) +
					String.format("충전 요구 전력량 : %s\n", chargingSupplyAmphere) +
					String.format("충전 요구 시간 : %s\n", chargingTime) +
					String.format("충전 요구 금액 : %s\n", chargingPrice) +

					String.format("선결재 금액 : %s\n", prepay_amount) +
					String.format("선결제 승인일시 : %s\n", prepay_date) +
					String.format("선결재 거래번호 : %s\n", prepay_tran_no) +
					String.format("선결재 승인번호 : %s\n", prepay_auth_no);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV h]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "h", recvPayloadParsing, packet, "", userNo)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbNote 충전기 상태를 관리서버로 전송
			sendChargerState(charger, "h", chargerStatus, -1, -1, chargerKWH, -1, -1);

			/**
			 * sbNote 비회원 현장결제
			 * 	F0F0F0F010101010 현장 실물카드 결제
			 * 	F0F0F0F020202020 현장 웹 결제
			 */
			if (userNo.equals("F0F0F0F0 10101010") || userNo.equals("F0F0F0F0 20202020") || userNo.startsWith("FF") ||
					(userNo.equals("E0C11001 A16EB253") && _app.isDeveloperServer()))
			{
				// sbNote 현장 실물카드 결제의 경우는 웹서버에 결제 정보를 전송합니다.
				if (userNo.equals("F0F0F0F0 10101010") || (userNo.equals("E0C11001 A16EB253") && _app.isDeveloperServer()))
				{
					if (userNo.equals("E0C11001 A16EB253"))
					{
						String tmp = _app.getOnlyTestTime();
						prepay_amount = 500;
						prepay_auth_no = tmp;
						prepay_tran_no = tmp;
						prepay_date = "20" + tmp;
					}

					sendSavePrepayPayment(charger, "",
							userNo.replaceAll(" ", "")
							, "h",
							prepay_amount,
							prepay_date,
							prepay_tran_no,
							prepay_auth_no).subscribe(paymentSuccess -> { });
				}

				int resp_code = RESP_0;

				// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
				byte[] payload;
				byte[] sendPacket;
				int payloadIdx = 0;
				try
				{
					// payload
					payload = new byte[1];

					payload[0] = ChargerCommandUtil.number2Hex(resp_code, 2)[1];

					sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_H);
					int final_resp_code = resp_code;

					sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
						// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
						// sbTodo 수신된 패킷의 정보를 서버로 전송
						String sendPayloadParsing = String.format("응답코드 : %s\n", final_resp_code);

						if (_app.isDebugLog())
						{
							log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND h]\n" + sendPayloadParsing);
						}

						sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "h", sendPayloadParsing, sendPacket, "", userNo)
								.subscribeOn(Schedulers.io())
								.subscribe();
					}, sendError -> {
						// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
					});
				}
				catch (Exception ex)
				{
					log.info("sendChargerToOcasH : " + ex.getMessage());
				}
			}
			else
			{
				// sbNote 서버로 충전이 가능한 회원카드인지를 검사
				sendOcasPaymentAvailable("", userNo, "h",
						prepay_amount,
						prepay_date,
						prepay_tran_no,
						prepay_auth_no).subscribe(availSuccess ->
				{
					int resp_code = RESP_0;

					int responseStatus = availSuccess.getInt("status");
					JSONObject data = availSuccess.getJSONObject("data");

					if (data.getInt("result") == 0)
					{
						if (data.has("ret_code"))
						{
							if (data.getString("ret_code").equals("1")) { resp_code = RESP_1; }
							else if (data.getString("ret_code").equals("2")) { resp_code = RESP_2; }
							else if (data.getString("ret_code").equals("3")) { resp_code = RESP_3; }
							else if (data.getString("ret_code").equals("4")) { resp_code = RESP_4; }
							else if (data.getString("ret_code").equals("5")) { resp_code = RESP_5; }
							else if (data.getString("ret_code").equals("6")) { resp_code = RESP_6; }
						}
					}

					// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
					byte[] payload;
					byte[] sendPacket;
					int payloadIdx = 0;
					try
					{
						// Date currentDT = new Date();
						// SimpleDateFormat transFormat = new SimpleDateFormat("yyMMddHHmmss");
						// String strCurrentDT = transFormat.format(currentDT);

						// payload
						payload = new byte[1];

						payload[0] = ChargerCommandUtil.number2Hex(resp_code, 2)[1];

						sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_H);
						int final_resp_code = resp_code;

						sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
							// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
							// sbTodo 수신된 패킷의 정보를 서버로 전송
							String sendPayloadParsing = String.format("응답코드 : %s\n", final_resp_code);

							if (_app.isDebugLog())
							{
								log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND h]\n" + sendPayloadParsing);
							}

							sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "h", sendPayloadParsing, sendPacket, "", userNo)
									.subscribeOn(Schedulers.io())
									.subscribe();
						}, sendError -> {
							// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
						});
					}
					catch (Exception ex)
					{
						log.info("sendChargerToOcasH : " + ex.getMessage());
					}

				}, availFail -> {
					// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
					byte[] payload;
					byte[] sendPacket;
					int payloadIdx = 0;
					try
					{
						// payload
						payload = new byte[1];

						// 오류로 전송
						payload[0] = ChargerCommandUtil.number2Hex(RESP_1, 2)[1];

						sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_H);
						sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
							// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
							// sbTodo 수신된 패킷의 정보를 서버로 전송
							String sendPayloadParsing = String.format("응답코드 : %s\n", RESP_1);

							if (_app.isDebugLog())
							{
								log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND h]\n" + sendPayloadParsing);
							}

							sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "h", sendPayloadParsing, sendPacket, "", userNo)
									.subscribeOn(Schedulers.io())
									.subscribe();
						}, sendError -> {
							// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
						});
					}
					catch (Exception ex)
					{
						log.info("sendChargerToOcasH : " + ex.getMessage());
					}
				});
			}
		}
	}

	public void recvChargerToOcasI(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 2, 4, 1, 8, 6, 6, 48, 6, 4, 4, 4, 4, 4, 4, 2, 2});
		if (result != null)
		{
			int channel = ChargerCommandUtil.hex2Number(result[0]);
			int chargerStatus = ChargerCommandUtil.hex2Number(result[1]);
			int chargerKWH = ChargerCommandUtil.hex2Number(result[2]);
			int chargingStatus = ChargerCommandUtil.hex2Number(result[3]);
			// int userNo = ChargerCommandUtil.hex2Number(result[4]);
			String userNo = ChargerCommandUtil.parseOrderNo(result[4]);
			String chargingStartTime = ChargerCommandUtil.byte2DateTimeBCD(result[5]);
			String chargingEndTime = ChargerCommandUtil.byte2DateTimeBCD(result[6]);
			String usageKwh = ChargerCommandUtil.parseUsageKwh(result[7]);
			String chargingUnplugTime = ChargerCommandUtil.byte2DateTimeBCD(result[8]);

			float amperage = (float) (Math.round(ChargerCommandUtil.hex2Number(result[9]) * 100) / 10000.0);             // 충전설정전류량
			float chargingVoltage = (float) (Math.round(ChargerCommandUtil.hex2Number(result[10]) * 100) / 10000.0);      // 충전전압
			float chargingAmperage = (float) (Math.round(ChargerCommandUtil.hex2Number(result[11]) * 100) / 10000.0);    // 충전전류

			float batteryTotal = (float) (Math.round(ChargerCommandUtil.hex2Number(result[12]) * 100) / 10000.0);        // 배터리 전체 용량 (0.6에서 추가)
			float batteryRemain = (float) (Math.round(ChargerCommandUtil.hex2Number(result[13]) * 100) / 10000.0);       // 현재 배터리 잔량 (0.6에서 추가)
			float batteryTemperature = (float) (Math.round(ChargerCommandUtil.hex2Number(result[14]) * 100) / 10000.0);  // 배터리 온도 (0.6에서 추가)

			float temperature = (float) (Math.round(ChargerCommandUtil.hex2Number(result[15]) * 100) / 10000.0);         // 온도
			float humidity = (float) (Math.round(ChargerCommandUtil.hex2Number(result[16]) * 100) / 10000.0);            // 습도

			String recvPayloadParsing = String.format("채널 : %s\n", channel) +
					String.format("충전기 상태 : %s\n", chargerStatus) +
					String.format("충천기 사용 전력량 : %s\n", chargerKWH) +
					String.format("충전 상태 : %s\n", chargingStatus) +
					String.format("회원 번호 (주문번호) : %s\n", userNo) +
					String.format("충전 시작 시간 : %s\n", chargingStartTime) +
					String.format("충전 종료 시간 : %s\n", chargingEndTime) +
					String.format("충전 전력량 (시간별) : %s\n", usageKwh) +
					String.format("언플러그 시간 : %s\n", chargingUnplugTime) +
					String.format("충전 전압 : %s\n", chargingVoltage) +
					String.format("충전 전류 : %s\n", chargingAmperage) +
					String.format("배터리 전체 용량 : %s\n", batteryTotal) +
					String.format("현재 배터리 잔량 : %s\n", batteryRemain) +
					String.format("배터리 온도 : %s\n", batteryTemperature) +
					String.format("온도 : %s\n", temperature) +
					String.format("습도 : %s\n", humidity);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV i]\n" + recvPayloadParsing);
				// log.debug("[Parsing RECV i] 채널 : " + channel + ", 충전기상태 : " + chargerStatus + // 충전기상태
				//         ", 충전상태 : " + chargingStatus +  // 충전상태
				//         "\n" + chargerKWH +                 // 충전기 사용 전력량
				//         "\n" + userNo +                     // 회원번호(주문번호)
				//         "\n" + chargingStartTime +          // 충전시작시간 BCD
				//         "\n" + chargingEndTime +            // 충전종료시간 BCD
				//         // "" + ChargerCommandUtil.hex2Number(result[6]) +              // 충전전력량 (시간별)
				//         "\n" + chargingUnplugTime +         // 언플러그시간 BCD
				//
				//         "\n" + amperage +                   // 충전설정전류량
				//         "\n" + chargingVoltage +            // 충전전압
				//         "\n" + chargingAmperage +           // 충전전류
				//         "\n" + batteryTotal +               // 배터리 전체 용량 (0.6에서 추가)
				//         "\n" + batteryRemain +              // 현재 배터리 잔량  (0.6에서 추가)
				//         "\n" + batteryTemperature +         // 배터리 온도  (0.6에서 추가)
				//
				//         "\n" + temperature +                // 온도
				//         "\n" + humidity);                   // 습도
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "i", recvPayloadParsing, packet, "", userNo)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbNote 충전기 상태를 환경부 서버로 전송
			sendMEChargerStatus(charger, chargerStatus, chargingStatus, -1);

			// sbNote 충전기 상태를 관리서버로 전송
			String[] usageKwhList = usageKwh.split(",");
			float kwhTotal = 0f;
			for (final String kwh : usageKwhList)
			{
				kwhTotal += Float.parseFloat(kwh.trim());
			}
			sendChargerState(charger, "i", chargerStatus, chargingStatus, kwhTotal, chargerKWH, -1, -1);

			// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
			byte[] payload;
			byte[] sendPacket;
			int payloadIdx = 0;
			try
			{
				// payload
				payload = new byte[1];

				payload[0] = ChargerCommandUtil.number2Hex(RESP_0, 2)[1];

				sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_I);
				sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
					// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
					// sbTodo 수신된 패킷의 정보를 서버로 전송
					String sendPayloadParsing = String.format("응답코드 : %s\n", RESP_0);

					if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND i]\n" + sendPayloadParsing); }

					sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "i", sendPayloadParsing, sendPacket, "", userNo)
							.subscribeOn(Schedulers.io())
							.subscribe();
				}, sendError -> {
					// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
				});
			}
			catch (Exception ex)
			{
				log.info("sendChargerToOcasI : " + ex.getMessage());
			}
		}
	}

	public void recvChargerToOcasJ(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 2, 4, 1, 8});
		if (result != null)
		{
			int channel = ChargerCommandUtil.hex2Number(result[0]);
			int chargerStatus = ChargerCommandUtil.hex2Number(result[1]);
			int chargerKWH = ChargerCommandUtil.hex2Number(result[2]);
			int chargingStatus = ChargerCommandUtil.hex2Number(result[3]);
			// int userNo = ChargerCommandUtil.hex2Number(result[4]);
			String userNo = ChargerCommandUtil.parseOrderNo(result[4]);

			String recvPayloadParsing = String.format("채널 : %s\n", channel) +
					String.format("충전기 상태 : %s\n", chargerStatus) +
					String.format("충천기 사용 전력량 : %s\n", chargerKWH) +
					String.format("충전 상태 : %s\n", chargingStatus) +
					String.format("회원 번호 (주문번호) : %s\n", userNo);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV j]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "j", recvPayloadParsing, packet, "", userNo)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbNote 충전기 상태를 관리서버로 전송
			sendChargerState(charger, "j", chargerStatus, chargingStatus, -1, chargerKWH, -1, -1);

			// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
			byte[] payload;
			byte[] sendPacket;
			int payloadIdx = 0;
			try
			{
				// sbNote 충전시 사용된 금액을 결제 결제 실패시 동작 정의 필요
				// sendOcasPayment(charger, "", userNo, "j", chargerKWH).subscribe(paymentSuccess -> {
				//     // int resp_code = RESP_0;
				//     //
				//     // if (!paymentSuccess.getBoolean("result"))
				//     // {
				//     //     JSONObject data = paymentSuccess.getJSONObject("data");
				//     //
				//     //     if (data.has("ret_code"))
				//     //     {
				//     //         if (data.getString("ret_code").equals("1")) resp_code = RESP_1;
				//     //         else if (data.getString("ret_code").equals("2")) resp_code = RESP_2;
				//     //         else if (data.getString("ret_code").equals("3")) resp_code = RESP_3;
				//     //         else if (data.getString("ret_code").equals("4")) resp_code = RESP_4;
				//     //         else if (data.getString("ret_code").equals("5")) resp_code = RESP_5;
				//     //         else if (data.getString("ret_code").equals("6")) resp_code = RESP_6;
				//     //     }
				//     // }
				// }, paymentFail -> {
				//
				// });

				// payload
				payload = new byte[1];

				payload[0] = ChargerCommandUtil.number2Hex(RESP_0, 2)[1];

				sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_J);
				sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
					// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
					// sbTodo 수신된 패킷의 정보를 서버로 전송
					String sendPayloadParsing = String.format("응답코드 : %s\n", RESP_0);

					if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND j]\n" + sendPayloadParsing); }

					sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "j", sendPayloadParsing, sendPacket, "", userNo)
							.subscribeOn(Schedulers.io())
							.subscribe();
				}, sendError -> {
					// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
				});
			}
			catch (Exception ex)
			{
				log.info("sendChargerToOcasJ : " + ex.getMessage());
			}
		}
	}

	public void recvChargerToOcasL(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 1, 8, 6, 6, 48, 6});

		int ml = ChargerCommandUtil.getML(packet);
		boolean newPacketLength = ml > (1 + 1 + 8 + 6 + 6 + 48 + 6);

		if (newPacketLength)
			result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 1, 8, 6, 6, 48, 6, 4, 6, 30, 20});

		if (result != null)
		{
			int channel = ChargerCommandUtil.hex2Number(result[0]);
			int chargingStatus = ChargerCommandUtil.hex2Number(result[1]);
			// int userNo = ChargerCommandUtil.hex2Number(result[2]);
			String userNo = ChargerCommandUtil.parseOrderNo(result[2]);

			String chargingStartTime = ChargerCommandUtil.byte2DateTimeBCD(result[3]);
			String chargingEndTime = ChargerCommandUtil.byte2DateTimeBCD(result[4]);
			String usageKwh = ChargerCommandUtil.parseUsageKwh(result[5]);
			String chargingUnplugTime = ChargerCommandUtil.byte2DateTimeBCD(result[6]);

			int prepay_amount = 0;
			String prepay_date = "n/a";
			String prepay_tran_no = "n/a";
			String prepay_auth_no = "n/a";

			if (newPacketLength)
			{
				prepay_amount = ChargerCommandUtil.hex2Number(result[7]);
				prepay_date = ChargerCommandUtil.byte2DateTimeBCD(result[8]);
				prepay_tran_no = new String(result[9], StandardCharsets.UTF_8).trim();
				prepay_auth_no = new String(result[10], StandardCharsets.UTF_8).trim();
			}

			String recvPayloadParsing = String.format("채널 : %s\n", channel) +
					String.format("충전 상태 : %s\n", chargingStatus) +
					String.format("회원 번호 (주문번호) : %s\n", userNo) +
					String.format("충전 시작 시간 : %s\n", chargingStartTime) +
					String.format("충전 종료 시간 : %s\n", chargingEndTime) +
					String.format("충전 전력량 (시간별) : %s\n", usageKwh) +
					String.format("언플러그 시간 : %s\n", chargingUnplugTime) +

					String.format("선결재 금액 : %s\n", prepay_amount) +
					String.format("선결제 승인일시 : %s\n", prepay_date) +
					String.format("선결재 거래번호 : %s\n", prepay_tran_no) +
					String.format("선결재 승인번호 : %s\n", prepay_auth_no);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV l]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "l", recvPayloadParsing, packet, "", userNo)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
			if (_app.isDebugMode())
			{
				usageKwh = "100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,0,0,0,0";
			}

			// sbNote 충전기 상태를 관리서버로 전송
			String[] usageKwhList = usageKwh.split(",");
			float kwhTotal = 0f;
			for (final String kwh : usageKwhList)
			{
				kwhTotal += Float.parseFloat(kwh.trim());
			}
			sendChargerState(charger, "l", -1, chargingStatus, kwhTotal, -1, -1, -1);

			// sbNote 현장 실물카드 결제의 경우는 웹서버에 결제 정보를 전송합니다.
			if (userNo.equals("E0C11001 A16EB253") && _app.isDeveloperServer())
			{
				userNo = "F0F0F0F0 10101010";

				String tmp = _app.getOnlyTestTime();
				prepay_amount = 500;
				prepay_auth_no = tmp;
				prepay_tran_no = tmp;
				prepay_date = "20" + tmp;
			}

			try
			{
				String finalUserNo = userNo;

				sendOcasPayment(charger, "",
						userNo.replaceAll(" ", "")
						, "l",
						chargingStartTime.replaceAll(" ", ""),
						chargingEndTime.replaceAll(" ", ""),
						chargingUnplugTime.replaceAll(" ", ""),
						usageKwh,
						prepay_amount,
						prepay_date,
						prepay_tran_no,
						prepay_auth_no).subscribe(paymentSuccess ->
				{
					int resp_code = RESP_0;

					int responseStatus = paymentSuccess.getInt("status");
					JSONObject data = paymentSuccess.getJSONObject("data");

					if (data.getInt("result") == 0)
					{
						if (data.has("ret_code"))
						{
							if (data.getString("ret_code").equals("1")) { resp_code = RESP_1; }
							else if (data.getString("ret_code").equals("2")) { resp_code = RESP_2; }
							else if (data.getString("ret_code").equals("3")) { resp_code = RESP_3; }
							else if (data.getString("ret_code").equals("4")) { resp_code = RESP_4; }
							else if (data.getString("ret_code").equals("5")) { resp_code = RESP_5; }
							else if (data.getString("ret_code").equals("6")) { resp_code = RESP_6; }
						}
					}

					byte[] payload;
					byte[] sendPacket;
					int payloadIdx = 0;

					// payload
					payload = new byte[1];

					payload[0] = ChargerCommandUtil.number2Hex(resp_code, 2)[1];

					int final_resp_code = resp_code;

					sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_L);
					sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
						// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
						// sbTodo 수신된 패킷의 정보를 서버로 전송
						String sendPayloadParsing = String.format("응답코드 : %s\n", final_resp_code);

						if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND l]\n" + sendPayloadParsing); }

						sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "l", sendPayloadParsing, sendPacket, "", finalUserNo)
								.subscribeOn(Schedulers.io())
								.subscribe();
					}, sendError -> {
						// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
					});
				}, paymentFail -> {

					byte[] payload;
					byte[] sendPacket;
					int payloadIdx = 0;

					// payload
					payload = new byte[1];

					payload[0] = ChargerCommandUtil.number2Hex(RESP_1, 2)[1];

					sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_L);
					sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
						// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
						// sbTodo 수신된 패킷의 정보를 서버로 전송
						String sendPayloadParsing = String.format("응답코드 : %s\n", RESP_0);

						if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND l]\n" + sendPayloadParsing); }

						sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "l", sendPayloadParsing, sendPacket, "", finalUserNo)
								.subscribeOn(Schedulers.io())
								.subscribe();
					}, sendError -> {
						// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
					});
				});
			}
			catch (Exception ex)
			{
				log.info("sendChargerToOcasL : " + ex.getMessage());
			}
		}
	}

	public void recvChargerToOcasM(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1});
		if (result != null)
		{
			int downloadType = ChargerCommandUtil.hex2Number(result[0]);

			String recvPayloadParsing = String.format("다운로드 종류 : %s\n", downloadType);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV m]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "m", recvPayloadParsing, packet)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
			byte[] payload;
			byte[] sendPacket;
			int payloadIdx = 0;
			try
			{
				// payload
				payload = new byte[14];

				// 펌웨어 이름 저장
				byte[] tmp = charger.getRecvLastFirmwareVersion().getBytes();
				for (int i = 0; i < tmp.length; i++)
				{
					payload[payloadIdx++] = tmp[i];
				}

				// 펌웨어 사이트 저장
				byte[] byteSize = ChargerCommandUtil.number2Hex(charger.getRecvLastFirmwareSize(), 4);
				System.arraycopy(byteSize, 0, payload, payloadIdx, byteSize.length);
				payloadIdx += byteSize.length;

				sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_M);
				sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
					// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
					// sbTodo 수신된 패킷의 정보를 서버로 전송
					String sendPayloadParsing = String.format("다운로드 버전 : %s\n", charger.getRecvLastFirmwareVersion()) +
							String.format("전체용량 : %s\n", charger.getRecvLastFirmwareSize());

					if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND m]\n" + sendPayloadParsing); }

					sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "m", sendPayloadParsing, sendPacket)
							.subscribeOn(Schedulers.io())
							.subscribe();
				}, sendError -> {
					// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
				});
			}
			catch (Exception ex)
			{
				log.info("sendChargerToOcasM : " + ex.getMessage());
			}
		}
	}

	public void recvChargerToOcasN(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{1, 10, 4});
		if (result != null)
		{
			int downloadType = ChargerCommandUtil.hex2Number(result[0]);
			String strDownloadVersion = new String(result[1], StandardCharsets.UTF_8).trim();
			int lastIndex = ChargerCommandUtil.hex2Number(result[2]);

			String recvPayloadParsing = String.format("다운로드 종류 : %s\n", downloadType) +
					String.format("다운로드 버전 : %s\n", strDownloadVersion) +
					String.format("데이터 인덱스 : %s\n", lastIndex);

			if (lastIndex == 0)
			{
				// sbNote 펌웨어 다운로드 시작하는 시점
				charger.setFirmwarePacketCurIndex(0);

				// // // sbNote 추가 200901 처리완료된 오프셋 비교위한 초기화
				// // charger.setFirmwareCompletedOffset(lastIndex);
				// // charger.setFirmwareLastOffset(lastIndex);
				// charger.setUpdateFirmwareDataList(new LinkedList<ChargerModel.FirmwareData>());

				/**
				 * 221337 / 256 = 864.59766
				 * 256 * 864 = 221,184 ==> 1728
				 * 221337 - 221,184 = 153
				 */
				int firmwarePacketCount = (int)Math.ceil(charger.getRecvLastFirmwareSize() / 256.0);
				charger.setFirmwarePacketCount(firmwarePacketCount - 1);
			}
			else
			{
				charger.setFirmwarePacketCurIndex(charger.getFirmwarePacketCurIndex() + 1);

				// // sbNote 추가 200901 처리완료된 오프셋 비교
				// charger.setFirmwareCompletedOffset(lastIndex);
			}

			// if (lastIndex > charger.getFirmwareCompletedOffset())
			// {
			//
			// }

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV n]\n" + recvPayloadParsing);
			}

			if (_app.isSendNCmdHistory())
			{
				// sbTodo 수신된 패킷의 정보를 서버로 전송
				sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "n", recvPayloadParsing, packet)
						.subscribeOn(Schedulers.io())
						.subscribe();
			}

			// // sbNote 추가 200901 처리완료된 오프셋 비교
			// charger.setFirmwareCompletedOffset(lastIndex);

			// sbTodo 펌웨어 다운로드 및 전송
			downloadFirmware(charger, lastIndex, packetGroupId);

			// // sbTodo 패킷 수신후 처리해야 하는 작업을 진행
			// byte[] payload;
			// byte[] sendPacket;
			// int payloadIdx = 0;
			// try
			// {
			//
			//     int sOffset = lastIndex;
			//     this.lastLength = (this.updateFirmwareFileSize - (lastIndex + 256)) > 0 ? 256 : this.updateFirmwareFileSize - lastIndex;
			//     lastIndex += this.lastLength;
			//
			//     if (this.lastLength == 0)    // 펌웨어 전송이 완료되었습니다
			//     {
			//         log.info("펌웨어 전송이 완료되었습니다.");
			//         return;
			//     }
			//
			//     byte[] data = Arrays.copyOfRange(this.updateFirmwareData, sOffset, sOffset + this.lastLength);
			//
			//     // SBCommonUtil.LogDebug(CustomApplication.TAG, "offset : " + sOffset + " ~ " + (sOffset + this.lastLength) + ", length : " + data.length + ", filesize : " + this.updateFirmwareFileSize);
			//
			//     payload = new byte[1 + 10 + 4 + 4 + this.lastLength];     // 다운로드종류, 다운로드버전, 전체용량, 데이터인덱스, 데이터
			//     // int payloadIdx = 0;
			//
			//     // 응답코드
			//     payload[payloadIdx++] = ChargerCommandUtil.number2Hex(RESP_0, 2)[1];
			//
			//     // 다운로드 버전
			//     byte[] tmp = strDownloadVersion.getBytes();
			//     for (int i = 0; i < tmp.length; i++)
			//     {
			//         payload[payloadIdx++] = tmp[i];
			//     }
			//
			//     // 전체용량
			//     byte[] byteSize = ChargerCommandUtil.number2Hex(charger.getRecvLastFirmwareSize(), 4);
			//     System.arraycopy(byteSize, 0, payload, payloadIdx, byteSize.length);
			//     payloadIdx += byteSize.length;
			//
			//     // 데이터 인덱스
			//     byte[] byteOffset = ChargerCommandUtil.number2Hex(sOffset, 4);
			//     System.arraycopy(byteOffset, 0, payload, payloadIdx, byteOffset.length);
			//     payloadIdx += byteOffset.length;
			//
			//     // 데이터
			//     for (int i = 0; i < data.length; i++)
			//     {
			//         payload[payloadIdx++] = data[i];
			//     }
			//
			//     sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_N);
			//     sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
			//         // sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
			//     }, sendError -> {
			//         // sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
			//     });
			// }
			// catch (Exception ex)
			// {
			//     log.info("sendChargerToOcasN : " + ex.getMessage());
			// }
		}
	}

	public void sendChargerToOcasN(ChargerModel charger, int lastIndex, long packetGroupId)
	{
		// // sbNote 추가 200901
		// //   지연으로 인한 재전송 요청이 발생하는 경우 이미 전송한 오프셋과 동일한 데이터는 무시 하기 위한 절차..
		// if (charger.getFirmwareCompletedOffset() == lastIndex) return;
		// charger.setFirmwareCompletedOffset(lastIndex);

		// sbTodo 패킷 수신후 처리해야 하는 작업을 진행
		byte[] payload;
		byte[] sendPacket;
		int payloadIdx = 0;
		try
		{
			int sOffset = lastIndex;
			int lastLength = (charger.getRecvLastFirmwareSize() - (lastIndex + 256)) > 0 ? 256 : charger.getRecvLastFirmwareSize() - lastIndex;
			lastIndex += lastLength;

			if (lastLength == 0)    // 펌웨어 전송이 완료되었습니다
			{
				log.info("펌웨어 전송이 완료되었습니다.");
				return;
			}

			// // ==========================================================
			// if (charger.getUpdateFirmwareDataList() == null || charger.getUpdateFirmwareDataList().size() == 0)
			// {
			// 	return;
			// }
			//
			// if (charger.getUpdateFirmwareDataList().size() > 1)
			// 	Collections.sort(charger.getUpdateFirmwareDataList());
			//
			// byte[] data = Arrays.copyOfRange(charger.getUpdateFirmwareDataList().pop().getData(), 0, lastLength);

			// sbNote 링크드 리스트 형식으로 변경하면서 주석 처리
			// byte[] data = Arrays.copyOfRange(this.updateFirmwareData, sOffset, sOffset + lastLength);
			byte[] data = Arrays.copyOfRange(charger.getUpdateFirmwareData(), 0, lastLength);
			// ==========================================================

			if (_app.isDebugLog())
			{
				log.debug("offset : " + sOffset + " ~ " + (sOffset + lastLength) + ", length : " + data.length + ", filesize : " + charger.getRecvLastFirmwareSize());
			}

			payload = new byte[1 + 10 + 4 + 4 + lastLength];     // 다운로드종류, 다운로드버전, 전체용량, 데이터인덱스, 데이터

			// 응답코드
			payload[payloadIdx++] = ChargerCommandUtil.number2Hex(RESP_0, 2)[1];

			// 다운로드 버전
			byte[] tmp = charger.getRecvLastFirmwareVersion().getBytes();
			for (int i = 0; i < tmp.length; i++)
			{
				payload[payloadIdx++] = tmp[i];
			}

			// 전체용량
			byte[] byteSize = ChargerCommandUtil.number2Hex(charger.getRecvLastFirmwareSize(), 4);
			System.arraycopy(byteSize, 0, payload, payloadIdx, byteSize.length);
			payloadIdx += byteSize.length;

			// 데이터 인덱스
			byte[] byteOffset = ChargerCommandUtil.number2Hex(sOffset, 4);
			System.arraycopy(byteOffset, 0, payload, payloadIdx, byteOffset.length);
			payloadIdx += byteOffset.length;

			// 데이터
			for (int i = 0; i < data.length; i++)
			{
				payload[payloadIdx++] = data[i];
			}

			sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_N);
			sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
				// sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
				// sbTodo 수신된 패킷의 정보를 서버로 전송
				String sendPayloadParsing = String.format("응답코드 : %s\n", RESP_0) +
						String.format("다운로드 버전 : %s\n", charger.getRecvLastFirmwareVersion()) +
						String.format("전체 용량 : %s\n", charger.getRecvLastFirmwareSize()) +
						String.format("데이터 인덱스 : %s\n", sOffset);
				// String.format("데이터 : %s\n", charger.getUserPoint());

				if (_app.isDebugLog()) { log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND n]\n" + sendPayloadParsing); }

				if (_app.isSendNCmdHistory())
				{
					sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "n", sendPayloadParsing, sendPacket)
							.subscribeOn(Schedulers.io())
							.subscribe();
				}
			}, sendError -> {
				// sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
			});
		}
		catch (Exception ex)
		{
			log.info("sendChargerToOcasN : " + ex.getMessage());
		}
	}

	public void recvChargerToOcasQ(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		// byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{6});
		// if (result != null)
		// {
		//     String chargerCurrentTime = ChargerCommandUtil.byte2DateTimeBCD(result[3]);
		//
		//     String recvPayloadParsing = String.format("현재 시간 : %s\n", chargerCurrentTime);
		//
		//     if (_app.isDebugLog()) { log.debug(String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV q]\n" + recvPayloadParsing); }
		//
		//     // sbTodo 수신된 패킷의 정보를 서버로 전송
		//     sendPacketLog(charger, Charger2Ocas, RECV, packetGroupId, "q", recvPayloadParsing, packet).subscribe();
		//
		//     // sbTodo 패킷 수신후 처리해야 하는 작업을 진행
		//     byte[] payload;
		//     byte[] sendPacket;
		//     int payloadIdx = 0;
		//     try
		//     {
		//         // payload
		//         payload = new byte[14];
		//
		//         payload[0] = ChargerCommandUtil.number2Hex(RESP_0, 2)[1];
		//
		//         sendPacket = assemblePacket(charger.getByteStationId(), charger.getByteChargerId(), payload, INS_CHARGER_TO_OCAS_Q);
		//         sendChargerToOcas(charger, sendPacket).subscribe(sendSuccess -> {
		//             // sbTodo 패킷 전송 성공에 대한 작업을 처리 (ex DB에 저장)
		//             // sbTodo 수신된 패킷의 정보를 서버로 전송
		//             String sendPayloadParsing = String.format("응답코드 : %s\n", RESP_0);
		//
		//             if (_app.isDebugLog()) { log.debug(String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing SEND q]\n" + sendPayloadParsing); }
		//             sendPacketLog(charger, Charger2Ocas, SEND, packetGroupId, "q", sendPayloadParsing, sendPacket).subscribe();
		//         }, sendError -> {
		//             // sbTodo validPacket 실패 또는 소켓 전송 실패에 대한 처리
		//         });
		//     }
		//     catch (Exception ex)
		//     {
		//         log.info("sendChargerToOcasQ : " + ex.getMessage());
		//     }
		// }
	}


	/**
	 * 웹서버 => 충전기 => OCAS
	 *
	 * @param charger
	 * @param packet
	 * @param packetGroupId
	 */
	public void recvOcasToChargerG(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{2, 4, 2, 2, 1, 1});
		if (result != null)
		{
			int chargerState = ChargerCommandUtil.hex2Number(result[0]);
			int chargerKWH = ChargerCommandUtil.hex2Number(result[1]);
			int chargerEventCode = ChargerCommandUtil.hex2Number(result[4]);
			int chargerRSSI = ChargerCommandUtil.hex2Number(result[5]);

			float temperature = (float) (Math.round(ChargerCommandUtil.hex2Number(result[2]) * 100) / 10000.0);         // 온도
			float humidity = (float) (Math.round(ChargerCommandUtil.hex2Number(result[3]) * 100) / 10000.0);            // 습도

			String recvPayloadParsing = String.format("충전기 상태 : %s\n", chargerState) +
					String.format("충천기 사용 전력량 : %s\n", chargerKWH) +
					String.format("온도 : %s\n", temperature) +
					String.format("습도 : %s\n", humidity) +
					String.format("이벤트 코드 : %s\n", chargerEventCode) +
					String.format("RSSI : %s\n", chargerRSSI);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV G]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Ocas2Charger, RECV, charger.getHttpHandlerPacketGroupId(), "G", recvPayloadParsing, packet)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbNote 충전기 상태를 관리서버로 전송
			sendChargerState(charger, "G", chargerState, -1, -1, chargerKWH, chargerEventCode, chargerRSSI);

			// sbNote 충전기로부터 수신된 정보를 웹서버로 전송
			httpResponse(charger, "G",
					String.format("{\"result\":1, \"charger_state\":\"%s\", \"kwh\":\"%s\", \"temperature\":\"%s\", \"humidity\":\"%s\", \"event_code\":\"%s\", \"rssi\":\"%s\"}",
							chargerState,
							chargerKWH,
							temperature,
							humidity,
							chargerEventCode,
							chargerRSSI));
		}
	}

	public void recvOcasToChargerH(@NonNull ChargerModel charger, @NonNull byte[] packet, long packetGroupId)
	{
		byte[][] result = ChargerCommandUtil.parsePayload(packet, 9, new int[]{2, 4, 1});
		if (result != null)
		{
			int chargerState = ChargerCommandUtil.hex2Number(result[0]);
			int chargerKWH = ChargerCommandUtil.hex2Number(result[1]);
			int retCode = ChargerCommandUtil.hex2Number(result[2]);

			String recvPayloadParsing = String.format("충전기 상태 : %s\n", chargerState) +
					String.format("충전기 사용 전력량 : %s\n", chargerKWH) +
					String.format("응답코드 : %s\n", retCode);

			if (_app.isDebugLog())
			{
				log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ", charger.getStrStationId(), charger.getStrChargerId()) + "[Parsing RECV H]\n" + recvPayloadParsing);
			}

			// sbTodo 수신된 패킷의 정보를 서버로 전송
			sendPacketLog(charger, Ocas2Charger, RECV, charger.getHttpHandlerPacketGroupId(), "H", recvPayloadParsing, packet)
					.subscribeOn(Schedulers.io())
					.subscribe();

			// sbNote 충전기 상태를 관리서버로 전송
			sendChargerState(charger, "H", chargerState, -1, chargerKWH, -1, -1, -1);

			// "result"            => ele('result', $ocas_response),
			// 	"msg"              => ele('msg', $ocas_response),
			// 	"charger_state"    => ele('charger_state', $ocas_response),
			// 	"kwh"              => ele('kwh', $ocas_response),
			// sbNote 충전기로부터 수신된 정보를 웹서버로 전송
			httpResponse(charger, "H",
					String.format("{\"result\":1, \"charger_state\":\"%s\", \"kwh\":\"%s\", \"retcode\":\"%s\"}",
							chargerState,
							chargerKWH,
							retCode));
		}
	}



	private int validPacket(String type, @NonNull byte[] packet)
	{
		type = type.toLowerCase();
		String strCmd = "";
		String strVD = "";
		String strLog = "";
		//String strValid = "";

		try
		{
			if (packet[0] != SOH)
			{
				log.warn("Invalid SOH");
				return RESP_2; // 전문오류
			}

			if (packet[packet.length - 1] != EOT)
			{
				log.warn("Invalid EOT");
				return RESP_2; // 전문오류
			}

			byte[] SID = Arrays.copyOfRange(packet, 1, 5);
			byte[] CID = Arrays.copyOfRange(packet, 5, 6);
			byte[] ML = Arrays.copyOfRange(packet, 7, 9);
			byte[] VD = Arrays.copyOfRange(packet, 9, packet.length - 3);
			byte[] CRC = Arrays.copyOfRange(packet, packet.length - 3, packet.length - 1);

			// @formatter:off
			switch (packet[6])
			{
                case INS_OCAS_2_CHARGER_A: strCmd = "A"; break;
                case INS_OCAS_2_CHARGER_B: strCmd = "B"; break;
                case INS_OCAS_2_CHARGER_C: strCmd = "C"; break;
                case INS_OCAS_2_CHARGER_D: strCmd = "D"; break;
                case INS_OCAS_2_CHARGER_E: strCmd = "E"; break;
                case INS_OCAS_2_CHARGER_F: strCmd = "F"; break;
                case INS_OCAS_2_CHARGER_G: strCmd = "G"; break;
                case INS_OCAS_2_CHARGER_H: strCmd = "H"; break;
                case INS_OCAS_2_CHARGER_I: strCmd = "I"; break;
                case INS_OCAS_2_CHARGER_J: strCmd = "J"; break;
                case INS_OCAS_2_CHARGER_K: strCmd = "K"; break;
                case INS_OCAS_2_CHARGER_L: strCmd = "L"; break;

                // 충전기 <==> OCAS 응답 명령어
				case INS_CHARGER_TO_OCAS_B: strCmd = "b"; break;
				case INS_CHARGER_TO_OCAS_D: strCmd = "d"; break;
				case INS_CHARGER_TO_OCAS_E: strCmd = "e"; break;
				case INS_CHARGER_TO_OCAS_F: strCmd = "f"; break;
				case INS_CHARGER_TO_OCAS_G: strCmd = "g"; break;
				case INS_CHARGER_TO_OCAS_H: strCmd = "h"; break;
				case INS_CHARGER_TO_OCAS_I: strCmd = "i"; break;
				case INS_CHARGER_TO_OCAS_J: strCmd = "j"; break;
				case INS_CHARGER_TO_OCAS_L: strCmd = "l"; break;
				case INS_CHARGER_TO_OCAS_M: strCmd = "m"; break;
				case INS_CHARGER_TO_OCAS_N: strCmd = "n"; break;
				case INS_CHARGER_TO_OCAS_Q: strCmd = "q"; break;

				default:
					strCmd = "unknown";
					break;
			}
			// @formatter:on

			strLog += "\n[SID] : " + ChargerCommandUtil.logByteToString(SID) + "(" + ChargerCommandUtil.hex2Number(SID) + ") ";
			strLog += "\n[CID] : " + ChargerCommandUtil.logByteToString(CID) + "(" + ChargerCommandUtil.hex2Number(CID) + ") ";
			strLog += "\n[INS] : " + strCmd + " ";
			strLog += "\n[ML] : " + ChargerCommandUtil.logByteToString(ML) + "(" + ChargerCommandUtil.hex2Number(ML) + ") ";

			if (type.equals("send") && !strCmd.equals("n"))
			{
				strLog += "\n[VD] : " + ChargerCommandUtil.logByteToString(VD) + " (len : " + VD.length + ") ";
			}

			strLog += "\n[CRC] : " + ChargerCommandUtil.logByteToString(CRC) + "(" + ChargerCommandUtil.hex2Number(CRC) + ") ";
			if (strVD.length() > 0)
			{
				strLog += "\n[VD Parse] : " + strVD;
			}

			// if (type.equals("send"))
			// {
			//     // log.debug("[validPacket SEND " + strCmd + "] " + strLog);
			// }
			// else
			// {
			//     log.debug("[validPacket RECV " + strCmd + "] " + strLog);
			// }

			if (VD.length != ChargerCommandUtil.hex2Number(ML))
			{
				if (type.equals("recv")) log.warn("[validPacket RECV " + strCmd + "] " + strLog);
				log.warn("ML Invalid");
				return RESP_2; // 전문오류
			}

			if (type.equals("recv"))
			{
				int calcCRC = ChargerCommandUtil.getCRC16(packet);
				int recvCRC = ChargerCommandUtil.hex2Number(CRC);
				if (recvCRC != (int) calcCRC)
				{
					log.warn("[validPacket RECV " + strCmd + "] " + strLog);
					log.warn("CRC Invalid [recv : " + recvCRC + "] [calc : " + calcCRC + "]");
				}

				return RESP_0; // sbNote CRC 오류가 있더라도 정상처리로...
			}
		}
		catch (Exception ex)
		{
			log.error(ex.getMessage());
			return RESP_2; // 전문오류
		}

		return RESP_0;
	}

	public byte[] assemblePacket(@NonNull byte[] byteStationId, @NonNull byte[] byteChargerId, @NonNull byte[] payload, byte instruction)
	{
		int headerLength = 1 + 4 + 1 + 1 + 2;    // SOH + 충전소 ID + 충전기 ID + INS + ML(데이터전송길이)
		int tailLength = 2 + 1;                  // CRC + EOT
		byte[] packet = new byte[headerLength + payload.length + tailLength];

		packet[0] = SOH;
		packet[1] = byteStationId[0];
		packet[2] = byteStationId[1];
		packet[3] = byteStationId[2];
		packet[4] = byteStationId[3];
		packet[5] = byteChargerId[0];
		packet[6] = instruction;

		if (packet[6] == 0x00)
		{
			return null;
		}

		packet[7] = (byte) ((payload.length & 0xFF00) >> 8);
		packet[8] = (byte) (payload.length & 0x00FF);
		//log += "[ML] : " + logByteToString(Arrays.copyOfRange(packet, 7, 9)) + "(" + payload.length + ") ";

		int packetIdx = 9;
		for (int idx = 0; idx < payload.length; idx++)
		{
			packet[packetIdx] = payload[idx];
			packetIdx++;
		}
		//log += "[VD] : " + logByteToString(Arrays.copyOfRange(packet, 9, packetIdx)) + " ";

		int checksum = ChargerCommandUtil.getCRC16(packet);
		packet[packet.length - 3] = (byte) ((checksum & 0xFF00) >> 8);
		packet[packet.length - 2] = (byte) (checksum & 0x00FF);
		//log += "[CRC] : " + logByteToString(Arrays.copyOfRange(packet, packet.length - 3, packet.length - 1)) + "(" + checksum + ") ";

		// CRC 계산후 tail에 추가
		packet[packet.length - 1] = EOT;

		return packet;
	}



	public Single<JSONObject> loadChargerInfo(ChargerModel charger, String stationID, String chargerID)
	{
		return Single.create(emitter -> {
			_app.getWebApi().getChargerInfo(stationID + "_" + chargerID)
					// .subscribeOn(Schedulers.io())
					.subscribe(responseBody -> {
						try
						{
							JSONObject res = new JSONObject(responseBody.string());
							boolean result = res.getBoolean("result");

							if (result)
							{
								JSONObject data = res.getJSONObject("data");

								String deviceName = (data.has("station_id") ? data.getString("station_id") : "")
										+ "-"
										+ (data.has("charger_id") ? data.getString("charger_id") : "");

								// Date currentDT = new Date();
								// charger.setRecvChargerInfoLastTime(currentDT.getTime());
								charger.setRecvChargerInfoLastTime(System.currentTimeMillis());

								charger.setStrEnvStationId((data.has("env_station_id") ? data.getString("env_station_id") : ""));

								// 충전기 공개/비공개 여부
								charger.setRecvOpen((data.has("open") ? data.getInt("open") : 1));

								try
								{
									charger.setRecvLastFirmwareVersion((data.has("charger_firmware_version") ? data.getString("charger_firmware_version") : ""));
									charger.setRecvLastFirmwareLink((data.has("charger_firmware_link") ? data.getString("charger_firmware_link") : ""));
									charger.setRecvLastFirmwareName((data.has("charger_firmware_name") ? data.getString("charger_firmware_name") : ""));
									charger.setRecvLastFirmwareSize((data.has("charger_firmware_size") ? data.getInt("charger_firmware_size") : 0));
									charger.setRecvLastFirmwareFilename((data.has("charger_firmware_filename") ? data.getString("charger_firmware_filename") : ""));
								}
								catch (Exception ex)
								{
									log.error("[" + deviceName + "] ==> " + ex.getMessage());
								}

								charger.setActive((data.has("active") ? data.getInt("active") : 0));
								charger.setStatus((data.has("status") ? data.getInt("status") : 0));


								// if (data.has("use_n_history"))
								// 	_app.setSendNCmdHistory(data.getInt("use_n_history") == 1);
								//
								// if (data.has("use_debug_log"))
								// 	_app.setDebugLog(data.getInt("use_debug_log") == 1);


								charger.setRecvChargerType((data.has("type") ? data.getString("type") : ""));
								charger.setRecvQuickTypeDcCombo((data.has("quick_dc_combo") && data.getString("quick_dc_combo").equals("1")));
								charger.setRecvQuickTypeCardemo((data.has("quick_type_cardemo") && data.getString("quick_type_cardemo").equals("1")));
								charger.setRecvQuickTypeAC3((data.has("quick_type_ac3") && data.getString("quick_type_ac3").equals("1")));

								charger.setRecvManufacturer((data.has("manufacturer") ? data.getString("manufacturer") : ""));
								charger.setRecvModelCode((data.has("model_code") ? data.getString("model_code") : ""));
								charger.setRecvConcurrentChannel((data.has("concurrent_channel") ? data.getInt("concurrent_channel") : 0));
								charger.setRecvFirmwareVersion((data.has("firmware_version") ? data.getString("firmware_version") : ""));
								charger.setRecvSound((data.has("sound") ? data.getInt("sound") : 0));
								charger.setRecvChannel((data.has("channel") ? data.getInt("channel") : 0));

								charger.setRecvKwh((data.has("kwh") ? data.getInt("kwh") : 0));                                             // 충전 요구 전력량 (H)
								charger.setRecvAmperage((data.has("amperage") ? data.getInt("amperage") : 0));                              // 충전 설정 전류량 (H)

								charger.setRecvAmperageChannel((data.has("amperage_channel") ? data.getInt("amperage_channel") : 0));               // 채널 설정 전류량 (D)

								ArrayList<Integer> unitPrice = new ArrayList<Integer>(24);
								JSONArray unitPriceJSONArray = data.has("unit_price") ? data.getJSONArray("unit_price") : new JSONArray();
								if (unitPriceJSONArray.length() == 24)
								{
									for (int idx = 0; idx < 24; idx++)
										unitPrice.add((int) unitPriceJSONArray.getInt(idx));
								}

								charger.setRecvUnitPrice(unitPrice);

								charger.setRecvNonMemberUnitPrice(data.getInt("unit_price_nonmember"));
								charger.setRecvEnvUnitPrice(data.getInt("env_price"));

								// log.info("[" + deviceName + "] ==> station id : " + data.getString("station_id"));
								// log.info("[" + deviceName + "] ==> charger id : " + data.getString("charger_id"));
								log.info("[" + deviceName + "] ==> 환경부 충전소 ID : " + charger.getStrEnvStationId());

								log.info("[" + deviceName + "] ==> last_firmware_version : " + charger.getRecvLastFirmwareVersion());
								log.info("[" + deviceName + "] ==> last_firmware_link : " + charger.getRecvLastFirmwareLink());
								log.info("[" + deviceName + "] ==> last_firmware_name : " + charger.getRecvLastFirmwareName());
								log.info("[" + deviceName + "] ==> last_firmware_size : " + charger.getRecvLastFirmwareSize());
								log.info("[" + deviceName + "] ==> last_firmware_filename : " + charger.getRecvLastFirmwareFilename());

								log.info("[" + deviceName + "] ==> charger type : " + data.getString("type"));

								log.info("[" + deviceName + "] ==> manufacturer : " + charger.getRecvManufacturer());
								log.info("[" + deviceName + "] ==> model code : " + charger.getRecvModelCode());
								log.info("[" + deviceName + "] ==> concurrent channel : " + charger.getRecvConcurrentChannel());
								log.info("[" + deviceName + "] ==> firmware version : " + charger.getRecvFirmwareVersion());
								log.info("[" + deviceName + "] ==> sound : " + charger.getRecvSound());
								log.info("[" + deviceName + "] ==> channel : " + charger.getRecvChannel());

								// Log.d(CustomApplication.TAG, "[" + deviceName + "] ==> 공급가능전력량 : " + getRecvAmperageSupplied());
								log.info("[" + deviceName + "] ==> 충전 요규 전력량 : " + charger.getRecvKwh());
								log.info("[" + deviceName + "] ==> 충전 설정 전류량 : " + charger.getRecvAmperage());
								log.info("[" + deviceName + "] ==> 채널 설정 전류량 : " + charger.getRecvAmperageChannel());

								log.info("[" + deviceName + "] ==> 충전단가 : " + charger.getRecvUnitPrice().toString());
								log.info("[" + deviceName + "] ==> 비회원 단가 : " + charger.getRecvNonMemberUnitPrice());

								charger.setSuccessChargerInfo(true);

								emitter.onSuccess(new JSONObject());
							}
							else
							{
								log.error(String.format("[%s-%s] 충전기 정보를 찾을수 없습니다.", stationID, chargerID));

								charger.setSuccessChargerInfo(false);
								emitter.onSuccess(new JSONObject());

								// emitter.onError(new Exception(String.format("[%s-%s] 충전기 정보를 가져올수 없습니다.", stationID, chargerID)));
							}
						}
						catch (JSONException e)
						{
							e.printStackTrace();
							emitter.onError(e);
						}
						catch (Exception e)
						{
							e.printStackTrace();
							emitter.onError(e);
						}
					}, error -> {
						// if (_app.isDebugLog()) log.debug("충전기 정보를 불러올수 없습니다.");
						// SBCommonUtil.LogError(CustomApplication.TAG, error.getMessage());
						emitter.onError(error);
					});
		});
	}

	public void downloadFirmware(ChargerModel charger, int offset, long packetGroupId)
	{
		//
		// sbNote 펌웨어 데이터 가져오는 방식,
		//  1 웹서버,
		//  2 로컬파일
		//
		if (_app.getFirmwareReadMode() == 1)
		{
			if (_app.isDebugLog())
			{
				log.debug("Download path : " + charger.getRecvLastFirmwareLink() + "&len=256&offset=" + offset);
			}

			/**
			 * sbNote 기존 파일 다운로드 사용 중지 ... 2020.04.03
			 */
			final DownloadInterface downloadService = new Retrofit.Builder().baseUrl(_app.getWebApiBaseUri()).client(new OkHttpClient()).build().create(DownloadInterface.class);

			Call<ResponseBody> call = downloadService.downloadFileByUrl(charger.getRecvLastFirmwareLink() + "&len=256&offset=" + offset);
			call.enqueue(new Callback<ResponseBody>()
			{
				@Override
				public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response)
				{
					if (response.isSuccessful())
					{
						if (_app.isDebugLog()) log.debug("server contacted and has file");

						// you can access headers of response
						String header = response.headers().get("Content-Disposition");

						// this is specific case, it's up to you how you want to save your file
						// if you are not downloading file from direct link, you might be lucky to obtain file name from header
						String fileName = header.replace("attachment; filename=", "");
						fileName = fileName.replaceAll("\"", "");

						ResponseBody body = response.body();

						try
						{
							assert body != null;

							if (body.contentLength() > 0)
							{
								byte[] fileReader = new byte[256];
								int read = body.byteStream().read(fileReader);
								charger.setUpdateFirmwareData(fileReader);
								charger.setUpdateFirmwareReadLength(read);

								sendChargerToOcasN(charger, offset, packetGroupId);
							}
							else
							{
								// sbTodo 서버로부터 전송받은 데이터가 없는 경우
							}
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
					else
					{
						log.error("server contact failed");
					}
				}

				@Override
				public void onFailure(Call<ResponseBody> call, Throwable t)
				{
					log.error("error");    // SocketTimeoutException
				}
			});
		}
		else if (_app.getFirmwareReadMode() == 2)
		{
			if (_app.isDebugLog())
			{
				log.debug("Download path : " + charger.getRecvLastFirmwareFilename() + "&len=256&offset=" + offset);
			}

			try
			{
				// 읽기 전용
				RandomAccessFile raf = new RandomAccessFile("/var/www/evrang/sites_evrang/root/" + charger.getRecvLastFirmwareFilename(), "r");

				byte[] buffer = new byte[256];
				raf.seek(offset);

				int readPos = 0;
				while (readPos < buffer.length)
				{
					int nread = raf.read(buffer, readPos, buffer.length - readPos);

					if (nread < 0)
					{
						break;
					}

					readPos += nread;
				}

				if (readPos > 0)
				{
					// ChargerModel.FirmwareData firmwareData = new ChargerModel.FirmwareData(offset);
					// firmwareData.setData(buffer);
					// charger.getUpdateFirmwareDataList().add(firmwareData);


					charger.setUpdateFirmwareData(buffer);
					charger.setUpdateFirmwareReadLength(readPos);

					sendChargerToOcasN(charger, offset, packetGroupId);
				}
				else
				{
					// sbTodo 서버로부터 전송받은 데이터가 없는 경우
				}
			}
			catch (IOException e)
			{
				if (_app.isDebugLog()) log.error(e.getMessage());

				e.printStackTrace();
			}
		}
	}

	public Single<JSONObject> sendOcasUserInfo(String memid, String card, String ins)
	{
		return Single.create(emitter -> {
			// sbTodo 수신된 패킷의 정보를 서버로 전송
			if (ins.toLowerCase().equals("g") == false)
			{
				emitter.onError(new Throwable("g 명령어의 경우만 결제를 진행할 수 있습니다."));
			}
			else
			{
				ConcurrentHashMap<String, Object> postParam = new ConcurrentHashMap<>();
				postParam.put("mem_id", memid);
				postParam.put("card", card);

				_app.getWebApi().ocas_user_info(postParam)
						// .subscribeOn(Schedulers.io())
						// .subscribeOn(Schedulers.from(_app.getLogExecutor())) // sbNote 원래 사용하던 스케쥴러
						.subscribe(responseBody -> {
							String strResponseBody = responseBody.string();

							log.debug(strResponseBody);

							try
							{
								JSONObject res = new JSONObject(strResponseBody);
								emitter.onSuccess(res);
							}
							catch (JSONException e)
							{
								e.printStackTrace();
								emitter.onError(e);
							}
							catch (Exception e)
							{
								e.printStackTrace();
								emitter.onError(e);
							}
						}, error -> {
							log.error(ins + ", 충전후 결제 실패, " + error.getMessage());
							emitter.onError(error);
						});
			}
		});
	}

	public Single<JSONObject> sendOcasPaymentAvailable(String memid, String card, String ins,
													   int prepay_amount, String prepay_date,
													   String prepay_tran_no, String prepay_auth_no)
	{
		return Single.create(emitter -> {
			// sbTodo 수신된 패킷의 정보를 서버로 전송
			if (ins.toLowerCase().equals("h") == false)
			{
				emitter.onError(new Throwable("h 명령어의 경우만 결제를 진행할 수 있습니다."));
			}
			else
			{
				ConcurrentHashMap<String, Object> postParam = new ConcurrentHashMap<>();
				postParam.put("mem_id", memid);
				postParam.put("card", card);

				postParam.put("prepay_amount", prepay_amount);
				postParam.put("prepay_date", prepay_date);
				postParam.put("prepay_tran_no", prepay_tran_no);
				postParam.put("prepay_auth_no", prepay_auth_no);

				_app.getWebApi().ocas_payment_available(postParam)
						// .subscribeOn(Schedulers.io())
						// .subscribeOn(Schedulers.from(_app.getLogExecutor())) // sbNote 원래 사용하던 스케쥴러
						.subscribe(responseBody -> {
							String strResponseBody = responseBody.string();

							// if (_app.isDebugLog())
							// {
							log.debug(strResponseBody);
							// }

							try
							{
								JSONObject res = new JSONObject(strResponseBody);
								emitter.onSuccess(res);
							}
							catch (JSONException e)
							{
								e.printStackTrace();
								emitter.onError(e);
							}
							catch (Exception e)
							{
								e.printStackTrace();
								emitter.onError(e);
							}
						}, error -> {
							log.error(ins + ", 충전후 결제 실패, " + error.getMessage());
							emitter.onError(error);
						});
			}
		});
	}

	public Single<JSONObject> sendOcasPayment(ChargerModel charger, String memid, String card, String ins,
											  String startDT, String endDT, String unplugDT, String kwh,
											  int prepay_amount, String prepay_date,
											  String prepay_tran_no, String prepay_auth_no)
	{
		return Single.create(emitter -> {
			// sbTodo 수신된 패킷의 정보를 서버로 전송
			if (ins.toLowerCase().equals("l") == false)
			{
				emitter.onError(new Throwable("l 명령어의 경우만 결제를 진행할 수 있습니다."));
			}
			else
			{
				ConcurrentHashMap<String, Object> postParam = new ConcurrentHashMap<>();
				postParam.put("mem_id", memid);
				postParam.put("card", card);
				postParam.put("sid", charger.getStrStationId());
				postParam.put("cid", charger.getStrChargerId());
				postParam.put("channel", charger.getRecvChannel());
				postParam.put("start", startDT);
				postParam.put("end", endDT);
				postParam.put("unplug", unplugDT);
				postParam.put("kwh", kwh);

				postParam.put("prepay_amount", prepay_amount);
				postParam.put("prepay_date", prepay_date);
				postParam.put("prepay_tran_no", prepay_tran_no);
				postParam.put("prepay_auth_no", prepay_auth_no);

				_app.getWebApi().ocas_payment(postParam)
						// .subscribeOn(Schedulers.io())
						// .subscribeOn(Schedulers.from(_app.getLogExecutor())) // sbNote 원래 사용하던 스케쥴러
						.subscribe(responseBody -> {

							String strResponseBody = responseBody.string();

							// if (_app.isDebugLog())
							// {
							log.debug(strResponseBody);
							// }

							try
							{
								JSONObject res = new JSONObject(strResponseBody);
								emitter.onSuccess(res);

								// boolean result = res.getBoolean("result");
								// if (result)
								// {
								//     JSONObject data = res.getJSONObject("data");
								//     if (data.has("ret_code") && data.getString("ret_code").equals("5"))
								//     {
								//
								//     }
								// }
							}
							catch (JSONException e)
							{
								e.printStackTrace();
								emitter.onError(e);
							}
							catch (Exception e)
							{
								e.printStackTrace();
								emitter.onError(e);
							}
						}, error -> {
							log.error(ins + ", 충전후 결제 실패, " + error.getMessage());
							emitter.onError(error);
						});
			}
		});
	}

	public Single<JSONObject> sendSavePrepayPayment(ChargerModel charger, String memid, String card, String ins,
													int prepay_amount, String prepay_date,
													String prepay_tran_no, String prepay_auth_no)
	{
		return Single.create(emitter -> {
			// sbTodo 수신된 패킷의 정보를 서버로 전송
			if (ins.toLowerCase().equals("h") == false)
			{
				emitter.onError(new Throwable("h 명령어의 경우만 비회원 현장 실물 결제 정보를 전송할수 있습니다."));
			}
			else
			{
				ConcurrentHashMap<String, Object> postParam = new ConcurrentHashMap<>();
				postParam.put("mem_id", memid);
				postParam.put("card", card);
				postParam.put("sid", charger.getStrStationId());
				postParam.put("cid", charger.getStrChargerId());
				postParam.put("channel", charger.getRecvChannel());

				postParam.put("prepay_amount", prepay_amount);
				postParam.put("prepay_date", prepay_date);
				postParam.put("prepay_tran_no", prepay_tran_no);
				postParam.put("prepay_auth_no", prepay_auth_no);

				_app.getWebApi().ocas_save_prepay_payment(postParam)
						// .subscribeOn(Schedulers.io())
						// .subscribeOn(Schedulers.from(_app.getLogExecutor())) // sbNote 원래 사용하던 스케쥴러
						.subscribe(responseBody -> {

							String strResponseBody = responseBody.string();

							// if (_app.isDebugLog())
							// {
							log.debug(strResponseBody);
							// }

							try
							{
								JSONObject res = new JSONObject(strResponseBody);
								emitter.onSuccess(res);

								// boolean result = res.getBoolean("result");
								// if (result)
								// {
								//     JSONObject data = res.getJSONObject("data");
								//     if (data.has("ret_code") && data.getString("ret_code").equals("5"))
								//     {
								//
								//     }
								// }
							}
							catch (JSONException e)
							{
								e.printStackTrace();
								emitter.onError(e);
							}
							catch (Exception e)
							{
								e.printStackTrace();
								emitter.onError(e);
							}
						}, error -> {
							log.error(ins + ", 현장 실물 결제 정보 전송 실패, " + error.getMessage());
							emitter.onError(error);
						});
			}
		});
	}

	public Single<JSONObject> sendPacketLog(ChargerModel charger, String type, String sendRecv, long packetGroupId, String ins, String desc, @Nonnull byte[] packet)
	{
		return sendPacketLog(charger, type, sendRecv, packetGroupId, ins, desc, packet, "", "");
	}

	public Single<JSONObject> sendPacketLog(ChargerModel charger, String type, String sendRecv, long packetGroupId, String ins, String desc, @Nonnull byte[] packet, String memid, String card)
	{
		return Single.create(emitter -> {

			//
			// sbNote 패킷 로그 저장방식,
			//  0 사용안함,
			//  1 웹서버,
			//  2 Direct Insert,
			//  3 Direct Insert and Query Batch Insert
			//
			if (_app.getPacketLogWriteMode() == 0)
			{

			}
			else if (_app.getPacketLogWriteMode() == 1)
			{
				/**
				 * 웹서버를 통한 패킷 로그 저장 방식
				 */
				// sbTodo 수신된 패킷의 정보를 서버로 전송
				ConcurrentHashMap<String, Object> packetLog = new ConcurrentHashMap<>();
				packetLog.put("gid", packetGroupId);
				packetLog.put("type", type);
				packetLog.put("send_recv", sendRecv);
				packetLog.put("sid", charger.getStrStationId());
				packetLog.put("cid", charger.getStrChargerId());
				packetLog.put("memid", memid);
				packetLog.put("card", card);
				packetLog.put("ins", ins);
				packetLog.put("packet", ChargerCommandUtil.byte2HexString(packet));
				packetLog.put("desc", desc);

				// _app.getPacketLogQueue().add(packetLog);

				_app.getWebApi().packet_log(packetLog)
						.subscribeOn(Schedulers.io())
						// .subscribeOn(Schedulers.from(_app.getLogExecutor())) // sbNote 원래 사용하던 스케쥴러
						.subscribe(success -> {
							if (_app.isDebugLog())
							{
								// log.debug(String.format("[%s-%s] Packet log send, 명령어 : %s, 타입 : %s, 응답 :  %s",
								//         charger.getStrStationId(), charger.getStrChargerId(), ins, type, success.string()));
							}
						}, error -> {
							log.error(ins + ", " + type + ", " + error.getMessage());
						});
			}
			else if (_app.getPacketLogWriteMode() == 2)
			{
				String sql = "insert into sb_packet_history(pkl_type, pkl_group_id, pkl_station_id, pkl_charger_id, pkl_mem_id, pkl_card_no, pkl_ins, pkl_packet, pkl_desc, pkl_ip, pkl_send_recv) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				PreparedStatement pstmt = null;
				try
				{
					if (_app.getDbConn() == null || _app.getDbConn().isClosed())
					{
						_app.connectDB();
					}

					// 3. PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장
					pstmt = _app.getDbConn().prepareStatement(sql);

					// 4. pstmt.set<데이터타입>(? 순서, 값) ex).setString(), .setInt ...
					pstmt.setString(1, type);
					pstmt.setLong(2, packetGroupId);
					pstmt.setString(3, charger.getStrStationId());
					pstmt.setString(4, charger.getStrChargerId());

					// Integer.parseInt(memid);
					// if (memid == null || memid.isEmpty())
					pstmt.setNull(5, java.sql.Types.NULL);
					// else
					// 	pstmt.setInt(5, Integer.parseInt(memid));

					pstmt.setString(6, card);
					pstmt.setString(7, ins);
					pstmt.setString(8, ChargerCommandUtil.byte2HexString(packet));
					pstmt.setString(9, desc);
					pstmt.setString(10, charger.getRemoteAddress());
					pstmt.setString(11, sendRecv);

					boolean isExecuteQuery = true;
					if (ins.equals("n"))
					{
						if (charger.getFirmwarePacketCurIndex() > 0
								&& charger.getFirmwarePacketCurIndex() < charger.getFirmwarePacketCount())
						{
							log.debug("펌웨어 전송 로그 무시 : " + charger.getFirmwarePacketCurIndex() + ", " + charger.getFirmwarePacketCount());
							log.debug(pstmt.toString());
							isExecuteQuery = false;
						}
					}

					if (isExecuteQuery)
					{
						// 5. SQL 문장을 실행하고 결과를 리턴 - SQL 문장 실행 후, 변경된 row 수 int type 리턴
						int r = pstmt.executeUpdate();
						// pstmt.excuteQuery() : select
						// pstmt.excuteUpdate() : insert, update, delete ..
						log.debug("packet history 변경된 row : " + r);
					}
				}
				catch (SQLException e)
				{
					log.error("[SQL Error : " + e.getMessage() + "]");
				}
				finally
				{
					// 사용순서와 반대로 close 함
					if (pstmt != null)
					{
						try
						{
							pstmt.close();
						}
						catch (SQLException e)
						{
							log.error(e.getMessage());
							e.printStackTrace();
						}
					}
					// if (con != null)
					// {
					// 	try { con.close(); }
					// 	catch (SQLException e) { e.printStackTrace(); }
					// }
				}
			}
			else if (_app.getPacketLogWriteMode() == 3)
			{

			}
		});
	}

	// public Single<JSONObject> sendChargerState(ChargerModel charger, String ins, int chargerState, int chargingState, float kwh, int eventCode, int rssi)
	public void sendChargerState(ChargerModel charger, String ins, int chargerState, int chargingState, float kwhCurrent, float kwhCumulative, int eventCode, int rssi)
	{
		// return Single.create(emitter -> {

		// sbTodo 수신된 패킷의 정보를 서버로 전송
		ConcurrentHashMap<String, Object> packetLog = new ConcurrentHashMap<>();

		packetLog.put("sid", charger.getStrStationId());
		packetLog.put("cid", charger.getStrChargerId());
		packetLog.put("channel", charger.getRecvChannel());

		packetLog.put("firmware", charger.getStrFirmwareInsb());
		packetLog.put("mdn", charger.getStrMDNInsb());

		if (chargerState >= 0)
		{ packetLog.put("charger_state", chargerState); }

		if (chargingState >= 0)
		{ packetLog.put("charging_state", chargingState); }

		if (kwhCurrent >= 0)
		{ packetLog.put("kwh", kwhCurrent); }

		if (kwhCumulative >= 0)
		{ packetLog.put("kwh_cumulative", kwhCumulative); }

		if (eventCode >= 0)
		{ packetLog.put("event_code", eventCode); }

		if (rssi >= 0)
		{ packetLog.put("rssi", rssi); }

		packetLog.put("last_ins", ins);

		log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] Packet log send, 명령어 : %s, ",
				charger.getStrStationId(), charger.getStrChargerId(), ins));

		_app.getWebApi().ocas_charger_state(packetLog)
				.subscribeOn(Schedulers.io())
				// .subscribeOn(Schedulers.from(_app.getStateExecutor()))
				.subscribe(success -> {
					if (_app.isDebugLog())
					{
						// log.debug(String.format("[%s-%s] Packet log send, 명령어 : %s, 타입 : %s, 응답 :  %s",
						//         charger.getStrStationId(), charger.getStrChargerId(), ins, type, success.string()));
					}
				}, error -> {
					log.error(ins + ", " + error.getMessage());
				});

		// });
	}

	public void sendMEChargerStatus(ChargerModel charger, int chargerStatus, int chargingStatus, int forceChangeStatus)
	{
		float diff = System.currentTimeMillis() - charger.getLastPacketTime();
		float sec = diff / 1000F;
		float minutes = sec / 60F;

		if (!charger.getStrEnvStationId().isEmpty() && charger.getStrEnvStationId().length() == 6)
		{
			int status = 0;

			// sbTodo 충전기의 이전상태 및 동일한 상태일 경우 전송주기 검사????

			if (charger.getRecvOpen() == 1)
			{
				// if (charger.getActive() == 1)
				// {
				// // sbNote 충전기 상태 정보 (환경부 코드 기준)
				// public static final int ME_CHARGER_STATE_UNKNOWN = 0;
				// public static final int ME_CHARGER_STATE_ERROR = 1;
				// public static final int ME_CHARGER_STATE_READY = 2;
				// public static final int ME_CHARGER_STATE_CHARGING = 3;
				// public static final int ME_CHARGER_STATE_OUT_OF_SERVICE = 4;
				// public static final int ME_CHARGER_STATE_CHECKING = 5;
				// public static final int ME_CHARGER_STATE_RESERVED = 6;
				// public static final int ME_CHARGER_STATE_UNCHECKED = 9;

				if (chargingStatus == -1)
				{
					// if ((chargerStatus & 1) == 1) tmp += "커넥터 연결, ";
					// if ((chargerStatus & 2) == 2) tmp += "충전중, ";
					// if ((chargerStatus & 4) == 4) tmp += "정의되지 않은 오류, ";
					// if ((chargerStatus & 8) == 8) tmp += "비상 스위치 눌림, ";
					// if ((chargerStatus & 16) == 16) tmp += "전원오류, ";
					// if ((chargerStatus & 32) == 32) tmp += "RCD 차단, ";
					// if ((chargerStatus & 64) == 64) tmp += "MC 융착, ";
					// if ((chargerStatus & 128) == 128) tmp += "Pilot 오류, ";
					// if ((chargerStatus & 256) == 256) tmp += "메모리 오류, ";

					// @formatter:off
                    switch (chargerStatus)
                    {
                        case 0:
                        case 1:
                            status = ME_CHARGER_STATE_READY;
                            break;

                        case 2:     // sbNote 커넥터 분리되었으나 충전이 종료되는 과정에 발생 ??
                        case 3:
                            status = ME_CHARGER_STATE_CHARGING;
                            break;

                        default:
                            // status = ME_CHARGER_STATE_ERROR;
                            status = ME_CHARGER_STATE_CHECKING;
                            break;
                    }
                    // @formatter:on
				}
				else
				{
					// // sbNote 충전상태
					// public static final int STATE_CHARGE_READY = 0;                 // 대기
					// public static final int STATE_CHARGE_RESERVED = 1;              // 예약대기
					// public static final int STATE_CHARGE_CHARGING = 2;              // 충전
					// public static final int STATE_CHARGE_STOP_USER = 3;             // 사용자 종료
					// public static final int STATE_CHARGE_STOP_CAR = 4;              // 차량 종료
					// public static final int STATE_CHARGE_STOP_CHARGER = 5;          // 충전기 종료
					// public static final int STATE_CHARGE_STOP_COMM_ERROR = 6;       // 전기차 통신 오류 종료
					// public static final int STATE_CHARGE_STOP_LOW_VOLTAGE = 7;      // 저전압 종료
					// public static final int STATE_CHARGE_STOP_HIGH_VOLTAGE_ = 8;    // 과전압 종료
					// public static final int STATE_CHARGE_STOP_OVERCURRENT = 9;      // 과전류 종료
					// public static final int STATE_CHARGE_STOP_MC = 10;              // MC 융착 종료
					// public static final int STATE_CHARGE_STOP_EMERGENCY = 11;       // 비상 스위치 종료
					// public static final int STATE_CHARGE_STOP_RCD = 12;             // RCD 차단 종료
					// public static final int STATE_CHARGE_STOP_POWER_OFF = 13;       // 전원 OFF 종료

					// @formatter:off
                    switch (chargingStatus)
                    {
                        case STATE_CHARGE_READY:
                            status = ME_CHARGER_STATE_READY;
                            break;

                        case STATE_CHARGE_RESERVED:
                            status = ME_CHARGER_STATE_RESERVED;
                            break;

                        case STATE_CHARGE_CHARGING:
                            status = ME_CHARGER_STATE_CHARGING;
                            break;

                        case STATE_CHARGE_STOP_USER:
                        case STATE_CHARGE_STOP_CAR:
                        case STATE_CHARGE_STOP_CHARGER:
                            status = ME_CHARGER_STATE_CHARGING;
                            break;

                        case STATE_CHARGE_STOP_COMM_ERROR:
                        case STATE_CHARGE_STOP_LOW_VOLTAGE:
                        case STATE_CHARGE_STOP_HIGH_VOLTAGE:
                        case STATE_CHARGE_STOP_OVERCURRENT:
                        case STATE_CHARGE_STOP_MC:
                        case STATE_CHARGE_STOP_EMERGENCY:
                        case STATE_CHARGE_STOP_RCD:
                        case STATE_CHARGE_STOP_POWER_OFF:
                        default:
                            // status = ME_CHARGER_STATE_ERROR;
                            status = ME_CHARGER_STATE_CHECKING;
                            break;
                    }
                    // @formatter:on
				}
			}
			else
			{
				status = ME_CHARGER_STATE_OUT_OF_SERVICE;
				//     if (charger.getActive() == 2)
				//         status = ME_CHARGER_STATE_OUT_OF_SERVICE;
				//     else if (charger.getActive() == 3)
				//         status = ME_CHARGER_STATE_CHECKING;
				//     else
				//         status = ME_CHARGER_STATE_UNKNOWN;
			}

			String messages = String.format("{\"bid\":\"%s\", \"bkey\":\"%s\", \"cstat\":[{\"sid\":\"%s\", \"cid\":\"%s\", \"status\":\"%d\" }]}",
					_app.getMeBid(),
					_app.getMeBkey(),
					charger.getStrEnvStationId(),
					charger.getStrChargerId(),
					status);

			log.info("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] 환경부 전송 : %s", charger.getStrStationId(), charger.getStrChargerId(), messages));

			if (!_app.isDebugMode() && !_app.isIgnoreMeApi())
			{
				ConcurrentHashMap<String, Object> statusParam = new ConcurrentHashMap<>();
				statusParam.put("messages", messages);

				_app.getMeApi().charger_status(statusParam)
						.subscribeOn(Schedulers.io())
						.subscribe(success -> {
							// log.info(String.format("[%s-%s] 환경부 응답 : %s", charger.getStrStationId(), charger.getStrChargerId(), success.string()));
							log.info("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] 환경부 응답", charger.getStrStationId(), charger.getStrChargerId()));
							log.info(success.string());
						}, error -> {
							// log.error(String.format("[%s-%s] 환경부 응답 : %s", charger.getStrStationId(), charger.getStrChargerId(), error.getMessage()));
							log.error("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] 환경부 응답", charger.getStrStationId(), charger.getStrChargerId()));
							log.error(error.getMessage());
						});
			}
			else
			{
				log.info("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] 환경부 전송 무시", charger.getStrStationId(), charger.getStrChargerId()));
			}
		}
	}

	public void httpResponse(ChargerModel charger, String ins, String responseJsonBody)
	{
		// return Single.create(emitter -> {
		ChannelHandlerContext ctx = charger.getHttpCtx();
		if (ctx == null)
		{
			log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ins %s - %s", charger.getStrStationId(), charger.getStrChargerId(), ins, "HttpResponse failed " + responseJsonBody));
			return;
		}

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
		ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		// ctx.write(response).addListener(ChannelFutureListener.CLOSE);

		// //write happens here when you are in event executor already
		// ctx.executor().execute(() -> {
		// 	ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		// });

		// // sbNote 핸들러 초기화
		charger.setHttpCtx(null);

		log.debug("[" + charger.getRemoteAddress() + "] " + String.format("[%s-%s] ins %s - %s", charger.getStrStationId(), charger.getStrChargerId(), ins, "HttpResponse success " + responseJsonBody));
		// });
	}
}
