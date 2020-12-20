package com.sanxing.upgrade.protocol.sb;

public class StartUpgradeRespPacket extends SBPacket {
	private static final long serialVersionUID = 7802201522994949612L;
	private int code;

	public StartUpgradeRespPacket() {
		setType(2048);
	}

	public void setCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}
}
