package com.sanxing.upgrade.protocol.gb;

public enum ResponseCode {
	FINISH("成功") {
	},
	ERROR_STARTED("正在升级，不得重复启动") {
	},
	ERROR_2("密码权限不足") {
	},
	ERROR_3("磁盘空间不足") {
	},
	ERROR_4("文件名无效") {
	},
	ERROR_5("文件类型错误") {
	},
	ERROR_6("文件长度不对") {
	},
	ERROR_7("帧长度错误") {
	},
	ERROR_8("帧总数错误") {
	},
	ERROR_9("总帧数与文件大小不符") {
	},
	ERROR_10("MD5码为0") {
	},
	ERROR_11("版本号为0") {
	},
	ERROR_12("读取文件失败") {
	},
	ERROR_13("验证MD5错误") {
	},
	ERROR_14("解压缩失败") {
	},
	ERROR_15("文件非法") {
	},
	ERROR_CANCEL("用户取消升级") {
	},
	ERROR_17("写文件失败") {
	},
	ERROR_FILE_DELETED("升级参数文件丢失") {
	},
	ERROR_19("帧序号超标") {
	},
	ERROR_UNKNOW("未知错误") {
	};
	private String remark;

	ResponseCode(String remark) {
		this.remark = remark;
	}

	public String getRemark() {
		return this.remark;
	}

	public static String[] getRemarks() {
		ResponseCode[] values = values();
		String[] remarks = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			remarks[i] = values[i].getRemark();
		}
		return remarks;
	}

	public static ResponseCode getByRemark(String remark) {
		ResponseCode[] values = values();
		for (int i = 0; i < values.length; i++) {
			if (values[i].getRemark().equals(remark))
				return values[i];
		}
		return null;
	}

	public static ResponseCode get(int ordinal) {
		if (ordinal < 0 || (values()).length < ordinal) {
			return ERROR_UNKNOW;
		}
		return values()[ordinal];
	}
}
