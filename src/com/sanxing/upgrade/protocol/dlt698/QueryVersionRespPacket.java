package com.sanxing.upgrade.protocol.dlt698;

public class QueryVersionRespPacket extends DLT698Packet {
	private static final long serialVersionUID = 572240496709073737L;
	private String version;

	public QueryVersionRespPacket() {
		setType(VERSION_RESP);
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return this.version;
	}
}
