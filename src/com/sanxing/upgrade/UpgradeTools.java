package com.sanxing.upgrade;

import com.sanxing.upgrade.core.DBManager;
import com.sanxing.upgrade.ui.FormMain;
import com.sanxing.upgrade.util.Resources;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

public class UpgradeTools {
	public static void main(String[] args) {
		Display display = new Display();

		try {
			try {
				Resources.initialize();
				DBManager.initialize();
			} catch (Exception e1) {
				ErrorDialog.openError(null, "注意", "无法使用系统", (IStatus) new Status(4, "sxcms", "初始化失败", e1), 4);

				return;
			}

			FormMain shell = new FormMain(display);
			shell.open();
			shell.layout();

			while (!shell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}

		catch (Exception e2) {
			ErrorDialog.openError(null, "注意", "系统将中断运行，请按“详细信息”按钮，记录异常内容并提供给研发人员处理，谢谢！",
					(IStatus) new Status(4, "sxcms", "发现未知异常", e2), 4);
		} finally {
			if (display != null)
				display.dispose();
		}
		if (display != null)
			display.dispose();
	}
}
