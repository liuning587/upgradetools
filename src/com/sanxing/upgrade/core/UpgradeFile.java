package com.sanxing.upgrade.core;

import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.util.SysUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class UpgradeFile {
	private ProtocolType protocolType;
	private String fullName;
	private String name;
	private int size;
	private int zipSize;
	private String zipMd5;
	private String version;
	private UpgradeFileType type;
	private int splitLength;
	private boolean zip;
	private byte[][] sections;
	private byte[] css;
	private String sign;

	public UpgradeFile(ProtocolType protocolType, String fullName, int splitLength, boolean zip) {
		this.protocolType = protocolType;
		this.fullName = fullName;
		this.splitLength = splitLength;
		this.zip = zip;
		this.sign = "";
	}

	public String getName() {
		return this.name;
	}

	public void setType(UpgradeFileType type) {
		this.type = type;
	}

	public UpgradeFileType getType() {
		return this.type;
	}

	public void load() throws Exception {
		File fileDest;
		if (this.splitLength == 0) {
			throw new IllegalArgumentException("设置分段长度前不能调用load()函数");
		}

		File fileSrc = new File(this.fullName);

		if (!fileSrc.isFile()) {
			throw new Exception("指定的升级文件不存在");
		}

		String[] strs = this.fullName.split("\\\\");
		this.name = strs[strs.length - 1];

		if (2147483647L < fileSrc.length())
			throw new Exception("指定的升级文件太大");
		this.size = (int) fileSrc.length();

		if (this.zip) {

			fileDest = new File(String.valueOf(this.fullName) + ".z");

			if (!fileDest.isFile() || 0L == fileDest.length()) {

				File fileZip = new File(String.valueOf(System.getProperty("user.dir"))
						+ System.getProperty("file.separator") + "zlib.exe");
				if (!fileZip.isFile()) {
					throw new Exception("找不到zlib.exe文件");
				}
				try {
					Process p = Runtime.getRuntime().exec("zlib d " + fileSrc.getPath());

					p.waitFor();
				} catch (Exception e) {
					throw e;
				}

				if (!fileDest.isFile() || 0L == fileDest.length())
					throw new Exception("压缩\"" + this.name + "\"文件失败，请将其移动到其它目录下(不含中文及特殊符号)重试");
			}
		} else {
			fileDest = new File(this.fullName);
		}

		this.zipSize = (int) fileDest.length();

		InputStream fis = new FileInputStream(fileDest);
		byte[] bytes = new byte[this.zipSize];
		try {
			if (this.zipSize != fis.read(bytes, 0, this.zipSize))
				throw new Exception("读取目标文件失败");
		} finally {
			fis.close();
		}

		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(bytes);
		this.zipMd5 = SysUtils.bytesToHex(md.digest());

		int n = this.zipSize / this.splitLength;

		int m = this.zipSize % this.splitLength;

		this.sections = (m == 0) ? new byte[n][] : new byte[n + 1][];

		this.css = new byte[this.sections.length];

		for (int i = 0; i < n; i++) {
			this.sections[i] = Arrays.copyOfRange(bytes, i * this.splitLength, i * this.splitLength + this.splitLength);
			this.css[i] = PacketParser.calcCs(this.sections[i]);
		}

		if (m != 0) {
			this.sections[n] = Arrays.copyOfRange(bytes, n * this.splitLength, n * this.splitLength + m);
			this.css[n] = PacketParser.calcCs(this.sections[n]);
		}

		StringBuffer buffer = new StringBuffer();

		buffer.append(this.protocolType);
		buffer.append("-");

		buffer.append(this.zipMd5);
		buffer.append("-");

		buffer.append(this.type.ordinal());
		buffer.append("-");

		buffer.append(this.version);
		buffer.append("-");

		buffer.append(this.splitLength);
		buffer.append("-");

		buffer.append(this.zip);

		this.sign = buffer.toString();
	}

	public int getSplitLength() {
		return this.splitLength;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return this.version;
	}

	public int getSize() {
		return this.size;
	}

	public int getZipSize() {
		return this.zipSize;
	}

	public String getZipMd5() {
		return this.zipMd5;
	}

	public byte[][] getSections() {
		return this.sections;
	}

	public byte[] getCss() {
		return this.css;
	}

	public String getSign() {
		return this.sign;
	}

	public boolean getZip() {
		return this.zip;
	}
}
