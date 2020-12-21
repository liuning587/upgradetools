package com.sanxing.upgrade.ui;

import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventDAO;
import com.sanxing.upgrade.core.EventList;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.util.Resources;
import com.sanxing.upgrade.util.SysUtils;
import java.sql.SQLException;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

class TaskEventListViewer extends StyledText {
	private Task currentTask;
	private int lastIndex;
	private boolean autoScroll = true;
	private final Color BLACK = Resources.getSystemColor(2);
	private final Color BLUE = Resources.getSystemColor(10);
	private final Color RED = Resources.getSystemColor(3);

	public TaskEventListViewer(Composite parent, int style) {
		super(parent, style);

		Menu menu = new Menu((Control) getShell());
		setMenu(menu);

		MenuItem menuItem = new MenuItem(menu, 0);
		menuItem.setText("复制(&C)");
		menuItem.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TaskEventListViewer.this.copy();
			}
		});

		menuItem = new MenuItem(menu, 0);
		menuItem.setText("全选(&A)");
		menuItem.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TaskEventListViewer.this.selectAll();
			}
		});

		menuItem = new MenuItem(menu, 32);
		menuItem.setSelection(true);
		menuItem.setText("自动滚屏(&S)");
		menuItem.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TaskEventListViewer.this.autoScroll = !TaskEventListViewer.this.autoScroll;
			}
		});
	}

	private void fillEvent(Event event) {
		StringBuffer info = new StringBuffer(SysUtils.timeToStr(event.time()));
		info.append(" ");
		info.append(event.toString());
		switch (event.type()) {
		//case null: fixme: 注意java语法
		default:
			error(info.toString());
			break;
		case STATE_CHANGED:
			info(info.toString());
			break;
		case START_TASK:
		case RECEIVE_RESPONSE:
			warning(info.toString());
			break;
		}
	}

	public void setInput(Task currentTask) {
		Object[] events;
		setRedraw(false);

		try {
			if (currentTask == null) {
				setText("");

				return;
			}
			if (this.currentTask != currentTask || currentTask.getEvents().count() == 0) {
				this.currentTask = currentTask;
				setText("");
				this.lastIndex = 0;

				EventList list = new EventList();
				try {
					EventDAO.load(currentTask.getTerminalAddr(), list);
				} catch (SQLException e) {
					System.out.println(e);
				}

				Object[] arrayOfObject = list.toArray();
				for (int j = this.lastIndex; j < arrayOfObject.length; j++) {
					fillEvent((Event) arrayOfObject[j]);
					info("\n");
				}
			}

			events = currentTask.getEvents().toArray();
			for (int i = this.lastIndex; i < events.length; i++) {
				fillEvent((Event) events[i]);
				info("\n");
			}
		} finally {
			setRedraw(true);
		}
		if (this.lastIndex != events.length) {
			this.lastIndex = events.length;
			if (this.autoScroll)
				setSelection(getText().length());
		}
	}

	private void log(String message, Color color) {
		StyleRange range = new StyleRange(getCharCount(), message.length(), color, Resources.getSystemColor(1), 0);
		append(message);
		setStyleRange(range);
	}

	private void info(String message) {
		log(message, this.BLACK);
	}

	private void warning(String message) {
		log(message, this.BLUE);
	}

	private void error(String message) {
		log(message, this.RED);
	}
}
