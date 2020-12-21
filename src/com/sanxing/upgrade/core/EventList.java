package com.sanxing.upgrade.core;

import java.io.Serializable;
import java.util.ArrayList;

public class EventList implements Serializable {
	private static final long serialVersionUID = -2226279241508227918L;
	private ArrayList<Event> list = new ArrayList<Event>(30);

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
		for (int i = 0; i < this.list.size(); i++) {
			if (((Event) this.list.get(i)).isPending()) {
				return this.list.get(i);
			}
		}
		return null;
	}

	public synchronized void done() {
		for (int i = 0; i < this.list.size(); i++) {
			if (((Event) this.list.get(i)).isPending())
				((Event) this.list.get(i)).done();
		}
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
		for (int i = 0; i < this.list.size(); i++) {
			Event event = this.list.get(i);
			if (event.isPending() && EventType.CUSTOMER_BREAK == event.type())
				return true;
		}
		return false;
	}

	public synchronized void clear() {
		this.list.clear();
	}
}
