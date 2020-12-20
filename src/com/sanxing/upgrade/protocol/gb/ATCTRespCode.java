package com.sanxing.upgrade.protocol.gb;

public enum ATCTRespCode {
	OK("正确") {
	},
	ERROR_OFFLINE("终端不在线") {
	},
	ERROR_BUSY("通道繁忙，稍候再试") {
	},
	ERROR_NOALLOW("升级未被许可") {
	},
	ERROR_UNKNOW("未知错误") {

	};
	private String remark;

	ATCTRespCode(String remark) {
		this.remark = remark;
	}

	public String getRemark() {
		return this.remark;
	}

	public static String[] getRemarks() {
		ATCTRespCode[] values = values();
		String[] remarks = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			remarks[i] = values[i].getRemark();
		}
		return remarks;
	}

	public static ATCTRespCode getByRemark(String remark) {
		ATCTRespCode[] values = values();
		for (int i = 0; i < values.length; i++) {
			if (values[i].getRemark().equals(remark))
				return values[i];
		}
		return null;
	}

	public static ATCTRespCode get(int ordinal) {
		if (ordinal < 0 || (values()).length <= ordinal) {
			return ERROR_UNKNOW;
		}
		return values()[ordinal];
	}
}
