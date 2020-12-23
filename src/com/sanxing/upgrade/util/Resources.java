package com.sanxing.upgrade.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class Resources {
	private static final String FILENAME_CUSTOM = "custom.properties";
	private static Properties properties = null;

	private static ImageRegistry imgRegistry = null;

	private static FontRegistry fontRegistry = null;

	public static final String IMAGE_APPLICATION = "IMAGE_APPLICATION";

	public static final String IMAGE_UPGRADE = "IMAGE_UPGRADE";

	public static final String IMAGE_NEW = "IMAGE_NEW";

	public static final String IMAGE_CONNECT = "IMAGE_CONNECT";

	public static final String IMAGE_START_ALL = "IMAGE_START_ALL";

	public static final String IMAGE_DISCONNECT = "IMAGE_DISCONNECT";

	public static final String IMAGE_CLEAR = "IMAGE_CLEAR";

	public static final String IMAGE_SAVE = "IMAGE_SAVE";

	public static final String IMAGE_START = "IMAGE_START";

	public static final String IMAGE_BREAK = "IMAGE_BREAK";

	public static final String IMAGE_CANCEL = "IMAGE_CANCEL";

	public static final String IMAGE_DELETE = "IMAGE_DELETE";

	public static final String IMAGE_TASK_STATE_NEW = "IMAGE_TASK_STATE_NEW";

	public static final String IMAGE_TASK_STATE_WAITING = "IMAGE_TASK_STATE_WAITING";

	public static final String IMAGE_TASK_STATE_UPGRADING = "IMAGE_TASK_STATE_UPGRADING";

	public static final String IMAGE_TASK_STATE_STOP = "IMAGE_TASK_STATE_STOP";

	public static final String IMAGE_TASK_STATE_FINISH = "IMAGE_TASK_STATE_FINISH";

	public static final String IMAGE_INFORMATION_SMALL = "IMAGE_INFORMATION_SMALL";

	public static final String IMAGE_WARNING_SMALL = "IMAGE_WARNING_SMALL";

	public static final String IMAGE_ERROR_SMALL = "IMAGE_ERROR_SMALL";

	public static final String IMAGE_DUKE = "IMAGE_DUKE";

	public static final String FONT_DEFAULT = "FONT_DEFAULT";

	public static final String PROP_PROTOCOL_TYPE = "PROP_PROTOCOL_TYPE";

	public static final String PROP_HOSTNAME = "PROP_HOSTNAME";

	public static final String PROP_PORT = "PROP_PORT";

	public static final String PROP_MSTA = "PROP_MSTA";

	public static final String PROP_ALLOW_LOGIN_FEPS = "PROP_ALLOW_LOGIN_FEPS";

	public static final String PROP_PASSWORD = "PROP_PASSWORD";

	public static final String PROP_HEARTBEAT_INTERVAL = "PROP_HEARTBEAT_INTERVAL";

	public static final String PROP_DYNAMIC_PWD = "PROP_DYNAMIC_PWD";

	public static final String PROP_UPGRADE_PASSWORD = "PROP_UPGRADE_PASSWORD";

	public static final String PROP_SPECIAL_CHANNEL = "PROP_SPECIAL_CHANNEL";

	public static final String PROP_MAX_TASK_COUNT = "PROP_MAX_TASK_COUNT";

	public static final String PROP_SEND_INTERVAL = "PROP_SEND_INTERVAL";

	public static final String PROP_AUTO_CANCEL = "PROP_AUTO_CANCEL";

	public static final String PROP_ALLOW_QUERY_VERSION = "PROP_ALLOW_QUERY_VERSION";

	public static final String PROP_SKIP_NEEDLESS_UPGRADE = "PROP_SKIP_NEEDLESS_UPGRADE";

	public static final String PROP_SKIP_LATER_VERSION = "PROP_SKIP_LATER_VERSION";

	public static final String PROP_AFFIRM_VERSION = "PROP_AFFIRM_VERSION";

	public static final String PROP_RESTART_FAULT_TASK = "PROP_RESTART_FAULT_TASK";

	public static final String PROP_FILENAME = "PROP_FILENAME";

	public static final String PROP_FILE_TYPE = "PROP_FILE_TYPE";

	public static final String PROP_FILE_VERSION = "PROP_FILE_VERSION";

	public static final String PROP_SPLIT_LENGTH = "PROP_SPLIT_LENGTH";

	public static final String PROP_ZIP = "PROP_ZIP";

	public static final String PROP_RESTART_TERMINAL = "PROP_RESTART_TERMINAL";

	public static final String PROP_RESTART_TERMINAL_DELAY = "PROP_RESTART_TERMINAL_DELAY";

	private static String getPropertiesFilename(String filename) {
		StringBuffer fileName = new StringBuffer(System.getProperty("user.dir"));
		fileName.append(System.getProperty("file.separator"));
		fileName.append(filename);
		return fileName.toString();
	}

	public static void initialize() throws Exception {
		properties = new Properties();

		InputStreamReader stream = new InputStreamReader(Resources.class.getResourceAsStream("/system.properties"));
		try {
			properties.load(stream);
		} finally {
			stream.close();
		}

		imgRegistry = new ImageRegistry();

		Iterator<Map.Entry<Object, Object>> iterator = properties.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Object, Object> entry = iterator.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			if (key.indexOf("IMAGE_") == 0) {
				imgRegistry.put(key, ImageDescriptor.createFromURL(Resources.class.getResource(value)));
			}
		}
		fontRegistry = new FontRegistry();
		fontRegistry.put("FONT_DEFAULT", new FontData[] { new FontData("宋体", 9, 0) });

		properties.clear();
		try {
			FileInputStream fs = new FileInputStream(getPropertiesFilename(FILENAME_CUSTOM));
			properties.load(fs);
			fs.close();
		} catch (Exception ex) {
			InputStreamReader custom = new InputStreamReader(Resources.class.getResourceAsStream("/custom.properties"));
			properties.load(custom);
			custom.close();
		} finally {
		}
	}

	public static String getProperty(String key) {
		if (properties == null)
			throw new IllegalArgumentException();
		return properties.getProperty(key);
	}

	public static void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public static void saveCustomProperty() throws Exception {
		FileOutputStream stream = new FileOutputStream(getPropertiesFilename(FILENAME_CUSTOM));
		try {
			properties.store(stream, (String) null);
		} finally {
			stream.close();
		}
	}

	public static ImageDescriptor getImageDescriptor(String key) {
		if (imgRegistry == null) {
			return null;
		}
		return imgRegistry.getDescriptor(key);
	}

	public static Image getImage(String key) {
		if (imgRegistry == null) {
			return null;
		}
		return imgRegistry.get(key);
	}

	public static Font getFont(String key) {
		if (fontRegistry == null) {
			return null;
		}
		return fontRegistry.get(key);
	}

	public static Font getBoldFont(String key) {
		if (fontRegistry == null) {
			return null;
		}
		return fontRegistry.getBold(key);
	}

	public static Font getItalicFont(String key) {
		if (fontRegistry == null) {
			return null;
		}
		return fontRegistry.getItalic(key);
	}

	public static Color getSystemColor(int id) {
		return Display.getCurrent().getSystemColor(id);
	}

	public static Image getSystemImage(int id) {
		return Display.getCurrent().getSystemImage(id);
	}
}
