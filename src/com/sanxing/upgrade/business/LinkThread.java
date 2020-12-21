package com.sanxing.upgrade.business;

import com.sanxing.upgrade.channel.Channel;
import com.sanxing.upgrade.protocol.Packet;
import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.util.Logger;

class LinkThread extends Thread {
	private PacketParser parser = UpgradeService.getInstance().getPacketParser();

	private FepConnector owner;

	private volatile boolean closed;

	private Packet loginReqPacket;

	private Packet heartbeatReqPacket;

	public LinkThread(FepConnector owner, Channel channel) {
		this.owner = owner;

		if (owner.isAllowLoginFeps()) {

			this.loginReqPacket = this.parser.packLoginRequest(owner.getMsta(), owner.getPassword());

			this.heartbeatReqPacket = this.parser.packHeartbeatRequest(owner.getMsta());
		}
	}

	public void close() {
		this.closed = true;
	}

	public void run() {
		this.closed = false;
		Timer timer = new Timer();

		while (!this.closed) {

			if (!this.owner.isConnected()) {
				Logger.warning("通道" + this.owner.getId() + ":连接前置机...");

				if (!this.owner.tryConnect()) {
					Logger.error("通道" + this.owner.getId() + ":连接失败，10秒后重试");

					timer.reset(this.owner, 10000);
					while (!timer.isTimeout()) {
						timer.waitEvent();
						if (this.closed) {
							break;
						}
					}
					continue;
				}
				Logger.warning("通道" + this.owner.getId() + ":已连接前置机");
			}

			if (!this.owner.isAllowLoginFeps()) {
				timer.reset(this.owner, 60000);
				while (!timer.isTimeout()) {
					timer.waitEvent();

					if (!this.owner.isConnected() || this.closed) {
						break;
					}
				}

				continue;
			}
			if (!this.owner.isLogined()) {
				Logger.info("通道" + this.owner.getId() + ":发送登录报文...");
				this.owner.trySend(this.loginReqPacket);

				timer.reset(this.owner, 60000);
				while (!timer.isTimeout()) {
					timer.waitEvent();

					if (this.owner.isLogined() || this.closed) {
						break;
					}
				}

				continue;
			}

			timer.reset(this.owner, this.owner.getHeartbeatInterval() * 1000);
			while (!timer.isTimeout()) {
				timer.waitEvent();

				if (!this.owner.isLogined() || this.closed) {
					break;
				}
			}

			if (!timer.isTimeout()) {
				continue;
			}

			this.owner.checkHeartbeat();

			Logger.info("通道" + this.owner.getId() + ":发送心跳报文...");
			this.owner.trySend(this.heartbeatReqPacket);
		}

		this.closed = true;
	}
}
