package com.sanxing.upgrade.business;

import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventDAO;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.ProtocolType;
import com.sanxing.upgrade.core.Queue;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.core.TaskDAO;
import com.sanxing.upgrade.core.UpgradeFile;
import com.sanxing.upgrade.util.Resources;
import java.sql.SQLException;

public abstract class UpgradeThread extends Thread {
	UpgradeService service = UpgradeService.getInstance();
	Queue<Task> waitingTasks = this.service.getWaitingTasks();
	Queue<Task> faultTasks = this.service.getFaultTasks();

	FepConnector fepConnector;

	UpgradeFile file;

	Timer timer;

	FileCheckInfo fileCheckInfo;
	FileSendInfo fileSendInfo;
	FileSupplyInfo fileSupplyInfo;
	ProtocolType protocolType;
	boolean restartFaultTask;
	byte msta;
	boolean allowLoginFeps;
	String password;
	int heartbeatInterval;
	boolean specialChannel;
	int sendInterval;
	boolean autoCancel;
	boolean allowQueryVersion;
	boolean skipNeedlessUpgrade;
	boolean skipLaterVersion;
	boolean affirmVersion;
	boolean dynamicPWD;
	String upgradePassword;
	boolean restartTerminal;
	int restartDelay;
	Task currentTask;
	byte[] taskLock = new byte[0];

	volatile boolean isClosed;

	public UpgradeThread(FepConnector owner) {
		this.fepConnector = owner;

		this.file = this.service.getUpgradeFile();
		this.timer = new Timer();
		this.fileCheckInfo = new FileCheckInfo((this.file.getSections()).length);
		this.fileSendInfo = new FileSendInfo((this.file.getSections()).length);
		this.fileSupplyInfo = new FileSupplyInfo((this.file.getSections()).length);

		this.protocolType = ProtocolType.values()[Integer.valueOf(Resources.getProperty("PROP_PROTOCOL_TYPE"))
				.intValue()];

		this.restartFaultTask = Boolean.valueOf(Resources.getProperty("PROP_RESTART_FAULT_TASK")).booleanValue();

		this.msta = Integer.valueOf(Resources.getProperty("PROP_MSTA")).byteValue();

		boolean allowLoginFeps = Boolean.valueOf(Resources.getProperty("PROP_ALLOW_LOGIN_FEPS")).booleanValue();

		if (allowLoginFeps) {

			if (ProtocolType.SB == this.protocolType) {
				this.password = Resources.getProperty("PROP_PASSWORD");
			}

			this.heartbeatInterval = Integer.valueOf(Resources.getProperty("PROP_HEARTBEAT_INTERVAL")).intValue();
		}

		this.specialChannel = Boolean.valueOf(Resources.getProperty("PROP_SPECIAL_CHANNEL")).booleanValue();

		this.sendInterval = Integer.valueOf(Resources.getProperty("PROP_SEND_INTERVAL")).intValue();

		this.autoCancel = Boolean.valueOf(Resources.getProperty("PROP_AUTO_CANCEL")).booleanValue();

		this.allowQueryVersion = Boolean.valueOf(Resources.getProperty("PROP_ALLOW_QUERY_VERSION")).booleanValue();

		this.skipNeedlessUpgrade = Boolean.valueOf(Resources.getProperty("PROP_SKIP_NEEDLESS_UPGRADE")).booleanValue();

		this.skipLaterVersion = Boolean.valueOf(Resources.getProperty("PROP_SKIP_LATER_VERSION")).booleanValue();

		this.affirmVersion = Boolean.valueOf(Resources.getProperty("PROP_AFFIRM_VERSION")).booleanValue();

		this.restartFaultTask = Boolean.valueOf(Resources.getProperty("PROP_RESTART_FAULT_TASK")).booleanValue();
		if (ProtocolType.SB == this.protocolType) {
			if (!Boolean.valueOf(Resources.getProperty("PROP_DYNAMIC_PWD")).booleanValue()) {
				this.upgradePassword = Resources.getProperty("PROP_UPGRADE_PASSWORD");
			}
		}

		this.restartTerminal = Boolean.valueOf(Resources.getProperty("PROP_RESTART_TERMINAL")).booleanValue();

		if (this.restartTerminal) {
			this.restartDelay = Integer.valueOf(Resources.getProperty("PROP_RESTART_TERMINAL_DELAY")).intValue();
		}
	}

	public void close() {
		this.isClosed = true;
		synchronized (this.taskLock) {
			if (this.currentTask != null) {
				this.currentTask.addEvent(Event.createPending(EventType.CUSTOMER_BREAK, "用户中断"));
			}
		}
	}

	public void run() {
		this.isClosed = false;
		while (!this.isClosed) {

			synchronized (this.taskLock) {

				this.currentTask = (Task) this.waitingTasks.take();

				if (this.currentTask == null) {
					if (this.restartFaultTask) {
						this.currentTask = (Task) this.faultTasks.take();
					}
				}
			}

			if (this.currentTask == null) {
				synchronized (this.waitingTasks) {
					try {
						this.waitingTasks.wait();
					} catch (InterruptedException interruptedException) {
					}
				}

				continue;
			}

			startTask();

			Event event = null;
			while (this.currentTask.isUpgrading() || this.currentTask.isCanceling()) {

				event = this.currentTask.getEvents().nextPending();

				if (event == null) {

					if (!this.timer.isTimeout()) {
						this.timer.waitEvent();
					}
					if (this.timer.isTimeout())
						handleTimeout();
					continue;
				}
				handleEvent(event);

				this.service.taskChanged(this.currentTask);
			}

			try {
				TaskDAO.update(this.currentTask);

				EventDAO.insert(this.currentTask.getTerminalAddr(), this.currentTask.getEvents().toArray());
				this.currentTask.getEvents().clear();
			} catch (SQLException e) {
				System.out.println(e);
			}

			if (!this.currentTask.isFinish() && this.restartFaultTask) {
				if (event == null || EventType.CUSTOMER_BREAK != event.type()) {

					this.currentTask.addState(Task.STATE_WAITING | Task.STATE_UPGRADING);

					this.faultTasks.put(this.currentTask);
				}
			}

			synchronized (this.taskLock) {
				this.currentTask = null;
			}
		}
	}

	void startTask() {
		this.timer.clear();

		this.fileSendInfo.clear();

		this.fileCheckInfo.clear();

		this.fileSupplyInfo.clear();

		this.currentTask.removeState(4096);

		this.currentTask.addEvent(Event.createPending(EventType.START_TASK, "开始处理"));

		this.service.taskChanged(this.currentTask);
	}

	boolean checkVersion(String version) {
		return false;
	}

	abstract void handleTimeout();

	abstract void handleEvent(Event paramEvent);
}
