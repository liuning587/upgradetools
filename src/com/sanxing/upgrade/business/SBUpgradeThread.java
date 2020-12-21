package com.sanxing.upgrade.business;

import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.protocol.Packet;
import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.protocol.sb.CancelUpgradeRespPacket;
import com.sanxing.upgrade.protocol.sb.CheckFileRespPacket;
import com.sanxing.upgrade.protocol.sb.QueryVersionRespPacket;
import com.sanxing.upgrade.protocol.sb.ResponseCode;
import com.sanxing.upgrade.protocol.sb.SBPacket;
import com.sanxing.upgrade.protocol.sb.SBPacketParser;
import com.sanxing.upgrade.protocol.sb.StartUpgradeRespPacket;
import com.sanxing.upgrade.protocol.sb.UpgradeOverPacket;
import com.sanxing.upgrade.util.SysUtils;
import java.util.Date;

class SBUpgradeThread extends UpgradeThread {
	private SBPacketParser parser = (SBPacketParser) UpgradeService.getInstance().getPacketParser();

	private float lastSendRate;

	public SBUpgradeThread(FepConnector owner) {
		super(owner);
	}

	void handleTimeout() {
		if (this.fileSendInfo.hasNextIndex()) {

			doSendFile(this.fileSendInfo.getNextIndex(), !this.fileSendInfo.hasNextIndex());
			float currentSendRate = this.fileSendInfo.sendRate();
			this.currentTask.setRcvRate(currentSendRate);

			if (this.fileSendInfo.hasNextIndex()) {
				this.timer.reset(this.currentTask.getEvents(), this.sendInterval);

				if ((currentSendRate - this.lastSendRate) > 0.01D) {
					this.lastSendRate = currentSendRate;

					this.service.taskChanged(this.currentTask);
				}
			} else {

				this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "文件发送完毕，稍后将继续查询接收情况"));

				this.timer.reset(this.currentTask.getEvents(), 30000);
				this.fileCheckInfo.start();

				this.service.taskChanged(this.currentTask);
			}
		} else if (!this.fileCheckInfo.isOverTiems()) {

			doCheckFile();

			this.timer.reset(this.currentTask.getEvents(), 15000);

			this.service.taskChanged(this.currentTask);
		} else if (this.fileSupplyInfo.hasNextIndex()) {

			doSupplyFile(this.fileSupplyInfo.getNextIndex(), !this.fileSupplyInfo.hasNextIndex());

			float currentSendRate = this.fileSupplyInfo.sendRate();
			this.currentTask.setRcvRate(currentSendRate);

			if (this.fileSupplyInfo.hasNextIndex()) {
				this.timer.reset(this.currentTask.getEvents(), this.sendInterval);

				if ((currentSendRate - this.lastSendRate) > 0.01D) {
					this.lastSendRate = currentSendRate;

					this.service.taskChanged(this.currentTask);
				}

			} else {

				this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "文件补发完毕，稍后将继续查询接收情况"));
				this.timer.reset(this.currentTask.getEvents(), 30000);

				this.fileCheckInfo.start();

				this.service.taskChanged(this.currentTask);
			}

		} else {

			this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "等待终端应答超时，任务结束处理"));

			this.currentTask.stop();

			this.service.taskChanged(this.currentTask);
		}
	}

	private void doQueryVersion() {
		this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "查询终端当前软件版本"));

		this.fepConnector
				.send((Packet) this.parser.packQueryVersionRequest(this.currentTask.getTerminalAddr(), this.msta));
	}

	private void doStartUpgrade() {
		this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "启动终端升级"));

		this.fepConnector.send((Packet) getUpgradeReqPacket(this.currentTask));
	}

	private void doSendFile(int index, boolean allowQuery) {
		this.fepConnector.send((Packet) getUpgradeDataReqPacket(this.currentTask, index, allowQuery));
	}

	private void doSupplyFile(int index, boolean allowQuery) {
		this.fepConnector.send((Packet) getUpgradeDataReqPacket(this.currentTask, index, allowQuery));
	}

	private void doCheckFile() {
		this.currentTask.addEvent(
				Event.create(EventType.STATE_CHANGED, "查询接收情况(" + (this.fileCheckInfo.currentQuerySeq() + 1) + ")"));

		this.fepConnector.send((Packet) getCheckFileReqPacket(this.currentTask));
	}

	private void doCancel() {
		this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "发送取消升级命令"));

		this.fepConnector.send((Packet) this.parser.packCancelUpgradeRequest(this.currentTask.getTerminalAddr(),
				getPassword(), this.msta));
	}

	void handleEvent(Event event) {
		event.done();

		if (EventType.CUSTOMER_BREAK == event.type()) {

			this.currentTask.stop();

			return;
		}
		int state = this.currentTask.getState();

		if (this.currentTask.isUpgrading()) {
			if ((state & 0x8) != 0) {

				if (EventType.START_TASK == event.type()) {

					if (this.allowQueryVersion
							&& this.currentTask.getOldVersion().compareTo(this.file.getVersion()) != 0) {

						doQueryVersion();

						this.timer.reset(this.currentTask.getEvents(), 30000);

					} else {

						this.currentTask.removeState(10);

						this.currentTask.getEvents().done();

						this.currentTask.addEvent(
								Event.create(EventType.STATE_CHANGED, "不允许查询版本号或升级前后版本相同，无法判断升级是否成功，升级任务将重新开始"));

						this.currentTask.addEvent(Event.createPending(EventType.START_TASK, "开始处理"));
					}
					return;
				}
				if (EventType.RECEIVE_RESPONSE == event.type()) {

					SBPacket packet = (SBPacket) this.parser.unpackReponse((Packet) event.attachment());
					if (packet.getType() == 16384) {
						ResponseCode code = ResponseCode.get(((UpgradeOverPacket) packet).getCode());

						if (ResponseCode.FINISH != code) {
							this.currentTask
									.addEvent(Event.create(EventType.STATE_CHANGED, "升级失败，终端返回：" + code.getRemark()));
						} else {
							this.currentTask.addState(16);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级完成"));
						}

						this.currentTask.stop();
						return;
					}
					if (packet.getType() == 1024) {

						String version = ((QueryVersionRespPacket) packet).getVersion();

						this.currentTask.setCurrentVersion(version);

						if (version.compareTo(this.file.getVersion()) == 0) {
							this.currentTask.addState(16);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级成功，版本已相符"));
						} else {

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端当前版本与目标版本不符，升级失败"));

							this.currentTask.removeState(10);
						}

						this.currentTask.stop();

						return;
					}
				}
			} else if ((state & 0x2) != 0) {

				if (EventType.START_TASK == event.type()) {

					this.fileCheckInfo.start();

					this.timer.clear();
					return;
				}
				if (EventType.RECEIVE_RESPONSE == event.type()) {

					SBPacket packet = (SBPacket) this.parser.unpackReponse((Packet) event.attachment());

					if (packet.getType() == 8192) {

						this.fileCheckInfo.check(((CheckFileRespPacket) packet).getPs());

						if (this.fileCheckInfo.isError()) {

							this.currentTask.addEvent(
									Event.create(EventType.STATE_CHANGED, "升级失败，终端可能无法接收大报文或者处理异常，请将分段长度设小后重试"));
							this.currentTask.stop();

							return;
						}

						if (this.fileCheckInfo.isCompletion()) {

							this.fileCheckInfo.clear();

							this.fileSupplyInfo.clear();

							this.currentTask.addState(8);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端已完整接收升级文件，等待升级结果报告"));

							this.timer.reset(this.currentTask.getEvents(), 90000);

							return;
						}

						if (this.fileCheckInfo.isStopReceive()) {

							this.fileSupplyInfo.start(this.fileCheckInfo.getLastPs());

							this.lastSendRate = 0.0F;

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED,
									"确认终端已停止接收，开始补发文件(" + this.fileSupplyInfo.count() + "包)"));

							this.fileCheckInfo.clear();

							this.timer.clear();

							return;
						}

						return;
					}
					if (packet.getType() == 16384) {
						ResponseCode code = ResponseCode.get(((UpgradeOverPacket) packet).getCode());

						if (ResponseCode.FINISH != code) {

							if (ResponseCode.ERROR_FILE_DELETED == code && this.allowQueryVersion
									&& this.currentTask.getOldVersion().compareTo(this.file.getVersion()) != 0) {

								this.fileCheckInfo.clear();

								this.fileSupplyInfo.clear();

								this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED,
										"终端返回：" + code.getRemark() + "，需要检查当前版本号"));

								doQueryVersion();

								this.timer.reset(this.currentTask.getEvents(), 30000);

								return;
							}

							this.currentTask
									.addEvent(Event.create(EventType.STATE_CHANGED, "升级失败，终端返回：" + code.getRemark()));
						} else {

							this.currentTask.addState(16);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级完成"));
						}
						this.currentTask.stop();
						return;
					}
					if (packet.getType() == 1024) {

						String version = ((QueryVersionRespPacket) packet).getVersion();

						this.currentTask.setCurrentVersion(version);

						if (version.compareTo(this.file.getVersion()) == 0) {
							this.currentTask.addState(16);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级成功，版本已相符"));
						} else {

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端当前版本与目标版本不符，升级失败"));

							this.currentTask.removeState(10);
						}

						this.currentTask.stop();
					}
				}
			} else if ((state & 0x1) != 0) {

				if (EventType.START_TASK == event.type()) {

					doStartUpgrade();

					this.timer.reset(this.currentTask.getEvents(), 60000);
					return;
				}
				if (EventType.RECEIVE_RESPONSE == event.type()) {

					SBPacket packet = (SBPacket) this.parser.unpackReponse((Packet) event.attachment());

					if (packet.getType() == 2048) {
						ResponseCode code = ResponseCode.get(((StartUpgradeRespPacket) packet).getCode());

						if (ResponseCode.FINISH != code) {

							this.currentTask
									.addEvent(Event.create(EventType.STATE_CHANGED, "启动升级失败，终端返回：" + code.getRemark()));

							if (this.autoCancel && code == ResponseCode.ERROR_STARTED) {

								doCancel();

								this.timer.reset(this.currentTask.getEvents(), 60000);
							} else {
								this.currentTask.stop();
							}

							return;
						}
						this.currentTask.addState(2);

						this.fileSendInfo.start();

						this.lastSendRate = 0.0F;

						this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED,
								"终端已启动升级，开始发送升级文件(" + this.fileSendInfo.count() + "包)"));

						this.timer.clear();
						return;
					}
					if (packet.getType() == 4096) {
						ResponseCode code = ResponseCode.get(((CancelUpgradeRespPacket) packet).getCode());

						if (ResponseCode.FINISH != code) {

							this.currentTask
									.addEvent(Event.create(EventType.STATE_CHANGED, "启动升级失败，终端返回：" + code.getRemark()));

							this.currentTask.stop();
						} else {

							doStartUpgrade();

							this.timer.reset(this.currentTask.getEvents(), 60000);
						}
						return;
					}
				}
			} else if ((state & 0xFFFFFBFF) == 0) {

				if (EventType.START_TASK == event.type()) {

					if (this.allowQueryVersion) {

						doQueryVersion();

						this.timer.reset(this.currentTask.getEvents(), 30000);
					} else {

						this.currentTask.addState(1);

						this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "不允许查询版本号"));

						doStartUpgrade();

						this.timer.reset(this.currentTask.getEvents(), 60000);
					}
					return;
				}
				if (EventType.RECEIVE_RESPONSE == event.type()) {

					SBPacket packet = (SBPacket) this.parser.unpackReponse((Packet) event.attachment());

					if (packet.getType() == 1024) {

						String version = ((QueryVersionRespPacket) packet).getVersion();

						this.currentTask.setOldVersion(version);

						this.currentTask.addState(1);

						this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端返回版本：" + version));

						if (this.skipNeedlessUpgrade) {
							this.file = this.service.getUpgradeFile();

							if (this.file.getVersion().compareTo(version) == 0) {

								this.currentTask.addState(16);

								this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "版本一致，不需要重复升级"));

								this.currentTask.stop();

								return;
							}
						}

						doStartUpgrade();

						this.timer.reset(this.currentTask.getEvents(), 60000);

						return;
					}
				}
			}
		} else if (this.currentTask.isCanceling()) {

			if (EventType.START_TASK == event.type()) {

				doCancel();

				this.timer.reset(this.currentTask.getEvents(), 60000);
				return;
			}
			if (EventType.RECEIVE_RESPONSE == event.type()) {

				SBPacket packet = (SBPacket) this.parser.unpackReponse((Packet) event.attachment());
				if (packet.getType() == 4096) {
					ResponseCode code = ResponseCode.get(((CancelUpgradeRespPacket) packet).getCode());

					if (ResponseCode.FINISH == code) {

						this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "取消当前升级成功"));
					} else {

						this.currentTask
								.addEvent(Event.create(EventType.STATE_CHANGED, "取消当前升级失败，终端返回：" + code.getRemark()));
					}

					this.currentTask.stop();
					return;
				}
			}
		}
	}

	private SBPacket getCheckFileReqPacket(Task task) {
		byte[] data = new byte[7];

		int p = 0;

		data[p++] = 41;

		data[p++] = 2;

		data[p++] = 0;
		data[p++] = Byte.MIN_VALUE;

		data[p++] = 0;
		data[p++] = 0;

		data[p++] = this.fileCheckInfo.nextQuerySeq();

		return this.parser.packRequest(this.currentTask.getTerminalAddr(), this.msta, (byte) 15, data,
				PacketParser.calcCs(data));
	}

	private SBPacket getUpgradeDataReqPacket(Task task, int index, boolean allowQuery) {
		byte[] section = this.file.getSections()[index - 1];
		byte[] data = new byte[7 + section.length];

		int p = 0;

		data[p++] = 41;

		data[p++] = 2;

		data[p++] = (byte) index;

		if (allowQuery) {
			data[p++] = (byte) (index >>> 8 | 0x80);
		} else {
			data[p++] = (byte) (index >>> 8);
		}

		data[p++] = (byte) section.length;
		data[p++] = (byte) (section.length >>> 8);

		data[p++] = 0;

		System.arraycopy(section, 0, data, p, section.length);

		byte cs = (byte) (this.parser.calcCs(data, 0, 7) + this.file.getCss()[index - 1]);

		return this.parser.packRequest(this.currentTask.getTerminalAddr(), this.msta, (byte) 15, data, cs);
	}

	private SBPacket getUpgradeReqPacket(Task task) {
		byte[] data = new byte[70];

		int p = 0;

		data[p++] = 41;

		data[p++] = 0;

		byte[] bytes = SysUtils.hexToBytes(SysUtils.reverseHex(getPassword()));
		System.arraycopy(bytes, 0, data, p, 3);
		p += 3;

		bytes = this.file.getName().getBytes();

		SysUtils.reverseBytes(bytes);

		System.arraycopy(bytes, 0, data, p + 25 - bytes.length, bytes.length);
		p += 25;

		data[p++] = (byte) this.file.getType().ordinal();

		data[p++] = (byte) this.file.getSize();
		data[p++] = (byte) (this.file.getSize() >>> 8);
		data[p++] = (byte) (this.file.getSize() >>> 16);
		data[p++] = (byte) (this.file.getSize() >>> 24);

		data[p++] = (byte) this.file.getZipSize();
		data[p++] = (byte) (this.file.getZipSize() >>> 8);
		data[p++] = (byte) (this.file.getZipSize() >>> 16);
		data[p++] = (byte) (this.file.getZipSize() >>> 24);

		data[p++] = (byte) (this.file.getZip() ? 1 : 0);

		bytes = SysUtils.hexToBytes(this.file.getVersion());

		SysUtils.reverseBytes(bytes);
		System.arraycopy(bytes, 0, data, p, 8);
		p += 8;

		bytes = SysUtils.hexToBytes(this.file.getZipMd5());

		SysUtils.reverseBytes(bytes);
		System.arraycopy(bytes, 0, data, p, 16);
		p += 16;

		data[p++] = (byte) (this.restartTerminal ? 1 : 0);

		data[p++] = (byte) this.restartDelay;

		int i = (this.file.getSections()).length;
		data[p++] = (byte) i;
		data[p++] = (byte) (i >>> 8);

		i = this.file.getSplitLength();
		data[p++] = (byte) i;
		data[p++] = (byte) (i >>> 8);

		byte cs = PacketParser.calcCs(data);

		return this.parser.packRequest(this.currentTask.getTerminalAddr(), this.msta, (byte) 15, data, cs);
	}

	private String getPassword() {
		if (this.dynamicPWD)
			return this.parser.getUpgradePassword(new Date());
		return this.upgradePassword;
	}
}
