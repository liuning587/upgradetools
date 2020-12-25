package com.sanxing.upgrade.util;

import com.sanxing.upgrade.core.Queue;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class Logger {
	private static final Color BLACK = Resources.getSystemColor(2);

	private static final Color BLUE = Resources.getSystemColor(10);
	private static final Color RED = Resources.getSystemColor(3);

	private static StyledText textLog;

	private static Runnable runnable;

	private static Queue<Loginfo> loginfoQueue = new Queue<Loginfo>();

	public static void init(StyledText textLog) {
		Logger.textLog = textLog;
		runnable = new Runnable() {
			public void run() {
				if (Logger.textLog.isDisposed())
					return;
				while (true) {
					Loginfo loginfo = (Loginfo) Logger.loginfoQueue.take();
					if (loginfo == null) {
						Logger.textLog.setSelection(Logger.textLog.getCharCount());
						return;
					}
					Logger.log(loginfo);
				}
			}
		};
	}

	public static void info(String message) {
		loginfoQueue.put(new Loginfo(LogType.INFORMATION, message));
		Display.getDefault().asyncExec(runnable);
	}

	public static void error(String message) {
		loginfoQueue.put(new Loginfo(LogType.ERROR, message));
		Display.getDefault().asyncExec(runnable);
	}

	public static void warning(String message) {
		loginfoQueue.put(new Loginfo(LogType.WARNING, message));
		Display.getDefault().asyncExec(runnable);
	}

	public static void debug(String message) {
		error(message);
	}

	private static void log(Loginfo loginfo) {
		Color color = BLACK;
		switch (loginfo.getType()) {
		case ERROR://null: fixme:  注意java语法
			color = RED;
			break;
		case WARNING:
			color = BLUE;
			break;
		default:
			break;
		}
		String message = String.valueOf(SysUtils.timeToStr(loginfo.getTime())) + " " + loginfo.getMessage() + "\n";
		StyleRange range = new StyleRange(textLog.getCharCount(), message.length(), color, Resources.getSystemColor(1),
				0);
		textLog.append(message);
		textLog.setStyleRange(range);
	}

	public static void printHexString(byte[] b) {
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			hex += ' ';
			System.out.print(hex.toUpperCase());
		}

		System.out.println();
	}

}
