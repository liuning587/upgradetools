package com.sanxing.upgrade.business;

import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.ITaskChangedListener;
import com.sanxing.upgrade.core.ProtocolType;
import com.sanxing.upgrade.core.Queue;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.core.TaskList;
import com.sanxing.upgrade.core.UpgradeFile;
import com.sanxing.upgrade.core.UpgradeFileType;
import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.protocol.gb.GB09PacketParser;
import com.sanxing.upgrade.protocol.gb.GBPacketParser;
import com.sanxing.upgrade.protocol.sb.SBPacketParser;
import com.sanxing.upgrade.util.Resources;

public class UpgradeService {
	private static UpgradeService instance = new UpgradeService();

	private static PacketParser parser = null;

	private boolean closed = true;

	private TaskList tasks = TaskList.load();
	private FepConnectController fepConnectController;
	private ITaskChangedListener taskChangedListener;

	private UpgradeService() {
		if (this.tasks == null) {
			this.tasks = new TaskList();
		}

		this.waitingTasks = new Queue();

		this.faultTasks = new Queue();

		this.fepConnectController = new FepConnectController();
	}

	private Queue<Task> waitingTasks;
	private Queue<Task> faultTasks;
	private UpgradeFile upgradeFile;

	public static UpgradeService getInstance() {
		return instance;
	}

	public Task appendTask(String terminalAddr) {
		if (this.tasks.isExists(terminalAddr)) {
			return null;
		}
		Task task = new Task(terminalAddr);

		this.tasks.put(task);

		return task;
	}

	public void removeTask(Task task) {
		this.tasks.remove(task);
	}

	private void updateParams() throws Exception {
		String str = Resources.getProperty("PROP_HOSTNAME");
		if (str.isEmpty()) {
			throw new Exception("未设置前置机地址");
		}

		ProtocolType type = ProtocolType.values()[Integer.valueOf(Resources.getProperty("PROP_PROTOCOL_TYPE"))
				.intValue()];

		this.upgradeFile = new UpgradeFile(type, Resources.getProperty("PROP_FILENAME"),
				Integer.valueOf(Resources.getProperty("PROP_SPLIT_LENGTH")).intValue(),
				Boolean.valueOf(Resources.getProperty("PROP_ZIP")).booleanValue());

		this.upgradeFile
				.setType(UpgradeFileType.values()[Integer.valueOf(Resources.getProperty("PROP_FILE_TYPE")).intValue()]);

		str = Resources.getProperty("PROP_FILE_VERSION");
		this.upgradeFile.setVersion(str);

		this.upgradeFile.load();
	}

	public UpgradeFile getUpgradeFile() {
		return this.upgradeFile;
	}

	public void addTaskChangedListener(ITaskChangedListener listener) {
		this.taskChangedListener = listener;
	}

	public void taskChanged(Task task) {
		this.taskChangedListener.taskChanged(task);
	}

	public void startAll() {
		Object[] array = this.tasks.toArray();

		for (int i = 0; i < array.length; i++) {
			Task task = (Task) array[i];

			if (!task.isFinish() && !task.isUpgrading() && !task.isCanceling()) {
				startTask(task);
			}
		}
	}

	public void connect() throws Exception {
		updateParams();

		this.waitingTasks.clear();

		this.faultTasks.clear();

		this.closed = false;

		this.fepConnectController.open();
	}

	public void disconnect() {
		this.fepConnectController.close();

		synchronized (this.waitingTasks) {
			this.waitingTasks.notifyAll();
		}

		Object[] waitings = this.waitingTasks.toArray();
		byte b;
		int i;
		Object[] arrayOfObject1;
		for (i = (arrayOfObject1 = waitings).length, b = 0; b < i;) {
			Object task = arrayOfObject1[b];
			((Task) task).stop();
			b++;
		}

		waitings = this.faultTasks.toArray();
		for (i = (arrayOfObject1 = waitings).length, b = 0; b < i;) {
			Object task = arrayOfObject1[b];
			((Task) task).stop();
			b++;
		}

		this.closed = true;
	}

	public boolean isClosed() {
		return this.closed;
	}

	public TaskList getTasks() {
		return this.tasks;
	}

	public boolean cancelTask(Task task) {
		if (task.isCanceling() || task.isUpgrading()) {
			return false;
		}
		task.addState(6144);

		if (!task.isNew())
			task.removeState(10);
		this.waitingTasks.put(task);

		synchronized (this.waitingTasks) {
			this.waitingTasks.notifyAll();
		}

		return true;
	}

	private boolean prepareTask(Task task) {
		if (task.isCanceling() || task.isUpgrading()) {
			return false;
		}

		task.addState(5120);

		if (task.isFinish()) {
			task.removeState(26);
		}

		if (task.getFileSign().isEmpty()) {

			task.setFileSign(this.upgradeFile.getSign());

		} else if (task.getFileSign().compareTo(this.upgradeFile.getSign()) != 0) {
			if ((task.getState() & 0x2) != 0) {
				task.removeState(10);

				task.addEvent(Event.create(EventType.STATE_CHANGED,
						"升级内容改变，导致升级重新开始(" + task.getFileSign() + " -> " + this.upgradeFile.getSign() + ")"));
			}

			task.setFileSign(this.upgradeFile.getSign());
		}

		return true;
	}

	public boolean startTask(Task task) {
		if (prepareTask(task)) {
			this.waitingTasks.put(task);

			synchronized (this.waitingTasks) {
				this.waitingTasks.notifyAll();
			}
		}
		return true;
	}

	public void pauseTask(Task task) {
		if (!task.isCanceling() && !task.isUpgrading())
			return;
		task.addEvent(Event.createPending(EventType.CUSTOMER_BREAK, "用户中断"));
	}

	public Queue<Task> getWaitingTasks() {
		return this.waitingTasks;
	}

	public Queue<Task> getFaultTasks() {
		return this.faultTasks;
	}

	public void clearDone() {
		this.tasks.clearDone();
	}

	public PacketParser getPacketParser() {
		if (parser == null) {
			ProtocolType type = ProtocolType.values()[Integer.valueOf(Resources.getProperty("PROP_PROTOCOL_TYPE"))
					.intValue()];
			if (ProtocolType.GB == type) {
				parser = (PacketParser) new GBPacketParser();
			} else if (ProtocolType.GB09 == type) {
				parser = (PacketParser) new GB09PacketParser();
			} else {
				parser = (PacketParser) new SBPacketParser();
			}

		}
		return parser;
	}
 }
