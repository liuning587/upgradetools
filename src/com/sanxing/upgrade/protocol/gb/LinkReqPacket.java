package com.sanxing.upgrade.protocol.gb;

public class LinkReqPacket extends GBPacket {
	private static final long serialVersionUID = -2595087593613350057L;

	public LinkReqPacket() {
		setType(LINK_CHECK_REQ);
	}
}
