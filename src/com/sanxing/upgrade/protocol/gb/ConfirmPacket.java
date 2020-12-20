package com.sanxing.upgrade.protocol.gb;

public class ConfirmPacket extends GBPacket {
	private static final long serialVersionUID = -2595087593613350057L;
	private byte state;

	public ConfirmPacket() {
		setType(512);
	}

	public void setState(byte state) {
		this.state = state;
	}

	public byte getState() {
		return this.state;
	}
}
