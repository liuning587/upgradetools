package com.sanxing.upgrade.protocol.gb;

import com.sanxing.upgrade.protocol.Packet;
import com.sanxing.upgrade.util.SysUtils;
import java.util.Arrays;

public class GB09PacketParser extends GBPacketParser {
	public static final byte AFN_CONFIRM = 0;
	public static final byte AFN_LINK = 2;
	public static final byte AFN_QRYCFG = 9;
	public static final byte AFN_TRANSFER = 19;
	public static final byte AFN_ATCT = 51;
	public static final byte FN_VERSION = 1;
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

	private static byte[] seqLock = new byte[0];

	private byte newSEQ() {
		synchronized (seqLock) {
			PFC = (byte) (PFC + 1);
			return PFC;
		}
	}

	public GBPacket packRequest(String terminalAddr, byte msta, byte afn, int fn, byte[] data, byte dataCs) {
		GBPacket packet = new GBPacket();
		packet.setTerminalAddr(terminalAddr);
		packet.setMsta(msta);

		int usrLen = 12 + data.length;
		byte[] wData = new byte[usrLen + 8];

		int p = 0;

		wData[p++] = 0x68;

		wData[p++] = (byte) (usrLen << 2 | 0x2);
		wData[p++] = (byte) (usrLen << 2 >> 8);

		wData[p++] = (byte) (usrLen << 2 | 0x2);
		wData[p++] = (byte) (usrLen << 2 >> 8);

		wData[p++] = 0x68;

		wData[p++] = 64;

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

	public GBPacket packQueryVersionRequest(String terminalAddr, byte msta) {
		return packRequest(terminalAddr, msta, (byte) 9, 1, new byte[0], (byte) 0);
	}

	public Packet unpackReponse(Packet packet) {
		byte[] data = packet.getData();
		byte afn = ((GBPacket) packet).getAfn();

		if (isATCTResp(data)) {
			packet = new ATCTRespPacket();
			String act = String.valueOf(SysUtils.bytesToChr(Arrays.copyOfRange(data, 0, 4)));
			if (act.compareTo("ATCT") == 0) {
				byte state = Integer.valueOf(String.valueOf(SysUtils.bytesToChr(Arrays.copyOfRange(data, 4, 8))), 16)
						.byteValue();
				if (state > 0)
					state = (byte) (state - 1);
				((ATCTRespPacket) packet).setState(state);
			} else if (act.compareTo("EROR") == 0) {
				((ATCTRespPacket) packet).setState((byte) ATCTRespCode.ERROR_UNKNOW.ordinal());
			}
			return packet;
		}

		int p = 11;

		byte msta = (byte) (data[p++] >>> 1);

		p++;

		p++;

		p += 2;

		byte dt1 = data[p++];

		byte dt2 = data[p++];

		int fn = dt2 * 8;
		for (int i = 0; i < 8; i++) {
			if ((dt1 & 1 << i) != 0) {
				fn += i + 1;
				break;
			}
		}
		if (2 == afn) {
			packet = new LinkReqPacket();
			packet.setType(4);
		} else if (afn == 0) {
			if (fn == 1) {
				packet = new ConfirmPacket();
				packet.setType(512);
				((ConfirmPacket) packet).setState((byte) ResponseCode.FINISH.ordinal());
			} else if (fn == 2) {
				packet = new ConfirmPacket();
				packet.setType(512);
				((ConfirmPacket) packet).setState((byte) ResponseCode.FINISH.ordinal());
			} else if (fn == 3) {
				byte safn = data[p++];

				if (safn == 19) {
					packet = new ConfirmPacket();
					packet.setType(512);
					p += 4;

					((ConfirmPacket) packet).setState(data[p]);

				} else if (safn == 2) {
					packet = new ConfirmPacket();
					packet.setType(512);
					p += 4;

					if (data[p] == 0) {
						((ConfirmPacket) packet).setState((byte) ResponseCode.FINISH.ordinal());
					} else {
						((ConfirmPacket) packet).setState(data[p]);
					}

				}
			}
		} else if (9 == afn) {
			if (fn == 1) {
				packet = new QueryVersionRespPacket();
				packet.setType(1024);
				p += 12;
				((QueryVersionRespPacket) packet)
						.setVersion(String.valueOf(SysUtils.bytesToChr(Arrays.copyOfRange(data, p, p + 4))).trim());
			}

		} else if (19 == afn && 3 == fn) {
			packet = new CheckFileRespPacket();
			packet.setType(8192);

			int count = data[p++] & 0xFF | data[p++] << 8 & 0xFF00;
			((CheckFileRespPacket) packet).setCount(count);

			((CheckFileRespPacket) packet).setLastIndex(data[p++] & 0xFF | data[p++] << 8 & 0xFF00);

			if (count == 0) {
				((CheckFileRespPacket) packet).setPs(new byte[0]);
			} else {
				((CheckFileRespPacket) packet).setPs(Arrays.copyOfRange(data, p, p + (count - 1) / 8 + 1));
			}
		}
		if (packet == null)
			packet = new GBPacket();
		packet.setTerminalAddr(getTerminalAddr(data));
		packet.setMsta(msta);
		((GBPacket) packet).setAfn(afn);
		packet.setData(data);

		return packet;
	}
}
