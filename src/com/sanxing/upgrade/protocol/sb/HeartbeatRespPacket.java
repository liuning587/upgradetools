package com.sanxing.upgrade.protocol.sb;

public class HeartbeatRespPacket extends SBPacket {
	private static final long serialVersionUID = 8828700439516516666L;

	public HeartbeatRespPacket() {
		setType(HB_RESP);
	}
}
