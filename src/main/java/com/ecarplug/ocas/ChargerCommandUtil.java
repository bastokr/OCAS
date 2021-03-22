package com.ecarplug.ocas;


import io.reactivex.annotations.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.Arrays;

@Slf4j
public class ChargerCommandUtil
{
	// private static final String TAG = "ChargerCommandUtil";

	private static final int[] crcTable = {
			0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
			0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
			0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
			0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
			0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
			0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
			0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
			0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
			0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
			0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
			0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
			0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
			0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
			0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
			0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
			0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
			0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
			0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
			0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
			0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
			0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
			0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
			0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
			0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
			0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
			0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
			0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
			0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
			0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
			0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
			0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
			0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040
	};

	/**
	 * 충전소 ID ~ DATA 까지의 정보로 체크섬을 계산...
	 *
	 * @param data
	 * @return
	 */
	public static int getCRC16(@NonNull byte[] data)
	{
		int startOffset = 1;                // SOH 제외 (1byte)
		int endOffset = data.length - 3;    // CRC, EOT 제외 (3byte)
		byte[] crcBody = Arrays.copyOfRange(data, startOffset, endOffset);

		int crc = 0xffff;
		for (int i = 0; i < crcBody.length; ++i)
		{
			crc = (crc >> 8) ^ crcTable[(crc ^ crcBody[i]) & 0x00ff];
		}

		return (int) crc;
	}

	private static int certificationCRC16(@NonNull byte[] data)
	{
		int startOffset = 0;
		int endOffset = data.length;
		byte[] crcBody = Arrays.copyOfRange(data, startOffset, endOffset);

		int crc = 0xffff;
		for (int i = 0; i < crcBody.length; ++i)
		{
			crc = (crc >> 8) ^ crcTable[(crc ^ crcBody[i]) & 0x00ff];
		}

		return (int) crc;
	}

	public static BigInteger makeCertificationNumber(int in_uiStationID, int in_ucChargerID, @NonNull byte[] in_pucTime)
	{
		BigInteger ulCertificationNumber = BigInteger.ZERO;

		String recvDT = byte2DateTimeBCDForCertification(in_pucTime);

		// Make Crc 1
		String result = String.format("ecarPlug%s", recvDT);
		byte[] resultByte = result.getBytes();
		long crcOri = certificationCRC16(resultByte);
		ulCertificationNumber = BigInteger.valueOf(crcOri).shiftLeft(48);
		log.debug("[RECV Q] CRC : " + result + ", " + BigInteger.valueOf(crcOri).toString(16) + ", " + ulCertificationNumber.toString(16));

		// Make Crc 2
		result = String.format("%08d%s", in_uiStationID, recvDT); // 8 + 12
		resultByte = result.getBytes();
		crcOri = certificationCRC16(resultByte);
		BigInteger crc2 = BigInteger.valueOf(crcOri).shiftLeft(32);
		ulCertificationNumber = crc2.or(ulCertificationNumber);
		log.debug("[RECV Q] CRC : " + result + " ==> " + BigInteger.valueOf(crcOri).toString(16) + ", " + crc2.toString(16) + ", " + ulCertificationNumber.toString(16));

		// Make Crc 3
		result = String.format("EVRang%s", recvDT); // 8 + 12
		resultByte = result.getBytes();
		crcOri = certificationCRC16(resultByte);
		BigInteger crc3 = BigInteger.valueOf(crcOri).shiftLeft(16);
		ulCertificationNumber = crc3.or(ulCertificationNumber);
		log.debug("[RECV Q] CRC : " + result + " ==> " + BigInteger.valueOf(crcOri).toString(16) + ", " + crc3.toString(16) + ", " + ulCertificationNumber.toString(16));

		// Make Crc 4
		result = String.format("%02d%s", in_ucChargerID, recvDT); // 8 + 12
		resultByte = result.getBytes();
		crcOri = certificationCRC16(resultByte);
		BigInteger crc4 = BigInteger.valueOf(crcOri);
		ulCertificationNumber = crc4.or(ulCertificationNumber);
		log.debug("[RECV Q] CRC : " + result + " ==> " + BigInteger.valueOf(crcOri).toString(16) + ", " + crc4.toString(16) + ", " + ulCertificationNumber.toString(16));

		return ulCertificationNumber;
	}

	// public static String parseStationChargerID(@NonNull byte[] payload)
    // {
    //     // 31020005_01
    //     byte[] byteStationId = Arrays.copyOfRange(payload, 1, 5);
    //     byte[] byteChargerId = new byte[1];
    //     byteChargerId[0] = payload[5];
    //
    //     int intStationId = ChargerCommandUtil.hex2Number(byteStationId);
    //     int intChargerId = ChargerCommandUtil.hex2Number(byteChargerId[0]);
    //     String strStationId = String.valueOf(intStationId);
    //     String strChargerId = String.format("%02d", intChargerId);
    //
    //     return strStationId + "_" + strChargerId;
    // }

    public static int getML(@NonNull byte[] payload)
    {
        byte[] ML = Arrays.copyOfRange(payload, 7, 9);
        return ChargerCommandUtil.hex2Number(ML);
    }

	/**
	 * 수신된 패킷의 정보를 분리
	 *
	 * @param payload
	 * @param offset
	 * @param splitSize
	 * @return
	 */
	public static byte[][] parsePayload(@NonNull byte[] payload, int offset, @NonNull int[] splitSize)
	{
		try
		{
			byte[][] result = new byte[splitSize.length][];

			int idx = 0;
			int payloadIdx = offset;
			for (final int size : splitSize)
			{
				result[idx] = Arrays.copyOfRange(payload, payloadIdx, payloadIdx + size);
				idx++;
				payloadIdx += size;
			}

			return result;
		}
		catch (Exception ex)
		{
			log.error("parsePayload : " + ex.getMessage());
		}

		return null;
	}

	public static int hex2Number(byte buffer)
	{
		int value = Integer.MIN_VALUE;
		value = (buffer & 0x000000FF);
		return value;
	}

	public static int hex2Number(@NonNull byte[] buffer)
	{
		int value = Integer.MIN_VALUE;

		switch (buffer.length)
		{
			case 4:
				value = (buffer[3] & 0x000000FF) + ((buffer[2] << 8) & 0x0000FF00) + ((buffer[1] << 16) & 0x00FF0000) + ((buffer[0] << 24) & 0xFF000000);
				break;

			case 3:
				value = (buffer[2] & 0x000000FF) + ((buffer[1] << 8) & 0x0000FF00) + ((buffer[0] << 16) & 0x00FF0000);
				break;

			case 2:
				value = (buffer[1] & 0x000000FF) + ((buffer[0] << 8) & 0x0000FF00);
				break;

			case 1:
				value = (buffer[0] & 0x000000FF);
				break;
		}

		return value;
	}

	public static byte[] number2Hex(int value, int byteLength)
	{
		byte[] tmp = new byte[byteLength];

		int mask = 0xFF000000;
		if (byteLength == 2) mask = 0xFF00;

		for (int idx = 0; idx < byteLength; idx++)
		{
			int shiftBit = 8 * (byteLength - (idx + 1));
			tmp[idx] = (byte) ((value & (mask >> (idx * 8))) >> shiftBit);
		}

		return tmp;

		// payload[payloadIdx++] = (byte)((value & 0xFF000000) >> 24);
		// payload[payloadIdx++] = (byte)((value & 0x00FF0000) >> 16);
		// payload[payloadIdx++] = (byte)((value & 0x0000FF00) >> 8);
		// payload[payloadIdx++] = (byte)(value & 0x000000FF);
	}

	public static byte[] bigIntegerNumber2Hex(BigInteger value, int byteLength)
	{
		byte[] tmp = new byte[byteLength];

		for (int idx = 0; idx < byteLength; idx++)
		{
			int shiftBit = 8 * (byteLength - (idx + 1));
			BigInteger and1 = new BigInteger("FF00000000000000", 16).shiftRight(idx * 8);
			BigInteger tmp1 = value.and(and1);
			BigInteger tmp2 = tmp1.shiftRight(shiftBit);
			tmp[idx] = (byte)tmp2.intValue();
			log.debug(value + ", " + value.toString(16) + ", " + and1.toString(16) + ", " + tmp1.toString(16) + ", " + tmp2.toString(16) + ", " + tmp2.intValue());
		}

		// 2019-04-10 14:30:25.913 D/Peripheral: [RECV Q] CRC : ecarPlug190410143019 ==> 7370422265168527360 ==> 7370422265168527360
		// 2019-04-10 14:30:25.921 D/Peripheral: [RECV Q] CRC : 31160000190410143019 ==> 122303488720896 ==> 7370544568657248256 ==> 7370544568657248256
		// 2019-04-10 14:30:25.927 D/Peripheral: [RECV Q] CRC : EVRang190410143019 ==> 2814705664 ==> 7370544571471953920 ==> 7370544571471953920
		// 2019-04-10 14:30:25.933 D/Peripheral: [RECV Q] CRC : 00190410143019 ==> 5965 ==> 7370544571471959885 ==> 7370544571471959885
		// 2019-04-10 14:30:25.936 D/Peripheral: 7370544571471959885, 66496f3ca7c5174d, ff00000000000000, 6600000000000000, 66, 102
		// 2019-04-10 14:30:25.938 D/Peripheral: 7370544571471959885, 66496f3ca7c5174d, ff000000000000, 49000000000000, 49, 73
		// 2019-04-10 14:30:25.941 D/Peripheral: 7370544571471959885, 66496f3ca7c5174d, ff0000000000, 6f0000000000, 6f, 111
		// 2019-04-10 14:30:25.943 D/Peripheral: 7370544571471959885, 66496f3ca7c5174d, ff00000000, 3c00000000, 3c, 60
		// 2019-04-10 14:30:25.945 D/Peripheral: 7370544571471959885, 66496f3ca7c5174d, ff000000, a7000000, a7, 167
		// 2019-04-10 14:30:25.946 D/Peripheral: 7370544571471959885, 66496f3ca7c5174d, ff0000, c50000, c5, 197
		// 2019-04-10 14:30:25.948 D/Peripheral: 7370544571471959885, 66496f3ca7c5174d, ff00, 1700, 17, 23
		// 2019-04-10 14:30:25.949 D/Peripheral: 7370544571471959885, 66496f3ca7c5174d, ff, 4d, 4d, 77

		return tmp;
	}

	public static String byte2DateTimeBCD(@NonNull byte[] buffer)
	{
		if (buffer.length != 6)
			return "";

		return String.format("%02x%02x%02x %02x%02x%02x", buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5]);
	}

	// public static byte[] datetimeBCD2Byte(String datetime)
	// {
	// 	byte[] tmp = new byte[6];
	// 	for (int idx = 0; idx < tmp.length; idx++) tmp[idx] = 0x00;
	//
	// 	String replaceDatetime = datetime.replaceAll("/ /gi", "");
	// 	int length = replaceDatetime.length();
	// 	for (int idx = 0; idx >= 0; idx--)
	// 	{
	// 		// Log.d(CustomApplication.TAG, idx + ", " + (length - 2) + " ~ " + length);
	// 		tmp[idx] = (byte)Integer.parseInt(replaceOrderNo.substring(length - 2, length), 16);
	// 		length -= 2;
	//
	// 		log.debug(idx + ", " + (length - 2) + " ~ " + length + " => " + String.format("%02X", tmp[idx]));
	// 	}
	//
	// 	return tmp;
	// }

	private static String byte2DateTimeBCDForCertification(@NonNull byte[] buffer)
	{
		if (buffer.length != 6)
			return null;

		return String.format("%02x%02x%02x%02x%02x%02x", buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5]);

		// char[] ret = new char[13];
		// for (int idx = 0; idx < 12; idx++)
		//     ret[idx] = strDT.charAt(idx);
		//
		// ret[12] = 0;
		//
		// return ret;
	}

	public static String parseOrderNo(@NonNull byte[] buffer)
	{
		return String.format("%02X%02X%02X%02X %02X%02X%02X%02X",
				buffer[0],
				buffer[1],
				buffer[2],
				buffer[3],
				buffer[4],
				buffer[5],
				buffer[6],
				buffer[7]);
	}

	public static byte[] orderNo2Byte(String orderNo)
	{
		byte[] tmp = new byte[8];
		for (int idx = 0; idx < tmp.length; idx++) tmp[idx] = 0x00;

		String replaceOrderNo = orderNo.replaceAll("/ /gi", "");
		int length = replaceOrderNo.length();
		for (int idx = 7; idx >= 0; idx--)
		{
			// Log.d(CustomApplication.TAG, idx + ", " + (length - 2) + " ~ " + length);
			tmp[idx] = (byte)Integer.parseInt(replaceOrderNo.substring(length - 2, length), 16);
			length -= 2;

			log.debug(idx + ", " + (length - 2) + " ~ " + length + " => " + String.format("%02X", tmp[idx]));
		}

		return tmp;
	}

	/**
	 * 충전기 상태
	 *
	 * @param buffer
	 * @return
	 */
	public static String convertChargerState(@NonNull byte[] buffer)
	{
		String tmp = "";
		int intBuffer = hex2Number(buffer);

		if ((intBuffer & 1) == 1) tmp += "커넥터 연결, ";
		if ((intBuffer & 2) == 2) tmp += "충전중, ";
		if ((intBuffer & 4) == 4) tmp += "정의되지 않은 오류, ";
		if ((intBuffer & 8) == 8) tmp += "비상 스위치 눌림, ";
		if ((intBuffer & 16) == 16) tmp += "전원오류, ";
		if ((intBuffer & 32) == 32) tmp += "RCD 차단, ";
		if ((intBuffer & 64) == 64) tmp += "MC 융착, ";
		if ((intBuffer & 128) == 128) tmp += "Pilot 오류, ";
		if ((intBuffer & 256) == 256) tmp += "메모리 오류, ";

		return tmp.length() == 0 ? "N/A" : tmp;
	}

	/**
	 * 충전 상태
	 *
	 * @param buffer
	 * @return
	 */
	public static String convertChargingState(@NonNull byte[] buffer)
	{
		String tmp = "";
		int intBuffer = hex2Number(buffer);

		switch (intBuffer)
		{
			case 0:
				tmp += "대기";
				break;
			case 1:
				tmp += "충전";
				break;
			case 2:
				tmp += "사용자 종료";
				break;
			case 3:
				tmp += "차량 종료";
				break;
			case 4:
				tmp += "충전기 종료";
				break;
			case 5:
				tmp += "충전기와 전기차간 통신 오류 종료";
				break;
			case 6:
				tmp += "저전압 종료";
				break;
			case 7:
				tmp += "과전압 종료";
				break;
			case 8:
				tmp += "과전류 종료";
				break;
			case 9:
				tmp += "MC 융착 종료";
				break;
			case 10:
				tmp += "비상 스위치 종료";
				break;
			case 11:
				tmp += "RCD 차단 종료";
				break;
		}

		return tmp;
	}

	public static String parseUsageKwh(@NonNull byte[] buffer)
    {
        String usageKwh = "";
        try
        {
            byte[][] kwh = new byte[24][];
            int idx = 0;

            for (int payloadIdx = 0; payloadIdx < 48; )
            {
                kwh[idx] = Arrays.copyOfRange(buffer, payloadIdx, payloadIdx + 2);

                if (payloadIdx > 0) usageKwh += ",";
                usageKwh += ChargerCommandUtil.hex2Number(kwh[idx]);

                idx++;
                payloadIdx += 2;
            }
        }
        catch (Exception ex)
        {
            log.error("parsePayload : " + ex.getMessage());
        }

        return usageKwh;
    }

	public static String logByteToString(@NonNull byte[] buffer)
	{
		StringBuilder sb = new StringBuilder();

		for (final byte b : buffer)
		{
			sb.append(String.format("%02X ", b & 0xff));
		}

		return sb.toString();
	}

	public static String logByteToString(byte buffer)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%02X", buffer & 0xff));
		//Log.d(TAG, sb.toString());
		return sb.toString();
	}

	public static String joinByte(byte[] data)
	{
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final byte b : data)
		{
			//if (!first) sb.append(",");
			sb.append(String.format("%d,", b & 0xff));
			first = false;
		}

		return sb.toString();
	}

	public static void printByte(@NonNull byte[] data, String label)
	{
		// StringBuilder sb = new StringBuilder();
		// for (final byte b : data)
		// {
		// 	sb.append(String.format("%d,", b & 0xff));
		// }
        //
		// log.debug(label + " =====> " + sb.toString());

		log.trace(label + " ===> " + byte2HexString(data) + "\n");
	}

	public static void printMsg(String msg)
	{
		// new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date())
		log.debug(" =====> " + msg);
	}

	public static String byte2HexString(@NonNull byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final byte b : data)
        {
            if (first)
            {
                sb.append(String.format("%02X", b & 0xff));
                first = false;
            }
            else
            {
                sb.append(String.format(", %02X", b & 0xff));
            }
        }

        return sb.toString();
    }

    public static String padLeftZeros(String str, int n) {
        return String.format("%1$" + n + "s", str.trim()).replace(' ', '0');
    }

	//byte[] status = Arrays.copyOfRange(packet, 9, 11);
	//byte[] charger1 = Arrays.copyOfRange(packet, 11, 15);
	//byte[] respCode = Arrays.copyOfRange(packet, 15, 16);
	//
	//public static String byteToBCD(byte[] buffer)
	//{
	//    //payload[payloadIdx + 0] = Byte.parseByte(strCurrentDT.substring(0, 2),16);
	//    //payload[payloadIdx + 1] = Byte.parseByte(strCurrentDT.substring(2, 4),16);
	//    //payload[payloadIdx + 2] = Byte.parseByte(strCurrentDT.substring(4, 6),16);
	//    //payload[payloadIdx + 3] = Byte.parseByte(strCurrentDT.substring(6, 8),16);
	//    //payload[payloadIdx + 4] = Byte.parseByte(strCurrentDT.substring(8, 10),16);
	//    //payload[payloadIdx + 5] = Byte.parseByte(strCurrentDT.substring(10, 12),16);
	//
	//    Arrays.copyOfRange(buffer, 0, 2);
	//    switch (buffer.length)
	//    {
	//
	//    }
	//}
	//
	//public static byte[] paddingHEX(byte[] payload)
	//{
	//    // big endian올 표기, 우로 정렬, 빈버퍼는 0x00으로 padding,
	//    byte[] padding = null;
	//    Arrays.fill(padding, (byte)0x00);
	//    //byte[] b = new byte[8];
	//    //BitSet bitSet = BitSet.valueOf(payload);
	//    //bitSet.clear(41, 56); //This will clear 41st to 56th Bit
	//    //b = bitSet.toByteArray();
	//
	//    return padding;
	//}
	//
	//public static byte[] paddingBCD(byte[] payload)
	//{
	//    // big endian올 표기, 우로 정렬, 빈버퍼는 0x00으로 padding,
	//    byte[] padding = null;
	//    Arrays.fill(padding, (byte)0x00);
	//    return padding;
	//}
	//
	//public static byte[] paddingASC(byte[] payload)
	//{
	//    // 좌로 정렬, 빈버퍼는 0x20으로 padding,
	//    byte[] padding = null;
	//    Arrays.fill(padding, (byte)0x20);
	//    return padding;
	//}
}

