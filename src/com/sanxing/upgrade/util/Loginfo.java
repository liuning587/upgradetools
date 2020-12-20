package com.sanxing.upgrade.util;

import java.util.Date;

class Loginfo {
	private LogType type;
	private String message;
	private Date time;

	public Loginfo(LogType type, String message) {
		this.type = type;
		this.message = message;
		this.time = new Date();
	}

	public LogType getType() {
		return this.type;
	}

	public String getMessage() {
		return this.message;
	}

	public Date getTime() {
		return this.time;
	}
}
