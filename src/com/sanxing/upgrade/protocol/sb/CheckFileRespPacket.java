package com.sanxing.upgrade.protocol.sb;

public class CheckFileRespPacket extends SBPacket {
	private static final long serialVersionUID = 8023477765195486026L;
	private int count;
	private int seq;
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

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public int getSeq() {
		return this.seq;
	}

	public void setPs(byte[] ps) {
		this.ps = ps;
	}

	public byte[] getPs() {
		return this.ps;
	}
}
