package com.sanxing.upgrade.business;

import com.sanxing.upgrade.util.Resources;

public class FepConnectController {
	private FepConnector[] connectors;

	public void open() {
		boolean specialChannel = Boolean.valueOf(Resources.getProperty("PROP_SPECIAL_CHANNEL")).booleanValue();
		int maxTaskCount = Integer.valueOf(Resources.getProperty("PROP_MAX_TASK_COUNT")).intValue();

		int connectCount = specialChannel ? maxTaskCount : 1;

		int taskCount = specialChannel ? 1 : maxTaskCount;

		this.connectors = new FepConnector[connectCount];
		for (int i = 0; i < connectCount; i++) {
			this.connectors[i] = new FepConnector(taskCount, i + 1);
			this.connectors[i].open();
		}
	}

	public void close() {
		if (this.connectors == null)
			return;

		for (int i = 0; i < this.connectors.length; i++) {
			this.connectors[i].close();
		}

		this.connectors = null;
	}
}
