package com.sanxing.upgrade.core;

import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable {
	private static final long serialVersionUID = -8240196440472477811L;
	public static final int STATE_RETURN_VERSION = 0x0001;
	public static final int STATE_START_UPGRADE = 0x0002;
	public static final int STATE_CHECK_FILE = 0x0008;
	public static final int STATE_FINISH = 0x0010;//16;
	public static final int STATE_UPGRADING = 0x0400;//1024;
	public static final int STATE_CANCELING = 0x0800;//2048;
	public static final int STATE_WAITING = 0x1000;//4096;
	private String terminalAddr;
	private String oldVersion;
	private String currentVersion;
	private EventList events;
	private int state;
	private float rcvRate;
	private String fileSign;
	private String remark;
	private Date lastTime;

	public Task(String terminalAddr) {
		this.terminalAddr = terminalAddr;
		this.oldVersion = "";
		this.currentVersion = "";
		this.fileSign = "";
		this.remark = "";

		this.events = new EventList();
	}

	public String getTerminalAddr() {
		return this.terminalAddr;
	}

	public int getRate() {
		if ((this.state & STATE_FINISH) != 0) {
			return 100;
		}
		if ((this.state & STATE_CHECK_FILE) != 0) {
			return 98;
		}
		if ((this.state & STATE_START_UPGRADE) != 0) {
			return 2 + Math.round(96.0F * this.rcvRate);
		}

		if ((this.state & STATE_RETURN_VERSION) != 0) {
			return 2;
		}
		return 0;
	}

	public synchronized void setOldVersion(String oldVersion) {
		this.oldVersion = oldVersion;
	}

	public synchronized String getOldVersion() {
		return this.oldVersion;
	}

	public void setCurrentVersion(String currentVersion) {
		this.currentVersion = currentVersion;
	}

	public String getCurrentVersion() {
		return this.currentVersion;
	}

	public EventList getEvents() {
		return this.events;
	}

	public void addEvent(Event event) {
		this.events.add(event);
		this.remark = event.getRemark();
		this.lastTime = event.time();
	}

	public synchronized void addState(int state) {
		this.state |= state;
	}

	public synchronized void removeState(int state) {
		this.state &= state ^ 0xFFFFFFFF;
	}

	public synchronized int getState() {
		return this.state;
	}

	public synchronized void stop() {
		removeState(7168);

		this.events.done();

		this.events.close();
	}

	public synchronized boolean isNew() {
		return (this.state == 0 && this.events.count() == 0);
	}

	public synchronized boolean isFinish() {
		return ((this.state & STATE_FINISH) != 0);
	}

	public synchronized boolean isUpgrading() {
		return ((this.state & STATE_UPGRADING) != 0);
	}

	public synchronized boolean isCanceling() {
		return ((this.state & STATE_CANCELING) != 0);
	}

	public synchronized boolean isWaiting() {
		return ((this.state & STATE_WAITING) != 0);
	}

	public synchronized boolean isBreakPending() {
		return this.events.hasBreakPending();
	}

	public Event firstPendingEvent() {
		return this.events.nextPending();
	}

	public Event lastEvent() {
		return this.events.last();
	}

	public String getStateRemark() {
		if (isFinish())
			return "成功";
		if (isNew())
			return "未开始";
		if (isWaiting())
			return "待处理";
		if (isUpgrading())
			return "升级中";
		if (isCanceling()) {
			return "取消中";
		}
		return "未完成";
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Task(终端地址=");
		buffer.append(this.terminalAddr);
		buffer.append(",状态=");
		buffer.append(getStateRemark());
		buffer.append(")");
		return buffer.toString();
	}

	public void setFileSign(String fileSign) {
		this.fileSign = fileSign;
	}

	public String getFileSign() {
		return this.fileSign;
	}

	public void setRcvRate(float rcvRate) {
		this.rcvRate = rcvRate;
	}

	public float getRcvRate() {
		return this.rcvRate;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getRemark() {
		return this.remark;
	}

	public void setLastTime(Date lastTime) {
		this.lastTime = lastTime;
	}

	public Date getLastTime() {
		return this.lastTime;
	}
}
