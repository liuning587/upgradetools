package com.sanxing.upgrade.protocol;

import com.sanxing.upgrade.util.SysUtils;
import java.io.Serializable;

public class Packet implements Serializable {
	private static final long serialVersionUID = -762551542760464653L;
	public static final int LOGIN_RESP = 1;
	public static final int HB_RESP = 2;
	public static final int LINK_CHECK_REQ = 4;
	public static final int CONFIRM_RESP = 512; //0x200
	public static final int VERSION_RESP = 1024; //0x400
	public static final int RUN_UPGRADE_RESP = 2048; // 0x800
	public static final int CANCEL_UPGRADE_RESP = 4096; // 0x1000
	public static final int CHECK_RCV_RESP = 8192; // 0x2000
	public static final int STOP_UPGRADE_RESP = 16384; // 0x4000
	public static final int ATCT_RESP = 1048576; // 0x100000
	public static final int UNKNOW_RESP = -2147483648; // 0x80000000
	private int type = Integer.MIN_VALUE;
	private String terminalAddr;

	public void setData(byte[] data) {
		this.data = data;
	}

	private byte msta;
	private byte[] data;

	public byte[] getData() {
		return this.data;
	}

	public int size() {
		return this.data.length;
	}

	public void setTerminalAddr(String terminalAddr) {
		this.terminalAddr = terminalAddr;
	}

	public String getTerminalAddr() {
		return this.terminalAddr;
	}

	public void setMsta(byte msta) {
		this.msta = msta;
	}

	public byte getMsta() {
		return this.msta;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

	public String toString() {
		if (this.data != null && this.data.length > 0) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("size:");
			buffer.append(this.data.length);
			buffer.append(" data:");
			buffer.append(SysUtils.bytesToHex(getData()));
			return buffer.toString();
		}
		return "size:0";
	}
}
