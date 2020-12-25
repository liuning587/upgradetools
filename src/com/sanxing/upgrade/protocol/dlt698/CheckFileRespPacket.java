package com.sanxing.upgrade.protocol.dlt698;

public class CheckFileRespPacket extends DLT698Packet {
	private static final long serialVersionUID = 5109635052803852169L;
	private int count;
	private int lastIndex;
	private byte[] ps;

	public CheckFileRespPacket() {
		setType(CHECK_RCV_RESP);
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return this.count;
	}

	public void setPs(byte[] ps) {
		int sta[] = {
	        0x00, 0x08, 0x04, 0x0c, 0x02, 0x0a, 0x06, 0x0e,
	        0x01, 0x09, 0x05, 0x0d, 0x03, 0x0b, 0x07, 0x0f
		};
		for (int i = 0; i < ps.length; i++) {
			ps[i] = (byte)(((sta[ps[i] & 0x0f]) << 4) | (sta[(ps[i] & 0xf0) >> 4]));
		}
		this.ps = ps;
	}

	public byte[] getPs() {
		return this.ps;
	}

	public void setLastIndex(int lastIndex) {
		this.lastIndex = lastIndex;
	}

	public int getLastIndex() {
		return this.lastIndex;
	}
}
