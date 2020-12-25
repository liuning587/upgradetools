package com.sanxing.upgrade.business;

import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventDAO;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.ITaskChangedListener;
import com.sanxing.upgrade.core.ProtocolType;
import com.sanxing.upgrade.core.Queue;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.core.TaskDAO;
import com.sanxing.upgrade.core.TaskList;
import com.sanxing.upgrade.core.UpgradeFile;
import com.sanxing.upgrade.core.UpgradeFileType;
import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.protocol.gb.GB09PacketParser;
import com.sanxing.upgrade.protocol.gb.GBPacketParser;
import com.sanxing.upgrade.protocol.sb.SBPacketParser;
import com.sanxing.upgrade.protocol.dlt698.DLT698PacketParser;
import com.sanxing.upgrade.util.Resources;
import java.sql.SQLException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;

public class UpgradeService {
	private static UpgradeService instance = new UpgradeService();

	private static PacketParser parser = null;

	private boolean closed = true;

	private TaskList tasks = new TaskList();
	private FepConnectController fepConnectController;
	private ITaskChangedListener taskChangedListener;

	private UpgradeService() {
		try {
			TaskDAO.load(this.tasks);
		} catch (SQLException e) {
			ErrorDialog.openError(null, "注意", "操作失败", (IStatus) new Status(4, "sxcms", "数据库故障", e), 4);
		}

		if (this.tasks == null) {
			this.tasks = new TaskList();
		}

		this.waitingTasks = new Queue<Task>();

		this.faultTasks = new Queue<Task>();

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

		try {
			TaskDAO.insert(task);

			this.tasks.put(task);
			return task;
		} catch (SQLException e) {
			System.out.println(e);
			return null;
		}
	}

	public void removeTask(Task task) {
		try {
			TaskDAO.delete(task);

			EventDAO.delete(task.getTerminalAddr());
		} catch (SQLException e) {
			System.out.println(e);
		}
		this.tasks.remove(task);
	}

	public void clearTask() {
		try {
			TaskDAO.clear();

			EventDAO.clear();
		} catch (SQLException e) {
			System.out.println(e);
		}
		this.tasks.clear();
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
		for (int i = 0; i < waitings.length; i++) {
			((Task) waitings[i]).stop();
		}

		waitings = this.faultTasks.toArray();
		for (int i = 0; i < waitings.length; i++) {
			((Task) waitings[i]).stop();
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
		task.addState(Task.STATE_WAITING | Task.STATE_CANCELING);

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

		task.addState(Task.STATE_WAITING | Task.STATE_UPGRADING);

		if (task.isFinish()) {
			task.removeState(26);
		}

		if (task.getFileSign().isEmpty()) {

			task.setFileSign(this.upgradeFile.getSign());

		} else if (task.getFileSign().compareTo(this.upgradeFile.getSign()) != 0) {
			if ((task.getState() & Task.STATE_START_UPGRADE) != 0) {
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
			ProtocolType type = ProtocolType.values()[Integer.valueOf(Resources.getProperty("PROP_PROTOCOL_TYPE")).intValue()];
			if (ProtocolType.DLT698 == type) {
				parser = (PacketParser) new DLT698PacketParser();
			} else if (ProtocolType.GB == type) {
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
