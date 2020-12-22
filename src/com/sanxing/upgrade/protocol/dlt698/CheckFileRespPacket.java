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
