package com.sanxing.upgrade.core;

import java.io.Serializable;
import java.util.ArrayList;

public class EventList implements Serializable {
	private static final long serialVersionUID = -2226279241508227918L;
	private ArrayList<Event> list = new ArrayList<Event>(30);
	private int pendingIndex = 0;

	public synchronized void add(Event event) {
		this.list.add(event);
		notifyAll();
	}

	public synchronized void close() {
		notifyAll();
	}

	public synchronized int count() {
		return this.list.size();
	}

	public synchronized Event nextPending() {
		if (this.pendingIndex == this.list.size())
			return null;
		for (int i = this.pendingIndex; i < this.list.size(); i++) {
			if (((Event) this.list.get(i)).isPending()) {
				this.pendingIndex = i + 1;
				return this.list.get(i);
			}
		}
		this.pendingIndex = this.list.size();

		return null;
	}

	public synchronized void done() {
		for (int i = this.pendingIndex; i < this.list.size(); i++) {
			if (((Event) this.list.get(i)).isPending())
				((Event) this.list.get(i)).done();
		}
		this.pendingIndex = this.list.size();
	}

	public synchronized Event last() {
		if (!this.list.isEmpty()) {
			return this.list.get(this.list.size() - 1);
		}
		return null;
	}

	public synchronized Object[] toArray() {
		return this.list.toArray();
	}

	public synchronized boolean hasBreakPending() {
		for (int i = this.pendingIndex; i < this.list.size(); i++) {
			Event event = this.list.get(i);
			if (event.isPending() && EventType.CUSTOMER_BREAK == event.type())
				return true;
		}
		return false;
	}
}
