package com.sanxing.upgrade.core;

import java.util.Collection;
import java.util.LinkedList;

public class BlockingQueue<T> {
	private LinkedList<T> list = new LinkedList<T>();

	public T take() {
		synchronized (this.list) {

			if (this.list.isEmpty()) {
				try {
					this.list.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (this.list.isEmpty()) {
				return null;
			}
			return this.list.removeFirst();
		}
	}

	public void put(T t) {
		synchronized (this.list) {
			this.list.addLast(t);
			this.list.notifyAll();
		}
	}

	public void putAll(Collection<T> ts) {
		synchronized (this.list) {
			for (T t : ts)
				this.list.addLast(t);
		}
	}

	public void clear() {
		synchronized (this.list) {
			this.list.clear();
		}
	}

	public void wakeup() {
		synchronized (this.list) {
			this.list.notifyAll();
		}
	}

	public Object[] toArray() {
		synchronized (this.list) {
			return this.list.toArray();
		}
	}

	public int size() {
		synchronized (this.list) {
			return this.list.size();
		}
	}
}
