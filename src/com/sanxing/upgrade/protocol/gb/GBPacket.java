package com.sanxing.upgrade.protocol.gb;

import com.sanxing.upgrade.protocol.Packet;

public class GBPacket extends Packet {
	private static final long serialVersionUID = 3461482739742596270L;
	private byte afn;

	public void setAfn(byte afn) {
		this.afn = afn;
	}

	public byte getAfn() {
		return this.afn;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("功能码：");
		buffer.append(this.afn);

		buffer.append(",");
		buffer.append(super.toString());

		return buffer.toString();
	}
 }
