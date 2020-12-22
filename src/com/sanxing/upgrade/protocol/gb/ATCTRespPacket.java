package com.sanxing.upgrade.protocol.gb;

public class ATCTRespPacket extends GBPacket {
	private static final long serialVersionUID = 1833061709791849955L;
	private byte state;

	public ATCTRespPacket() {
		setType(ATCT_RESP);
	}

	public void setState(byte state) {
		this.state = state;
	}

	public byte getState() {
		return this.state;
	}
}
