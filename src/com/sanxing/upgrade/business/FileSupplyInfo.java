package com.sanxing.upgrade.business;

public class FileSupplyInfo {
	private final int sectionCount;
	private int[] lostPs;
	private int currentIndex;

	public FileSupplyInfo(int sectionCount) {
		this.sectionCount = sectionCount;
		clear();
	}

	public int count() {
		return this.lostPs.length;
	}

	public float sendRate() {
		return (this.sectionCount - this.lostPs.length - this.currentIndex) / this.sectionCount;
	}

	public void clear() {
		this.lostPs = null;
		this.currentIndex = -1;
	}

	public boolean hasNextIndex() {
		return (this.lostPs != null && this.currentIndex < this.lostPs.length && this.currentIndex > -1);
	}

	public int getNextIndex() {
		return this.lostPs[this.currentIndex++];
	}

	public void start(byte[] ps) {
		int n = this.sectionCount / 8;
		int m = this.sectionCount % 8;
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < n; i++) {
			if (-1 != ps[i]) {
				if (ps[i] == 0) {
					for (int k = 0; k < 8; k++) {
						buffer.append(i * 8 + k + 1);
						buffer.append(" ");
					}
				} else {
					for (int k = 0; k < 8; k++) {
						if ((ps[i] & 1 << k) == 0) {
							buffer.append(i * 8 + k + 1);
							buffer.append(" ");
						}
					}
				}
			}
		}
		if (m != 0) {
			for (int k = 0; k < m; k++) {
				if ((ps[n] & 1 << k) == 0) {
					buffer.append(n * 8 + k + 1);
					buffer.append(" ");
				}
			}
		}
		String[] strs = buffer.toString().split(" ");

		this.lostPs = new int[strs.length];
		for (int j = 0; j < strs.length; j++)
			this.lostPs[j] = Integer.valueOf(strs[j]).intValue();
		this.currentIndex = 0;
	}
 }
