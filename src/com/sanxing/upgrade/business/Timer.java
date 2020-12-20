package com.sanxing.upgrade.business;

public class Timer {
	private Object lock;
	private long endMillis = 0L;

	public void reset(Object lock, int waitMsec) {
		this.lock = lock;

		if (waitMsec == 0) {
			this.endMillis = 0L;
		} else {
			this.endMillis = System.currentTimeMillis() + waitMsec;
		}
	}

	public boolean isTimeout() {
		if (0L == this.endMillis) {
			return true;
		}
		return (System.currentTimeMillis() >= this.endMillis);
	}

	public void waitEvent() {
		if (0L == this.endMillis) {
			return;
		}
		long timeout = this.endMillis - System.currentTimeMillis();
		if (timeout > 0L) {
			synchronized (this.lock) {
				try {
					this.lock.wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void clear() {
		this.endMillis = 0L;
	}
}
