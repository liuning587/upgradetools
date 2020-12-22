package com.sanxing.upgrade.protocol.dlt698;

public class LinkReqPacket extends DLT698Packet {
	private static final long serialVersionUID = 3387841178347227758L;

	public LinkReqPacket() {
		setType(LINK_CHECK_REQ);
	}
}
