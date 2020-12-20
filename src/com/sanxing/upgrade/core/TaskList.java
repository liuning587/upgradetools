package com.sanxing.upgrade.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class TaskList implements Serializable {
	private static final long serialVersionUID = -2917587944199081401L;
	private static final String filename = "TaskList.zer";
	private LinkedHashMap<String, Task> list = new LinkedHashMap<String, Task>();

	public boolean isExists(String terminalAddr) {
		return this.list.containsKey(terminalAddr);
	}

	public void put(Task task) {
		this.list.put(task.getTerminalAddr(), task);
	}

	public void remove(Task task) {
		this.list.remove(task.getTerminalAddr());
	}

	public Task get(String terminalAddr) {
		return this.list.get(terminalAddr);
	}

	public int count() {
		return this.list.size();
	}

	public TasksStatInfo getStatInfo() {
		TasksStatInfo result = new TasksStatInfo();

		result.count = this.list.size();
		for (Task task : this.list.values()) {

			if (task.isUpgrading() || task.isCanceling()) {
				result.runningCount++;
				continue;
			}
			if (task.isFinish()) {
				result.finishedCount++;
			}
		}
		result.canStartCount = result.count - result.runningCount - result.finishedCount;

		result.finishedRate = Math.round(result.finishedCount / result.count * 100.0F * 100.0F) / 100.0F;

		return result;
	}

	public boolean isDone() {
		for (Task task : this.list.values()) {
			if (!task.isFinish())
				return false;
		}
		return true;
	}

	public Object[] toArray() {
		return this.list.values().toArray();
	}

	public Object[] unfinishedTasks() {
		List<Task> result = new LinkedList<Task>();
		Iterator<Task> itr = this.list.values().iterator();
		while (itr.hasNext()) {
			Task task = itr.next();
			if (!task.isFinish())
				result.add(task);
		}
		return result.toArray();
	}

	public void clearDone() {
		Iterator<Task> itr = this.list.values().iterator();
		while (itr.hasNext()) {
			if (((Task) itr.next()).isFinish())
				itr.remove();
		}
	}

	public void save() {
		try {
			ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream("TaskList.zer"));
			stream.writeObject(this);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static TaskList load() {
		try {
			File file = new File("TaskList.zer");
			if (file.exists()) {
				ObjectInputStream stream = new ObjectInputStream(new FileInputStream("TaskList.zer"));
				TaskList taskList = (TaskList) stream.readObject();
				stream.close();
				return taskList;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void clear() {
		this.list.clear();
	}
}
