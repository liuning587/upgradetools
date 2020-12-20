package com.sanxing.upgrade.business;

import com.sanxing.upgrade.util.SysUtils;
import java.util.Arrays;

public class FileCheckInfo {
	private final int MAX_CHECK_TIMES = 10;

	private final int RECEIVE_IS_STOP = 2;

	private final int sectionCount;

	private final byte[] fullPs;

	private byte[] lastPs;

	private byte querySeq;

	private int steadyTime;

	public FileCheckInfo(int sectionCount) {
		this.sectionCount = sectionCount;

		int n = sectionCount / 8;
		int m = sectionCount % 8;
		String str = SysUtils.formatString("", 'F', n * 2);
		if (m != 0)
			str = String.valueOf(str) + SysUtils.binToHex(SysUtils.formatString("", '1', m));
		this.fullPs = SysUtils.hexToBytes(str);

		clear();
	}

	public void clear() {
		this.lastPs = null;
		this.querySeq = -1;
		this.steadyTime = 0;
	}

	public void start() {
		this.querySeq = 0;
	}

	public boolean isCompletion() {
		return Arrays.equals(this.fullPs, this.lastPs);
	}

	public void check(byte[] ps) {
		if (Arrays.equals(this.lastPs, ps)) {
			this.steadyTime++;
		} else {
			this.lastPs = ps;
			this.steadyTime = 1;
		}
	}

	public byte[] getLastPs() {
		return this.lastPs;
	}

	public boolean isStopReceive() {
		return (this.steadyTime >= 2);
	}

	public boolean isOverTiems() {
		return !(this.querySeq <= 9 && this.querySeq >= 0);
	}

	public byte nextQuerySeq() {
		return this.querySeq = (byte) (this.querySeq + 1);
	}

	public byte currentQuerySeq() {
		return this.querySeq;
	}

	public boolean isError() {
		if (this.lastPs.length != (this.sectionCount - 1) / 8 + 1) {
			return true;
		}

		if (!SysUtils.isZero(this.lastPs, 0, this.lastPs.length - 1)) {
			return false;
		}
		int m = (this.sectionCount - 1) % 8;
		byte b = (byte) (this.lastPs[this.lastPs.length - 1] & 1 << m - 1);

		return (b == 0);
	}

	public boolean isNull() {
		return !(this.lastPs != null && this.lastPs.length != 0);
	}
}
