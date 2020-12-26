package com.sanxing.upgrade.protocol.dlt698;

import com.sanxing.upgrade.protocol.Packet;
import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.util.Logger;
import com.sanxing.upgrade.util.SysUtils;
import java.util.Arrays;
import java.util.List;

public class DLT698PacketParser extends PacketParser {
	public static final byte AFN_CONFIRM = 0;
	public static final byte AFN_RESET = 1;
	public static final byte AFN_LINK = 2;
	public static final byte AFN_QRYD1 = 12;
	public static final byte AFN_TRANSFER = 19;
	public static final byte FN_VERSION = 1;
	public static final byte FN_DATA_CLEAR = 2;
	public static final byte FN_ALL_OK = 1;
	public static final byte FN_ALL_ERROR = 2;
	public static final byte FN_PART_CONFIRM = 3;
	public static final byte FN_RUN_UPGRADE = 1;
	public static final byte FN_CANCEL_UPGRADE = 2;
	public static final byte FN_UPGRADE_DATA = 3;
	public static final byte FN_UPGRADE_STOP = 4;
	public static final byte FN_LOGIN = 1;
	public static final byte FN_HB = 3;
	private static byte PFC = 0;
	private static int Pmax698PtlFrameLen = 2200;

	private static byte[] seqLock = new byte[0];

	private byte newSEQ() {
		synchronized (seqLock) {
			PFC = (byte) (PFC + 1);
			return PFC;
		}
	}

	public DLT698Packet packRequest(String terminalAddr, byte msta, byte afn, int fn, byte[] data, byte dataCs) {
		DLT698Packet packet = new DLT698Packet();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);

		int usrLen = 12 + data.length;
		byte[] wData = new byte[usrLen + 8];

		int p = 0;

		wData[p++] = 0x68;

		wData[p++] = (byte) (usrLen << 2 | 0x1);
		wData[p++] = (byte) (usrLen << 2 >> 8);

		wData[p++] = (byte) (usrLen << 2 | 0x1);
		wData[p++] = (byte) (usrLen << 2 >> 8);

		wData[p++] = 0x68;

		wData[p++] = 0x40;

		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p++] = (byte) (msta << 1);

		wData[p++] = afn;

		wData[p++] = (byte) (0x60 | newSEQ() & 0xF);

		wData[p++] = 0;
		wData[p++] = 0;

		wData[p++] = (byte) (1 << (fn - 1) % 8);
		wData[p++] = (byte) ((fn - 1) / 8);

		System.arraycopy(data, 0, wData, p, data.length);
		p += data.length;

		wData[p++] = (byte) (calcCs(wData, 6, 12) + dataCs);

		wData[p] = 0x16;

		packet.setData(wData);

		return packet;
	}

	//查询位图: 68 17 00 43 05 17 03 11 11 11 11 39 82 98 05 01 3F F0 01 04 00 00 E9 71 16
	//响应: 68 1E 00 C3 05 17 03 11 11 11 11 39 6E B5 85 01 3F F0 01 04 00 01 04 11 FF FF 80 00 00 B8 85 16 
	public DLT698Packet packCheckFileRequest(String terminalAddr, byte msta) {
		DLT698Packet packet = new DLT698Packet();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);

		byte[] wData = new byte[25];
		int p = 0;
		int crc;

		wData[p++] = 0x68;
		wData[p++] = 0x17;
		wData[p++] = 0x00;
		wData[p++] = 0x43; //ctrl
		wData[p++] = 0x05;
		wData[p++] = Integer.valueOf(terminalAddr.substring(10, 12), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(8, 10), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = (byte)msta;

		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x05;
		wData[p++] = 0x01;
		wData[p++] = (byte)(newSEQ() & 0x1f);
		wData[p++] = (byte)0xF0;
		wData[p++] = 0x01;
		wData[p++] = 0x04;
		wData[p++] = 0x00;
		wData[p++] = 0x00;

		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x16;

		packet.setData(wData);

		return packet;
	}

	//查询命令结果: 68 17 00 43 05 17 03 11 11 11 11 39 82 98 05 01 3F F0 01 03 00 00 EC FD 16
	public DLT698Packet packCheckUpgradeRequest(String terminalAddr, byte msta) {
		DLT698Packet packet = new DLT698Packet();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);
		
		byte[] wData = new byte[25];
		int p = 0;
		int crc;
		
		wData[p++] = 0x68;
		wData[p++] = 0x17;
		wData[p++] = 0x00;
		wData[p++] = 0x43; //ctrl
		wData[p++] = 0x05;
		wData[p++] = Integer.valueOf(terminalAddr.substring(10, 12), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(8, 10), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = (byte)msta;
		
		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x05;
		wData[p++] = 0x01;
		wData[p++] = (byte)(newSEQ() & 0x1f);
		wData[p++] = (byte)0xF0;
		wData[p++] = 0x01;
		wData[p++] = 0x03;
		wData[p++] = 0x00;
		wData[p++] = 0x00;
		
		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x16;
		
		packet.setData(wData);
		
		return packet;
	}

	//启动升级: 68 34 00 43 05 17 03 11 11 11 11 39 BB 84 07 01 01 F0 01 07 00 02 03 02 06 0A 00 0A 00 06 00 
	//00 43 54 04 03 E0 0A 00 16 00 12 04 00 02 02 16 00 09 00 00 1D 59 16
	public DLT698Packet packStartUpgradeRequest(String terminalAddr, byte msta, int fileSize, int blockSize, int fileType) {
		DLT698Packet packet = new DLT698Packet();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);
		
		byte[] wData;
		
		if (blockSize > 16*1024) {
			wData = new byte[54+2];
		} else {
			wData = new byte[54];
		}
		int p = 0;
		int crc;
		
		wData[p++] = 0x68;
		if (blockSize > 16*1024) {
			wData[p++] = 0x36;
		} else {
			wData[p++] = 0x34;
		}
		wData[p++] = 0x00;
		wData[p++] = 0x43; //ctrl
		wData[p++] = 0x05;
		wData[p++] = Integer.valueOf(terminalAddr.substring(10, 12), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(8, 10), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = (byte)msta;
		
		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x07;
		wData[p++] = 0x01;
		wData[p++] = (byte)(newSEQ() & 0x1f);
		wData[p++] = (byte)0xF0;
		wData[p++] = 0x01;
		wData[p++] = 0x07;
		wData[p++] = 0x00;

		wData[p++] = 0x02;
		wData[p++] = 0x03;
		wData[p++] = 0x02;
		wData[p++] = 0x06;
		wData[p++] = 0x0A;
		wData[p++] = 0x00;
		wData[p++] = 0x0A;
		wData[p++] = 0x00;

		wData[p++] = 0x06; //文件大小
		wData[p++] = (byte)(fileSize >> 24);
		wData[p++] = (byte)(fileSize >> 16);
		wData[p++] = (byte)(fileSize >> 8);
		wData[p++] = (byte)(fileSize >> 0);

		wData[p++] = 0x04;
		wData[p++] = 0x03;
		wData[p++] = (byte)0xE0; //文件属性
		wData[p++] = 0x0A;
		wData[p++] = 0x00; //文件版本
		wData[p++] = 0x16;
		wData[p++] = (byte)fileType; //终端文件

		if (blockSize > 16*1024) {
			wData[p++] = 0x06; //传输块大小
			wData[p++] = (byte)(blockSize >> 24);
			wData[p++] = (byte)(blockSize >> 16);
			wData[p++] = (byte)(blockSize >> 8);
			wData[p++] = (byte)(blockSize >> 0);
		} else {
			wData[p++] = 0x12; //传输块大小
			wData[p++] = (byte)(blockSize >> 8);
			wData[p++] = (byte)(blockSize >> 0);
		}

		wData[p++] = 0x02; //校验2个成员
		wData[p++] = 0x02;
		wData[p++] = 0x16; 
		wData[p++] = 0x00; //CRC校验
		wData[p++] = 0x09; //校验值
		wData[p++] = 0x00;

		wData[p++] = 0x00;
		
		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x16;
		
		packet.setData(wData);
		
		return packet;
	}

	//传输数据
	public DLT698Packet packUpgradeDataRequest(String terminalAddr, byte msta, int index, byte[] section, boolean allowQuery) {
		DLT698Packet packet = new DLT698Packet();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);
		
		byte[] wData;
		int wLen = 34 + section.length;
		if (allowQuery) {
			wLen += 25;
		}
		if (section.length > 15 * 1024) {
			wLen += 1;
		}
		wData = new byte[wLen]; 
		
		int p = 0;
		int crc;
		int lenArea = wData.length - 2;
		if (allowQuery) {
			lenArea -= 25;
		}
		wData[p++] = 0x68;
		if (lenArea <= 0x3FFF) {
			wData[p++] = (byte)(lenArea >> 0);
			wData[p++] = (byte)(lenArea >> 8);
		} else {
			lenArea /= 1024;
			wData[p++] = (byte)(lenArea >> 0);
			wData[p++] = (byte) ((byte)(lenArea >> 8) | (byte)0x40);
		}
		wData[p++] = 0x43; //ctrl
		wData[p++] = 0x05;
		wData[p++] = Integer.valueOf(terminalAddr.substring(10, 12), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(8, 10), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = (byte)msta;
		
		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x07;
		wData[p++] = 0x01;
		wData[p++] = (byte)(newSEQ() & 0x1f);
		wData[p++] = (byte)0xF0;
		wData[p++] = 0x01;
		wData[p++] = 0x08;
		wData[p++] = 0x00;

		wData[p++] = 0x02;
		wData[p++] = 0x02;
		wData[p++] = 0x12; //块序号
		wData[p++] = (byte)(index >> 8);
		wData[p++] = (byte)(index >> 0);

		wData[p++] = 0x09;

		if (section.length > 15 * 1024) {
			wData[p++] = (byte)0x83; 
			wData[p++] = (byte)(section.length >> 16);
			wData[p++] = (byte)(section.length >> 8);
			wData[p++] = (byte)(section.length >> 0);
		} else {
			wData[p++] = (byte)0x82; 
			wData[p++] = (byte)(section.length >> 8);
			wData[p++] = (byte)(section.length >> 0);
		}

		System.arraycopy(section, 0, wData, p, section.length);
		p += section.length;

		wData[p++] = 0x00;
		
		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x16;
		
		if (allowQuery) {
			//添加查询位图
			int start = p;
			wData[p++] = 0x68;
			wData[p++] = 0x17;
			wData[p++] = 0x00;
			wData[p++] = 0x43; //ctrl
			wData[p++] = 0x05;
			wData[p++] = Integer.valueOf(terminalAddr.substring(10, 12), 16).byteValue();
			wData[p++] = Integer.valueOf(terminalAddr.substring(8, 10), 16).byteValue();
			wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
			wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
			wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
			wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
			wData[p++] = (byte)msta;

			crc = PacketParser.calcCrc16(wData, start + 1, p - start - 1);
			wData[p++] = (byte) ((crc >> 0) & 0xff);
			wData[p++] = (byte) ((crc >> 8) & 0xff);
			
			wData[p++] = 0x05;
			wData[p++] = 0x01;
			wData[p++] = (byte)(newSEQ() & 0x1f);
			wData[p++] = (byte)0xF0;
			wData[p++] = 0x01;
			wData[p++] = 0x04;
			wData[p++] = 0x00;
			wData[p++] = 0x00;

			crc = PacketParser.calcCrc16(wData, start + 1, p - start - 1);
			wData[p++] = (byte) ((crc >> 0) & 0xff);
			wData[p++] = (byte) ((crc >> 8) & 0xff);
			
			wData[p++] = 0x16;
		}

//		Logger.printHexString(wData);
		packet.setData(wData);
		
		return packet;
	}
	
	//查询版本: 68 17 00 43 05 29 31 23 11 11 11 39 36 C5 05 01 3F 43 00 03 00 00 1F A2 16
	//响应: 68 47 00 C3 05 29 31 23 11 11 11 39 A1 BD 85 01 3F 43 00 03 00 01 02 06 0A 04 53 58 44 51 0A 04 56 31 2E 30 0A 
	//     06 31 39 30 33 30 36 0A 04 33 36 30 35 0A 06 31 38 30 38 30 33 0A 08 30 30 30 30 30 30 30 30 00 00 F8 4F 16
	//编译时间: 68 17 00 43 05 29 31 23 11 11 11 39 36 C5 05 01 3F FF 03 02 00 00 BA F4 16
	//响应: 68 21 00 C3 05 29 31 23 11 11 11 39 59 67 85 01 3F FF 03 02 00 01 1C 07 E4 0C 0B 0E 08 07 00 00 8E 3A 16
	public DLT698Packet packQueryVersionRequest(String terminalAddr, byte msta) {
		DLT698Packet packet = new DLT698Packet();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);

		byte[] wData = new byte[25];
		int p = 0;
		int crc;

		wData[p++] = 0x68;
		wData[p++] = 0x17;
		wData[p++] = 0x00;
		wData[p++] = 0x43; //ctrl
		wData[p++] = 0x05;
		wData[p++] = Integer.valueOf(terminalAddr.substring(10, 12), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(8, 10), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = (byte)msta;

		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x05;
		wData[p++] = 0x01;
		wData[p++] = (byte) (newSEQ() & 0x1f);
		wData[p++] = 0x43;
		wData[p++] = 0x00;
		wData[p++] = 0x03;
		wData[p++] = 0x00;
		wData[p++] = 0x00;

		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);

		wData[p++] = 0x16;

		packet.setData(wData);

		return packet;
	}

	//取消升级
	//68 1B 00 43 05 17 03 11 11 11 11 39 7D 7A 07 01 01 F0 01 01 00 01 01 0F 00 00 0F 3B 16
	public DLT698Packet packCancelUpgradeRequest(String terminalAddr, String upgradePassword, byte msta) {
		DLT698Packet packet = new DLT698Packet();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);

		byte[] wData = new byte[29];
		int p = 0;
		int crc;

		wData[p++] = 0x68;
		wData[p++] = 0x1B;
		wData[p++] = 0x00;
		wData[p++] = 0x43; //ctrl
		wData[p++] = 0x05;
		wData[p++] = Integer.valueOf(terminalAddr.substring(10, 12), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(8, 10), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = (byte)msta;

		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x07;
		wData[p++] = 0x01;
		wData[p++] = (byte) (newSEQ() & 0x1f);
		wData[p++] = (byte)0xF0;
		wData[p++] = 0x01;
		wData[p++] = 0x01;
		wData[p++] = 0x00;

		wData[p++] = 0x01; //参数
		wData[p++] = 0x01;
		wData[p++] = 0x0F;
		wData[p++] = 0x00;

		wData[p++] = 0x00;

		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x16;

		packet.setData(wData);

		return packet;
	}

	//请求复位
	//68 18 00 43 05 17 03 11 11 11 11 39 CE 84 07 01 00 43 00 01 00 00 00 9E 75 16
	//68 1A 00 C3 05 17 03 11 11 11 11 39 3B EB 87 01 00 43 00 01 00 00 00 00 00 3B 3A 16
	public DLT698Packet packResetSoftRequest(String terminalAddr, String terminalPassword, byte msta) {
		DLT698Packet packet = new DLT698Packet();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);

		byte[] wData = new byte[26];
		int p = 0;
		int crc;

		wData[p++] = 0x68;
		wData[p++] = 0x18;
		wData[p++] = 0x00;
		wData[p++] = 0x43; //ctrl
		wData[p++] = 0x05;
		wData[p++] = Integer.valueOf(terminalAddr.substring(10, 12), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(8, 10), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = (byte)msta;

		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);

		wData[p++] = 0x07;
		wData[p++] = 0x01;
		wData[p++] = (byte) (newSEQ() & 0x1f);
		wData[p++] = 0x43;
		wData[p++] = 0x00;
		wData[p++] = 0x01;
		wData[p++] = 0x00;
		wData[p++] = 0x00;
		wData[p++] = 0x00;

		crc = PacketParser.calcCrc16(wData, 1, p - 1);
		wData[p++] = (byte) ((crc >> 0) & 0xff);
		wData[p++] = (byte) ((crc >> 8) & 0xff);
		
		wData[p++] = 0x16;

		packet.setData(wData);

		return packet;
	}

	public int isVaild(byte[] buf) {
		int fLen;
		int hlen;
		int cs; 
	
		if (buf.length < 1) {
			return 0;
		}
	
		if (0x68 != buf[0]) {
			return -1;
		}

		if (buf.length < 3) {
			return 0;
		}
	
		fLen = ((int)(buf[2]&0x3f) << 8) + (buf[1]&0x0FF);
		if ((buf[2]&0x40) != 0) {
			fLen *= 1024;
		}
		if ((fLen > Pmax698PtlFrameLen-2) || (fLen < 7)) {
			return -2;
		}

		if (buf.length < 6) {
			return 0;
		}
	
		hlen = 6 + (int)(buf[4]&0x0f) + 1;

		if (buf.length < (hlen + 2)) {
			return 0;
		}
	
		//check hcs
		cs = PacketParser.calcCrc16(buf, 1, hlen-1);
		if (cs != (((buf[hlen+1]&0x0FF) << 8) + (buf[hlen]&0x0FF))) {
			return -3;
		}
	
		if (buf.length < (fLen+1)) {
			return 0;
		}
	
		//check fcs
		cs = PacketParser.calcCrc16(buf, 1, fLen-2);
		if (cs != (((buf[fLen]&0x0FF) << 8) + (buf[fLen-1]&0x0FF))) {
			return -4;
		}
	
		if (buf.length < (fLen+2)) {
			return 0;
		}
		if (0x16 != buf[fLen+1]) {
			return -5;
		}
	
		return fLen + 2;
	}
	
	//寻找合法报文
	public boolean filterPacket(Packet packet, List<Packet> list) {
		byte[] data = packet.getData();
		boolean result = false;
		int i = 0;
		int len;

		while (i < data.length) {
			while (i < data.length && 0x68 != data[i]) {
				i++;
			}

			if (20 > data.length - i) {
				break;
			}

			len = isVaild(Arrays.copyOfRange(data, i, data.length));
			if (len <= 0) {
				i++;
				continue;
			}
			
			if ((data[i+3] & 0x80) == 0x80) { //DIR1: 终端-->主站
				DLT698Packet validPacket = new DLT698Packet();
				//validPacket.setAfn(data[i+12]); //todo: set afn
				validPacket.setData(Arrays.copyOfRange(data, i, i + len));
				list.add(validPacket);
				result = true;
			}
			i += len;
		}
		return result;
	}

	public String getTerminalAddr(byte[] data) {
		StringBuffer terminalAddr = new StringBuffer();
		terminalAddr.append(SysUtils.byteToHex(data[10]));
		terminalAddr.append(SysUtils.byteToHex(data[9]));
		terminalAddr.append(SysUtils.byteToHex(data[8]));
		terminalAddr.append(SysUtils.byteToHex(data[7]));
		terminalAddr.append(SysUtils.byteToHex(data[6]));
		terminalAddr.append(SysUtils.byteToHex(data[5]));

		return terminalAddr.toString();
	}

	//从报文中提取终端地址字符串
	public String getTerminalAddr(Packet packet) {
		return getTerminalAddr(packet.getData());
	}

	public boolean isIgnore(Packet packet) {
		byte[] data = packet.getData();
		//681A00C30517031111111121F277 87010F F0010800 000000000FBA16
		int p = 14; //apdu offset
		if (data[p] == (byte)0x87 && data[p+1] == 0x01) {
			p += 3;
			if (data[p] == (byte)0xF0 && data[p+1] == 0x01 && data[p+2] == 0x08 && data[p+3] == 0x00 && data[p+4] == 0x00) { //文件分块传输管理写文件确认
				return true;
			}
		}
		return false;
	}

	public boolean isFepResp(Packet packet) {
		return (getTerminalAddr(packet).compareTo("000000000000") == 0);
	}

	public Packet packHeartbeatRequest(byte msta) {
		return packRequest("000000000000", msta, (byte) 2, 3, new byte[0], (byte) 0);
	}

	public Packet packLoginRequest(byte msta, String password) {
		return packRequest("000000000000", msta, (byte) 2, 1, new byte[0], (byte) 0);
	}

	//解析响应帧
	public Packet unpackReponse(Packet packet) {
		byte[] data = packet.getData();
		byte msta = data[11]; //CA

		int p = 14; //apdu offset
		
//		Logger.printHexString(data);
		
		if (data[p] == (byte)0x85 && data[p+1] == 0x01) {
			p += 3;
			if (data[p] == 0x43 && data[p+1] == 0x00 && data[p+2] == 0x03 && data[p+3] == 0x00 && data[p+4] == 0x01) { //电气设备-版本信息
				//System.out.printf("recv 电气设备-版本信息");
				//01 结果:数据
				//02 06 版本信息:6个成员
				//0A 04 53 58 44 51 厂商代码:SXDQ
				//0A 04 56 31 2E 30 软件版本号:V1.0
				//0A 06 31 39 30 33 30 36 软件版本日期:190306
				//0A 04 33 36 30 35 硬件版本号:3605
				//0A 06 31 38 30 38 30 33 硬件版本日期:180803
				//0A 08 30 30 30 30 30 30 30 30 厂家扩展信息:00000000
				packet = new QueryVersionRespPacket();
				packet.setType(Packet.VERSION_RESP);
				p += 4+11;
				((QueryVersionRespPacket) packet).setVersion(String.valueOf(SysUtils.bytesToChr(Arrays.copyOfRange(data, p, p + 4))).trim());
			} else if (data[p] == (byte)0xF0 && data[p+1] == 0x01 && data[p+2] == 0x04 && data[p+3] == 0x00 && data[p+4] == 0x01 && data[p+5] == 0x04) { //文件分块传输管理-传输块状态字
				//01 结果:数据
				//04 11 FF FF 80 传输块状态字:111111111111111110000000
				p += 6;
				packet = new CheckFileRespPacket();
				packet.setType(Packet.CHECK_RCV_RESP);
		
				int count = (int)(data[p++]&0x0ff);
				if ((count & 0x80) == 0x80) {
			        switch (count & 0x7f) {
			            case 1:
			            	count = (data[p++]&0x0FF);
			                break;
			            case 2:
			            	count = ((data[p]&0x0FF) << 8) | (data[p + 1]&0x0FF);
			                p += 2;
			                break;
			            case 3:
			            	count = ((data[p]&0x0FF) << 16) | ((data[p + 1]&0x0FF) << 8) | (data[p + 2]&0x0FF);
			                p += 3;
			                break;
			            case 4:
			            	count = ((data[p]&0x0FF) << 24) | ((data[p + 1]&0x0FF) << 16) | ((data[p + 2]&0x0FF) << 8) | (data[p + 3]&0x0FF);
			                p += 4;
			                break;
			            default:
			            	break;
			        }
				}

				((CheckFileRespPacket) packet).setCount(count);
				((CheckFileRespPacket) packet).setLastIndex(0); //fixme: 需要检查

				if (count == 0) {
					((CheckFileRespPacket) packet).setPs(new byte[0]);
				} else {
					((CheckFileRespPacket) packet).setPs(Arrays.copyOfRange(data, p, p + (count + 7) / 8));
				}
			}
		} else if (data[p] == (byte)0x87 && data[p+1] == 0x01) {
			p += 3;
			if (data[p] == 0x43 && data[p+1] == 0x00 && data[p+2] == 0x01 && data[p+3] == 0x00) { //复位确认
				//87 01 00 43 00 01 00 00 00 00
				packet = new ConfirmPacket();
				packet.setType(Packet.CONFIRM_RESP);
				if (data[p+4] != 0x00) {
					((ConfirmPacket) packet).setState((byte) ResponseCode.ERROR_UNKNOW.ordinal());
				} else {
					((ConfirmPacket) packet).setState((byte) ResponseCode.FINISH.ordinal());
				}
			} else if (data[p] == (byte)0xF0 && data[p+1] == 0x01 && data[p+2] == 0x01 && data[p+3] == 0x00 && data[p+4] == 0x00) { //文件分块传输管理复位确认
				//87 01 00 F0 01 01 00 00 00 00
				packet = new ConfirmPacket();
				packet.setType(Packet.CONFIRM_RESP);
				((ConfirmPacket) packet).setState((byte) ResponseCode.FINISH.ordinal());
			} else if (data[p] == (byte)0xF0 && data[p+1] == 0x01 && data[p+2] == 0x07 && data[p+3] == 0x00) { //文件分块传输管理启动传输确认
				//87 01 00 F0 01 07 00 00 00 00
				packet = new ConfirmPacket();
				packet.setType(Packet.CONFIRM_RESP);
				if (data[p+4] != 0x00) {
					((ConfirmPacket) packet).setState((byte) ResponseCode.ERROR_UNKNOW.ordinal());
				} else {
					((ConfirmPacket) packet).setState((byte) ResponseCode.FINISH.ordinal());
				}
			} else if (data[p] == (byte)0xF0 && data[p+1] == 0x01 && data[p+2] == 0x08 && data[p+3] == 0x00) { //文件分块传输管理写文件确认
				//87 01 00 F0 01 08 00 00 00 00
				//data[p+4] == 0x00
				packet = new DLT698Packet();
				//fixme: 先不不理会packet.setType(Packet.CONFIRM_RESP);
				if (data[p+4] != 0x00) {
					packet.setType(Packet.CONFIRM_RESP);
					((ConfirmPacket) packet).setState((byte) ResponseCode.ERROR_UNKNOW.ordinal());
				}
			}
		} else {
			packet = new DLT698Packet();
		}

		packet.setTerminalAddr(getTerminalAddr(data));
		packet.setMsta(msta);
		packet.setData(data);

		return packet;
	}

	@Override
	public boolean isATCTResp(byte[] paramArrayOfbyte) {
		// TODO Auto-generated method stub
		return false;
	}
}
