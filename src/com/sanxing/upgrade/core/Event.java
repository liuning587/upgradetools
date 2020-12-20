package com.sanxing.upgrade.core;

import java.io.Serializable;
import java.util.Date;

public class Event implements Serializable {
	private static final long serialVersionUID = -597182569681730145L;
	private Date time = new Date();
	private String remark = "";
	private EventType type;
	private Object attachment;
	private boolean pending = false;

	public static Event create(EventType type, String remark) {
		Event result = new Event();
		result.type = type;
		result.remark = remark;
		return result;
	}

	public static Event createPending(EventType type, String remark) {
		Event result = new Event();
		result.type = type;
		result.remark = remark;
		result.pending = true;
		return result;
	}

	public void attach(Object attachment) {
		this.attachment = attachment;
	}

	public Object attachment() {
		return this.attachment;
	}

	public Date time() {
		return this.time;
	}

	public EventType type() {
		return this.type;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getRemark() {
		return this.remark;
	}

	public void done() {
		this.pending = false;
	}

	public boolean isPending() {
		return this.pending;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(" + this.type);
		if (this.attachment != null)
			buffer.append("," + this.attachment);
		if (this.pending)
			buffer.append(",等待处理");
		buffer.append(")");

		return buffer.toString();
	}
 }
