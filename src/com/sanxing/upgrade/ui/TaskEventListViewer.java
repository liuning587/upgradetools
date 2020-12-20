package com.sanxing.upgrade.ui;

import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.protocol.Packet;
import com.sanxing.upgrade.util.Resources;
import com.sanxing.upgrade.util.SysUtils;
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

	public void setInput(Task currentTask) {
		if (currentTask == null) {
			setText("");

			return;
		}
		if (this.currentTask != currentTask) {
			this.currentTask = currentTask;
			setText("");
			this.lastIndex = 0;
		}
		Object[] events = currentTask.getEvents().toArray();
		setRedraw(false);
		try {
			StringBuffer buffer = new StringBuffer();
			for (int i = this.lastIndex; i < events.length; i++) {
				Event event = (Event) events[i];

				buffer.append(SysUtils.timeToStr(event.time()));
				buffer.append(" ");

				buffer.append(event.getRemark());

				if (event.attachment() != null && event.attachment() instanceof Packet) {
					buffer.append("(");
					buffer.append(SysUtils.bytesToHex(((Packet) event.attachment()).getData()));
					buffer.append(")");
				}

				buffer.append("\n");
				switch (event.type()) {
//				case null: fixme:  注意java语法
//					error(buffer.toString());
//					break;
				case STATE_CHANGED:
					information(buffer.toString());
					break;
				case START_TASK:
				case RECEIVE_RESPONSE:
					warning(buffer.toString());
					break;
				}
				buffer.setLength(0);
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

	private void information(String message) {
		log(message, this.BLACK);
	}

	private void warning(String message) {
		log(message, this.BLUE);
	}

	private void error(String message) {
		log(message, this.RED);
	}
}
