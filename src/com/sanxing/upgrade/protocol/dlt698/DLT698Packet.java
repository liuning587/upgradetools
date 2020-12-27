package com.sanxing.upgrade.protocol.dlt698;

import com.sanxing.upgrade.protocol.Packet;
//import com.sanxing.upgrade.util.SysUtils;

public class DLT698Packet extends Packet {
	private static final long serialVersionUID = 3468943741977905583L;
	private byte afn;

	public void setAfn(byte afn) {
		this.afn = afn;
	}

	public byte getAfn() {
		return this.afn;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
//		buffer.append("afn:");
//		buffer.append(SysUtils.byteToHex(this.afn));
//		buffer.append(" ");
		buffer.append(super.toString());

		return buffer.toString();
	}
}
