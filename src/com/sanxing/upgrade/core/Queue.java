package com.sanxing.upgrade.core;

import java.util.LinkedList;

public class Queue<T> {
	private LinkedList<T> list = new LinkedList<T>();

	public T take() {
		synchronized (this.list) {
			if (this.list.isEmpty()) {
				return null;
			}
			return this.list.removeFirst();
		}
	}

	public void put(T t) {
		synchronized (this.list) {
			this.list.addLast(t);
		}
	}

	public void clear() {
		synchronized (this.list) {
			this.list.clear();
		}
	}

	public Object[] toArray() {
		synchronized (this.list) {
			return this.list.toArray();
		}
	}
}
