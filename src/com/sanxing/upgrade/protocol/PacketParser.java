package com.sanxing.upgrade.protocol;

import java.util.List;

public abstract class PacketParser {
	public abstract boolean filterPacket(Packet paramPacket, List<Packet> paramList);

	public abstract boolean isFepResp(Packet paramPacket);

	public abstract String getTerminalAddr(Packet paramPacket);

	public abstract Packet packLoginRequest(byte paramByte, String paramString);

	public abstract Packet packHeartbeatRequest(byte paramByte);

	public abstract Packet unpackReponse(byte[] paramArrayOfbyte);

	public abstract boolean isATCTResp(byte[] paramArrayOfbyte);

	public static byte calcCs(byte[] data) {
		byte result = 0;
		byte b;
		int i;
		byte[] arrayOfByte;
		for (i = (arrayOfByte = data).length, b = 0; b < i;) {
			byte b1 = arrayOfByte[b];
			result = (byte) (result + b1);
			b++;
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
