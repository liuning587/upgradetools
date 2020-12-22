package com.sanxing.upgrade.core;

public enum ProtocolType {
	GB("国标(130-2005)") {
	},
	GB09("国标(376.1-2009)") {
	},
	SB("省标") {
	},
	DLT698("DLT698.45") {
	};
	private String remark;

	ProtocolType(String remark) {
		this.remark = remark;
	}

	public String getRemark() {
		return this.remark;
	}

	public static String[] getRemarks() {
		ProtocolType[] values = values();
		String[] remarks = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			remarks[i] = values[i].getRemark();
		}
		return remarks;
	}

	public static ProtocolType getByRemark(String remark) {
		ProtocolType[] values = values();
		for (int i = 0; i < values.length; i++) {
			if (values[i].getRemark().equals(remark))
				return values[i];
		}
		return null;
	}
}
