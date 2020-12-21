package com.sanxing.upgrade.protocol;

import java.util.List;

public abstract class PacketParser {
	public abstract boolean filterPacket(Packet paramPacket, List<Packet> paramList);

	public abstract boolean isFepResp(Packet paramPacket);

	public abstract String getTerminalAddr(Packet paramPacket);

	public abstract Packet packLoginRequest(byte paramByte, String paramString);

	public abstract Packet packHeartbeatRequest(byte paramByte);

	public abstract Packet unpackReponse(Packet paramPacket);

	public abstract boolean isATCTResp(byte[] paramArrayOfbyte);

	public static byte calcCs(byte[] data) {
		byte result = 0;
		for (int i = 0; i < data.length; i++) {
			result = (byte) (result + data[i]);
		}
		return result;
	}

	public byte calcCs(byte[] data, int pos, int length) {
		byte result = 0;
		for (int i = pos; i < pos + length; i++)
			result = (byte) (result + data[i]);
		return result;
	}
}
