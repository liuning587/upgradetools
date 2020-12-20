package com.sanxing.upgrade.protocol.gb;

public class CheckFileRespPacket extends GBPacket {
	private static final long serialVersionUID = 8023477765195486026L;
	private int count;
	private int lastIndex;
	private byte[] ps;

	public CheckFileRespPacket() {
		setType(8192);
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
