package com.sanxing.upgrade.ui;

import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.util.SysUtils;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

class TaskViewerSorter extends ViewerSorter {
	private Table table;
	private TableColumn sortColumn;
	private boolean ascending;

	public TaskViewerSorter(TableViewer viewer) {
		this.table = viewer.getTable();
	}

	public void setSortColumn(TableColumn sortColumn) {
		if (this.sortColumn != sortColumn) {
			this.ascending = true;
			this.sortColumn = sortColumn;
			this.table.setSortColumn(sortColumn);
		} else {

			this.ascending = !this.ascending;
		}
		if (this.ascending) {
			this.table.setSortDirection(128);
		} else {
			this.table.setSortDirection(1024);
		}
	}

	public int compare(Viewer viewer, Object e1, Object e2) {
		int result = 0;
		if (this.sortColumn == null) {
			return 0;
		}
		Task t1 = (Task) e1;
		Task t2 = (Task) e2;
		String columnName = this.sortColumn.getText();
		if (columnName.compareTo("终端地址") == 0) {
			result = SysUtils.compareString(t1.getTerminalAddr(), t2.getTerminalAddr());
		} else if (columnName.compareTo("状态") == 0) {
			result = SysUtils.compareString(t1.getStateRemark(), t2.getStateRemark());
		} else if (columnName.compareTo("升级前版本") == 0) {
			result = SysUtils.compareString(t1.getOldVersion(), t2.getOldVersion());
		} else if (columnName.compareTo("当前版本") == 0) {
			result = SysUtils.compareString(t1.getCurrentVersion(), t2.getCurrentVersion());
		} else if (columnName.compareTo("进度") == 0) {
			result = t1.getRate() - t2.getRate();
		} else {
			Event event1 = t1.lastEvent();
			Event event2 = t2.lastEvent();
			if (event1 == null || event2 == null)
				return 0;
			if (columnName.compareTo("最近事件") == 0) {
				result = SysUtils.compareString(event1.getRemark(), event2.getRemark());
			} else if (columnName.compareTo("发生时间") == 0) {
				result = SysUtils.compareDatetime(event1.time(), event2.time());
			}
		}
		if (!this.ascending)
			result = -result;
		return result;
	}
}
