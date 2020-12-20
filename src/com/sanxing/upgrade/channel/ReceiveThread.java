package com.sanxing.upgrade.channel;

import com.sanxing.upgrade.core.BlockingQueue;
import com.sanxing.upgrade.protocol.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

class ReceiveThread extends Thread {
	private TcpThread owner;
	private Selector selector;
	private BlockingQueue<Packet> responseQueue;
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

	private volatile boolean closed;

	public ReceiveThread(TcpThread owner, Selector selector, BlockingQueue<Packet> responseQueue) {
		this.owner = owner;
		this.selector = selector;
		this.responseQueue = responseQueue;
	}

	public void close() {
		this.closed = true;

		this.selector.wakeup();
	}

	public void run() {
		this.closed = false;
		while (!this.closed) {

			try {
				if (this.selector.select() > 0) {
					Set<SelectionKey> keys = this.selector.selectedKeys();
					Iterator<SelectionKey> itr = keys.iterator();
					while (itr.hasNext()) {
						SelectionKey key = itr.next();
						itr.remove();
						if (key.isValid() && key.isReadable()) {
							SocketChannel channel = (SocketChannel) key.channel();
							int len = channel.read(this.readBuffer);
							if (-1 == len) {
								this.closed = true;
								break;
							}
							this.readBuffer.flip();
							Packet packet = new Packet();
							packet.setData(Arrays.copyOf(this.readBuffer.array(), len));

							this.responseQueue.put(packet);
							this.readBuffer.clear();
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		while (true) {
			this.closed = true;

			this.owner.readThreadIsStopped();
			return;
		}
	}
}
