package com.sanxing.upgrade.protocol.sb;

import com.sanxing.upgrade.protocol.Packet;

public class SBPacket extends Packet {
	private static final long serialVersionUID = 3461482739742596270L;
	private byte ctrl;

	public void setCtrl(byte ctrl) {
		this.ctrl = ctrl;
	}

	public byte getCtrl() {
		return this.ctrl;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("控制码：");
		buffer.append(this.ctrl);

		buffer.append(",");
		buffer.append(super.toString());

		return buffer.toString();
	}
}
