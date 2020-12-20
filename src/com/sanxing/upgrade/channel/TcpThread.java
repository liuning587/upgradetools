package com.sanxing.upgrade.channel;

import com.sanxing.upgrade.core.BlockingQueue;
import com.sanxing.upgrade.protocol.Packet;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class TcpThread extends Thread {
	private Channel owner;
	private InetSocketAddress remote;
	private Selector selector;
	private SocketChannel socketChannel;
	private volatile boolean closed;
	private BlockingQueue<Packet> requestQueue;
	private BlockingQueue<Packet> responseQueue;
	private ReceiveThread receiveThread;

	public TcpThread(Channel owner, InetSocketAddress remote, BlockingQueue<Packet> requestQueue,
			BlockingQueue<Packet> responseQueue) {
		this.owner = owner;
		this.remote = remote;
		this.requestQueue = requestQueue;
		this.responseQueue = responseQueue;
	}

	public boolean isClosed() {
		return this.closed;
	}

	public boolean open() {
		try {
			this.selector = Selector.open();
			this.socketChannel = SocketChannel.open(this.remote);
			this.socketChannel.configureBlocking(false);
			this.socketChannel.register(this.selector, 1);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		this.receiveThread = new ReceiveThread(this, this.selector, this.responseQueue);
		this.receiveThread.start();

		this.closed = false;

		return true;
	}

	public void close() {
		this.closed = true;
	}

	public void run() {
		this.closed = false;

		while (!this.closed) {

			Packet packet = (Packet) this.requestQueue.take();

			if (packet == null) {
				continue;
			}
			ByteBuffer buffer = ByteBuffer.wrap(packet.getData());

			try {
				while (!this.closed && buffer.hasRemaining()) {
					int len = this.socketChannel.write(buffer);

					if (-1 == len) {
						this.closed = true;
						break;
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();

				break;
			}
		}

		this.receiveThread.close();

		if (this.receiveThread.isAlive()) {
			try {
				this.receiveThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		try {
			if (this.selector.isOpen()) {
				this.selector.close();
			}
			if (this.socketChannel.isOpen())
				this.socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.closed = true;

		this.owner.socketIsClosed();
	}

	public void readThreadIsStopped() {
		this.closed = true;

		this.requestQueue.wakeup();
	}
}
