package com.sanxing.upgrade.business;

import com.sanxing.upgrade.channel.Channel;
import com.sanxing.upgrade.core.BlockingQueue;
import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.ProtocolType;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.protocol.Packet;
import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.util.Logger;
import com.sanxing.upgrade.util.Resources;
import java.util.Calendar;
import java.util.Date;

public class FepConnector {
	private UpgradeService service = UpgradeService.getInstance();
	private PacketParser parser = this.service.getPacketParser();

	private int taskCount;

	private int id;

	private String hostname;

	private int port;

	private byte msta;

	private boolean allowLoginFeps;

	private String password;

	private int heartbeatInterval;

	private Channel channel;

	private LinkThread heartbeatThread;

	private boolean closed;

	private boolean logined;

	private Date lastHeartbeatTime;

	private BlockingQueue<Packet> responseQueue;

	private ResponseDispatchThread responseDispatchThread;

	private UpgradeThread[] upgradeThreads;

	private Task listenerTask;

	public FepConnector(int taskCount, int id) {
		this.taskCount = taskCount;
		this.id = id;

		this.logined = false;

		this.closed = true;
	}

	private void updateParams() {
		this.hostname = Resources.getProperty("PROP_HOSTNAME");
		this.port = Integer.valueOf(Resources.getProperty("PROP_PORT")).intValue();
		this.msta = Integer.valueOf(Resources.getProperty("PROP_MSTA")).byteValue();
		this.allowLoginFeps = Boolean.valueOf(Resources.getProperty("PROP_ALLOW_LOGIN_FEPS")).booleanValue();
		if (this.allowLoginFeps) {
			this.password = Resources.getProperty("PROP_PASSWORD");
			this.heartbeatInterval = Integer.valueOf(Resources.getProperty("PROP_HEARTBEAT_INTERVAL")).intValue();
		}
	}

	public void resetChannel() {
		this.channel.close();
	}

	public void open() {
		this.closed = false;

		updateParams();

		this.responseQueue = new BlockingQueue<Packet>();

		this.channel = new Channel(this, this.responseQueue);

		this.heartbeatThread = new LinkThread(this, this.channel);

		this.responseDispatchThread = new ResponseDispatchThread(this, this.service.getTasks(), this.responseQueue);

		synchronized (this) {

			this.logined = false;
		}

		this.heartbeatThread.start();

		this.responseDispatchThread.start();

		ProtocolType type = ProtocolType.values()[Integer.valueOf(Resources.getProperty("PROP_PROTOCOL_TYPE"))
				.intValue()];
		if (type == ProtocolType.SB) {
			this.upgradeThreads = (UpgradeThread[]) new SBUpgradeThread[this.taskCount];
			for (int i = 0; i < this.taskCount; i++) {
				this.upgradeThreads[i] = new SBUpgradeThread(this);
				this.upgradeThreads[i].start();
			}
		} else {
			this.upgradeThreads = (UpgradeThread[]) new GBUpgradeThread[this.taskCount];
			for (int i = 0; i < this.taskCount; i++) {
				this.upgradeThreads[i] = new GBUpgradeThread(this);
				this.upgradeThreads[i].start();
			}
		}
	}

	public void close() {
		this.closed = true;

		this.heartbeatThread.close();

		synchronized (this) {

			this.logined = false;
			notifyAll();
		}

		this.channel.close();

		if (this.heartbeatThread.isAlive()) {
			try {
				this.heartbeatThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for (int i = 0; i < this.upgradeThreads.length; i++) {
			this.upgradeThreads[i].close();
		}

		this.responseDispatchThread.close();

		synchronized (this.service.getWaitingTasks()) {
			this.service.getWaitingTasks().notifyAll();
		}

		this.responseQueue.wakeup();

		for (int i = 0; i < this.upgradeThreads.length; i++) {
			UpgradeThread thread = this.upgradeThreads[i];
			if (thread.isAlive()) {

				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
		}

		if (this.responseDispatchThread.isAlive()) {
			try {
				this.responseDispatchThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();

				return;
			}
		}
		Logger.error("通道" + this.id + ":已结束连接");
	}

	public synchronized boolean isConnected() {
		return !this.channel.isClosed();
	}

	public synchronized boolean isLogined() {
		return (this.logined && isConnected());
	}

	public synchronized void send(Packet packet) {
		if (this.allowLoginFeps) {
			while (!isLogined()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (this.closed)
					return;
			}
		}
		this.channel.send(packet);
	}

	public void trySend(Packet packet) {
		this.channel.send(packet);
	}

	public boolean tryConnect() {
		if (!this.channel.isClosed()) {
			this.channel.close();
		}

		return this.channel.open(this.hostname, this.port);
	}

	public void handleEvent(Event event) {
		if (EventType.RECEIVE_RESPONSE == event.type()) {

			Packet packet = this.parser.unpackReponse((Packet) event.attachment());

			switch (packet.getType()) {
			case 1:
				Logger.warning("通道" + this.id + ":已登录前置机");
				login();
				break;
			case 2:
				Logger.info("通道" + this.id + ":收到心跳应答");
				heartbeat();
				break;
			case 512:
				Logger.info("通道" + this.id + ":收到确认应答");
				login();
				heartbeat();
				break;
			}
		}
	}

	private synchronized void login() {
		this.logined = true;

		this.lastHeartbeatTime = new Date();
		notifyAll();
	}

	private synchronized void logout() {
		this.logined = false;
		notifyAll();
	}

	private synchronized void heartbeat() {
		this.lastHeartbeatTime = new Date();
		notifyAll();
	}

	public synchronized void checkHeartbeat() {
		if (!this.logined) {
			return;
		}

		Calendar c = Calendar.getInstance();

		c.setTime(this.lastHeartbeatTime);
		c.add(13, this.heartbeatInterval * 2);

		if (c.before(Calendar.getInstance())) {
			Logger.error("通道" + this.id + ":心跳已超时");
			logout();
		}
	}

	public synchronized void channelIsClosed() {
		Logger.error("通道" + this.id + ":通道已关闭");
		logout();
	}

	public int getId() {
		return this.id;
	}

	public void addATCTListener(Task listenerTask) {
		this.listenerTask = listenerTask;
	}

	public Task getATCTListener() {
		return this.listenerTask;
	}

	public boolean isAllowLoginFeps() {
		return this.allowLoginFeps;
	}

	public byte getMsta() {
		return this.msta;
	}

	public String getPassword() {
		return this.password;
	}

	public int getHeartbeatInterval() {
		return this.heartbeatInterval;
	}
}
