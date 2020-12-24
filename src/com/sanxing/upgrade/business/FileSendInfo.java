package com.sanxing.upgrade.business;

public class FileSendInfo {
	private final int sectionCount;
	private int currentIndex;

	public FileSendInfo(int sectionCount) {
		this.sectionCount = sectionCount;
		clear();
	}

	public int count() {
		return this.sectionCount;
	}

	public float sendRate() {
		if (this.sectionCount == 0)
			return (float)0.0;
		return (float)(this.currentIndex - 1) / this.sectionCount;
	}

	public void clear() {
		this.currentIndex = 0;
	}

	public void start() {
		this.currentIndex = 1;
	}

	public boolean hasNextIndex() {
		return (this.currentIndex <= this.sectionCount && this.currentIndex > 0);
	}

	public int getNextIndex() {
		return this.currentIndex++;
	}
}
