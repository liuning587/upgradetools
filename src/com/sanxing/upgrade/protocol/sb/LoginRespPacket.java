package com.sanxing.upgrade.protocol.sb;

public class LoginRespPacket extends SBPacket {
	private static final long serialVersionUID = -2595087593613350057L;

	public LoginRespPacket() {
		setType(LOGIN_RESP);
	}
}
