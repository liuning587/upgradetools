package com.sanxing.upgrade.protocol.dlt698;

public class ConfirmPacket extends DLT698Packet {
	private static final long serialVersionUID = 4440499957710848285L;
	private byte state;

	public ConfirmPacket() {
		setType(CONFIRM_RESP);
	}

	public void setState(byte state) {
		this.state = state;
	}

	public byte getState() {
		return this.state;
	}
}
