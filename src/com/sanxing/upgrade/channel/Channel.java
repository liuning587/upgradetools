package com.sanxing.upgrade.channel;

import com.sanxing.upgrade.business.FepConnector;
import com.sanxing.upgrade.core.BlockingQueue;
import com.sanxing.upgrade.protocol.Packet;
import java.net.InetSocketAddress;

public class Channel {
	private FepConnector owner;
	private TcpThread tcpThread;
	private BlockingQueue<Packet> requestQueue;
	private BlockingQueue<Packet> responseQueue;
	private volatile boolean closed;

	public Channel(FepConnector owner, BlockingQueue<Packet> responseQueue) {
		this.owner = owner;
		this.responseQueue = responseQueue;
		this.requestQueue = new BlockingQueue<Packet>();
		this.closed = true;
	}

	public boolean open(String hostname, int port) {
		this.tcpThread = new TcpThread(this, new InetSocketAddress(hostname, port), this.requestQueue,
				this.responseQueue);

		if (this.tcpThread.open()) {
			this.tcpThread.start();
			this.closed = false;
		} else {
			this.closed = true;
		}

		return !this.closed;
	}

	public void close() {
		this.closed = true;

		this.tcpThread.close();

		this.requestQueue.wakeup();

		if (this.tcpThread.isAlive()) {
			try {
				this.tcpThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isClosed() {
		return this.closed;
	}

	public void send(Packet packet) {
		this.requestQueue.put(packet);
	}

	public void socketIsClosed() {
		this.closed = true;
		this.owner.channelIsClosed();
	}
 }
