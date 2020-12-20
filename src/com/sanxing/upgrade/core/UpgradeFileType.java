package com.sanxing.upgrade.core;

public enum UpgradeFileType {
	TYPE0("0-负控终端主文件") {
	},
	TYPE1("1-配变终端主文件") {
	},
	TYPE2("2-规约库文件") {
	},
	TYPE3("3-负控终端参数文件") {
	},
	TYPE4("4-配变终端参数文件") {
	},
	TYPE5("5-字库文件asc8") {
	},
	TYPE6("6-字库文件asc12") {
	},
	TYPE7("7-字库文件asc16") {
	},
	TYPE8("8-字库文件hzk12") {
	},
	TYPE9("9-字库文件hzk16") {
	};
	private String remark;

	UpgradeFileType(String remark) {
		this.remark = remark;
	}

	public String getRemark() {
		return this.remark;
	}

	public static String[] getRemarks() {
		UpgradeFileType[] values = values();
		String[] remarks = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			remarks[i] = values[i].getRemark();
		}
		return remarks;
	}

	public static UpgradeFileType getByRemark(String remark) {
		UpgradeFileType[] values = values();
		for (int i = 0; i < values.length; i++) {
			if (values[i].getRemark().equals(remark))
				return values[i];
		}
		return null;
	}
}
