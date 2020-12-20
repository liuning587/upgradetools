package com.sanxing.upgrade.protocol.sb;

import com.sanxing.upgrade.protocol.Packet;
import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.util.SysUtils;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SBPacketParser extends PacketParser {
	public static final byte CTRL_LOGIN = 33;
	public static final byte CTRL_HEARTBEAT = 36;
	public static final byte CTRL_READDATA = 1;
	public static final byte CTRL_UPGRADE = 15;
	public static final byte FUNC_RUN_UPGRADE = 0;
	public static final byte FUNC_CANCEL_UPGRADE = 1;
	public static final byte FUNC_UPGRADE_DATA = 2;
	public static final byte FUNC_UPGRADE_STOP = 3;
	public static final byte MSTA_FEP = 30;
	private static byte FSEQ = 0;

	private static byte[] seqLock = new byte[0];

	private byte newSEQ() {
		synchronized (seqLock) {
			if (((FSEQ = (byte) (FSEQ + 1)) & 0xFF) > 127)
				FSEQ = 1;
			return FSEQ;
		}
	}

	public SBPacket packRequest(String terminalAddr, byte msta, byte ctrl, byte[] data, byte dataCs) {
		SBPacket packet = new SBPacket();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);
		packet.setCtrl(ctrl);

		int dataLen = data.length;
		byte[] wData = new byte[13 + dataLen];

		int p = 0;

		wData[p++] = 104;

		wData[p++] = Integer.valueOf(terminalAddr.substring(0, 2), 16).byteValue();
		wData[p++] = Integer.valueOf(terminalAddr.substring(2, 4), 16).byteValue();
		wData[p + 1] = Integer.valueOf(terminalAddr.substring(4, 6), 16).byteValue();
		wData[p] = Integer.valueOf(terminalAddr.substring(6, 8), 16).byteValue();
		p += 2;

		byte seq = newSEQ();

		wData[p++] = (byte) (msta | seq << 6);
		wData[p++] = (byte) (seq >>> 2);

		wData[p++] = 104;

		wData[p++] = ctrl;

		wData[p++] = (byte) dataLen;
		wData[p++] = (byte) (dataLen >> 8);

		System.arraycopy(data, 0, wData, p, data.length);
		p += data.length;

		wData[p++] = (byte) (calcCs(wData, 0, 11) + dataCs);

		wData[p] = 22;

		packet.setData(wData);

		return packet;
	}

	public Packet packLoginRequest(byte msta, String password) {
		byte[] data = SysUtils.hexToBytes(password);
		return packRequest("0000001E", msta, (byte) 33, data, calcCs(data));
	}

	public Packet packHeartbeatRequest(byte msta) {
		byte[] data = new byte[0];
		return packRequest("0000001E", msta, (byte) 36, data, calcCs(data));
	}

	public SBPacket packQueryVersionRequest(String terminalAddr, byte msta) {
		byte[] data = new byte[10];
		int p = 0;

		data[p++] = 1;
		p += 7;

		data[p++] = 9;
		data[p++] = -120;

		return packRequest(terminalAddr, msta, (byte) 1, data, calcCs(data));
	}

	public String getTerminalAddr(byte[] data) {
		int p = 1;

		StringBuffer terminalAddr = new StringBuffer();
		terminalAddr.append(SysUtils.byteToHex(data[p++]));
		terminalAddr.append(SysUtils.byteToHex(data[p++]));
		terminalAddr.append(SysUtils.byteToHex(data[p + 1]));
		terminalAddr.append(SysUtils.byteToHex(data[p++]));
		return terminalAddr.toString();
	}

	public SBPacket packCancelUpgradeRequest(String terminalAddr, String upgradePassword, byte msta) {
		byte[] data = new byte[5];
		int p = 0;

		data[p++] = 41;

		data[p++] = 1;

		byte[] bytes = SysUtils.hexToBytes(SysUtils.reverseHex(upgradePassword));
		System.arraycopy(bytes, 0, data, p, 3);
		p += 3;

		return packRequest(terminalAddr, msta, (byte) 15, data, calcCs(data));
	}

	public Packet unpackReponse(byte[] data) {
		SBPacket packet = null;

		int p = 1;

		p += 4;

		byte msta = (byte) (data[p++] & 0x3F);
		p++;

		p++;

		byte ctrl = (byte) (data[p++] & 0x3F);

		if (33 == ctrl) {
			packet = new LoginRespPacket();
			packet.setType(1);
		} else if (36 == ctrl) {
			packet = new HeartbeatRespPacket();
			packet.setType(2);
		} else if (1 == ctrl) {

			if (31 == data.length) {

				p += 2;

				p += 8;

				if (9 == data[p++] && -120 == data[p++]) {

					packet = new QueryVersionRespPacket();
					packet.setType(1024);
					byte[] bytes = Arrays.copyOfRange(data, p, p + 8);

					SysUtils.reverseBytes(bytes);
					((QueryVersionRespPacket) packet).setVersion(SysUtils.bytesToHex(bytes));
				}
			}
		} else if (15 == ctrl) {

			p += 2;

			p++;

			int funcCode = data[p++];
			if (funcCode == 0) {
				packet = new StartUpgradeRespPacket();
				packet.setType(2048);

				((StartUpgradeRespPacket) packet).setCode(data[p]);
			} else if (1 == funcCode) {
				packet = new CancelUpgradeRespPacket();
				packet.setType(4096);
				((CancelUpgradeRespPacket) packet).setCode(data[p]);
			} else if (2 == funcCode) {
				packet = new CheckFileRespPacket();
				packet.setType(8192);

				p += 2;

				((CheckFileRespPacket) packet).setSeq(data[p++]);

				((CheckFileRespPacket) packet).setPs(Arrays.copyOfRange(data, p, data.length - 2));
			} else if (3 == funcCode) {
				packet = new UpgradeOverPacket();
				packet.setType(16384);
				((UpgradeOverPacket) packet).setCode(data[p]);
			}
		}
		if (packet == null)
			packet = new SBPacket();
		packet.setTerminalAddr(getTerminalAddr(data));
		packet.setMsta(msta);
		packet.setCtrl(ctrl);
		packet.setData(data);

		return packet;
	}

	public boolean filterPacket(Packet packet, List<Packet> list) {
		byte[] data = packet.getData();
		boolean result = false;
		int i = 0;

		while (i < data.length) {

			while (i < data.length && 104 != data[i]) {
				i++;
			}

			if (13 > data.length - i) {
				break;
			}

			if (104 != data[i + 7]) {
				i++;

				continue;
			}

			int len = data[i + 9] & 0xFF | data[i + 10] << 8 & 0xFFFF;

			if (13 + len > data.length - i) {
				i++;

				continue;
			}

			if (22 != data[i + 13 + len - 1]) {
				i++;

				break;
			}

			SBPacket validPacket = new SBPacket();
			validPacket.setData(Arrays.copyOfRange(data, i, i + len + 13));
			list.add(validPacket);

			result = true;

			i = i + len + 13;
		}
		return result;
	}

	public String getUpgradePassword(Date date) {
		byte[] pwd = new byte[3];
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int year = calendar.get(1) - 1900;
		int month = calendar.get(2);
		int yDay = calendar.get(6) - 1;
		int mDay = calendar.get(5);
		int wDay = calendar.get(7) - 1;

		pwd[0] = (byte) ((year >> 1) + (wDay << 2) * yDay + mDay);
		pwd[1] = (byte) ((year << 1) + mDay * (yDay << 2) + wDay);
		pwd[2] = (byte) ((month << 2) + wDay * (mDay << 3) + yDay * 3);

		for (int i = 0; i < 3; i++) {
			if ((pwd[i] & 0xFF) >= 160) {
				pwd[i] = (byte) (pwd[i] - 112);
			}

			if ((pwd[i] & 0xF) >= 10) {
				pwd[i] = (byte) (pwd[i] - 6);
			}
		}

		return SysUtils.bytesToHex(pwd);
	}

	public String getTerminalAddr(Packet packet) {
		byte[] data = packet.getData();

		int p = 1;

		StringBuffer terminalAddr = new StringBuffer();
		terminalAddr.append(SysUtils.byteToHex(data[p++]));
		terminalAddr.append(SysUtils.byteToHex(data[p++]));
		terminalAddr.append(SysUtils.byteToHex(data[p + 1]));
		terminalAddr.append(SysUtils.byteToHex(data[p++]));

		return terminalAddr.toString();
	}

	public boolean isFepResp(Packet packet) {
		String terminalAddr = getTerminalAddr(packet);
		return (terminalAddr.compareTo("0000001E") == 0);
	}

	public boolean isATCTResp(byte[] data) {
		return false;
	}
}
