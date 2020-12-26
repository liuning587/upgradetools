package com.sanxing.upgrade.business;

import com.sanxing.upgrade.core.BlockingQueue;
import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.core.TaskList;
import com.sanxing.upgrade.protocol.Packet;
import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.util.Logger;
import com.sanxing.upgrade.util.SysUtils;
import java.util.ArrayList;
import java.util.List;

class ResponseDispatchThread extends Thread {
	private PacketParser parser = UpgradeService.getInstance().getPacketParser();
	private TaskList tasks;
	private FepConnector owner;
	private volatile boolean closed;
	private BlockingQueue<Packet> responseQueue;
	private List<Packet> list;

	public ResponseDispatchThread(FepConnector owner, TaskList tasks, BlockingQueue<Packet> responseQueue) {
		this.list = new ArrayList<Packet>();

		this.tasks = tasks;
		this.owner = owner;
		this.responseQueue = responseQueue;
	}

	public void close() {
		this.closed = true;
	}

	public void run() {
		this.closed = false;
		while (!this.closed) {
			Packet packet = (Packet) this.responseQueue.take();

			if (packet == null) {
				continue;
			}
			
			if (!this.parser.filterPacket(packet, this.list)) {
				Logger.debug("收到非法报文：" + SysUtils.bytesToHex(packet.getData()));

				continue;
			}
			for (Packet vPacket : this.list) {
				Event event;
				if (this.parser.isIgnore(vPacket)) {
					continue;
				}
				
				if (this.parser.isFepResp(vPacket)) {

					Event event1 = Event.createPending(EventType.RECEIVE_RESPONSE, "收到前置机应答");

					event1.attach(vPacket);

					this.owner.handleEvent(event1);
					continue;
				}
				String terminalAddr = null;
				if (this.parser.isATCTResp(vPacket.getData())) {
					Task listener = this.owner.getATCTListener();
					if (listener != null && (listener.isUpgrading() || listener.isCanceling()))
						terminalAddr = listener.getTerminalAddr();
				} else {
					terminalAddr = this.parser.getTerminalAddr(vPacket);
				}
				Task task = this.tasks.get(terminalAddr);

				if (task == null) {
					continue;
				}

				if (task.isUpgrading() || task.isCanceling()) {
					event = Event.createPending(EventType.RECEIVE_RESPONSE, "收到终端应答");
				} else {
					event = Event.create(EventType.RECEIVE_RESPONSE, "收到终端应答，但任务已停止");
				}
				event.attach(vPacket);

				task.addEvent(event);
			}

			this.list.clear();
		}
		this.closed = true;
	}
}
