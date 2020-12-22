package com.sanxing.upgrade.protocol.sb;

public class CancelUpgradeRespPacket extends SBPacket {
	private static final long serialVersionUID = 2634814719051346239L;
	private int code;

	public CancelUpgradeRespPacket() {
		setType(CANCEL_UPGRADE_RESP);
	}

	public void setCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}
}
