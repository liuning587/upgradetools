package com.sanxing.upgrade.protocol.sb;

public class UpgradeOverPacket extends SBPacket {
	private static final long serialVersionUID = 8984341688734654271L;
	private int code;

	public UpgradeOverPacket() {
		setType(STOP_UPGRADE_RESP);
	}

	public void setCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}
}
