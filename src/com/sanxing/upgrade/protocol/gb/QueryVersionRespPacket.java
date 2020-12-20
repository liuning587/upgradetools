package com.sanxing.upgrade.protocol.gb;

public class QueryVersionRespPacket extends GBPacket {
	private static final long serialVersionUID = -8581326334186051080L;
	private String version;

	public QueryVersionRespPacket() {
		setType(1024);
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return this.version;
	}
}
