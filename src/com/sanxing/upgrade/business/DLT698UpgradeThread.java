package com.sanxing.upgrade.business;

import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.EventType;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.protocol.Packet;
//import com.sanxing.upgrade.protocol.PacketParser;
import com.sanxing.upgrade.protocol.dlt698.CheckFileRespPacket;
import com.sanxing.upgrade.protocol.dlt698.ConfirmPacket;
import com.sanxing.upgrade.protocol.dlt698.DLT698Packet;
import com.sanxing.upgrade.protocol.dlt698.DLT698PacketParser;
import com.sanxing.upgrade.protocol.dlt698.QueryVersionRespPacket;
import com.sanxing.upgrade.protocol.dlt698.ResponseCode;
//import com.sanxing.upgrade.util.SysUtils;

class DLT698UpgradeThread extends UpgradeThread {
	private DLT698PacketParser parser = (DLT698PacketParser) UpgradeService.getInstance().getPacketParser();

	private float lastSendRate;

	private boolean autoCanceling;
	private boolean firstTask = true;

	public DLT698UpgradeThread(FepConnector owner) {
		super(owner);
	}

	void startTask() {
		this.autoCanceling = false;

		if (this.specialChannel) {
			if (this.firstTask) {
				this.firstTask = false;
			} else {
				this.fepConnector.resetChannel();
			}
		}

		super.startTask();
	}

	void handleTimeout() {
		if (this.fileSendInfo.hasNextIndex()) { // 1. 正常发送数据

			doSendFile(this.fileSendInfo.getNextIndex(), !this.fileSendInfo.hasNextIndex());
			float currentSendRate = this.fileSendInfo.sendRate();
			this.currentTask.setRcvRate(currentSendRate);

			if (this.fileSendInfo.hasNextIndex()) { // 有后续帧
				this.timer.reset(this.currentTask.getEvents(), this.sendInterval);

				if ((currentSendRate - this.lastSendRate) > 0.01D) {
					this.lastSendRate = currentSendRate;

					this.service.taskChanged(this.currentTask);
				}
			} else { // 最后一包

				this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "文件发送完毕，稍后将继续查询接收情况"));

				this.timer.reset(this.currentTask.getEvents(), 1000);
				this.fileCheckInfo.start();

				this.service.taskChanged(this.currentTask);
			}
		} else if (!this.fileCheckInfo.isOverTiems()) { // 2. 查询位图

			doCheckFile();

			this.timer.reset(this.currentTask.getEvents(), 10000);

			this.service.taskChanged(this.currentTask);
		} else if (this.fileSupplyInfo.hasNextIndex()) { // 3. 补包

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

				this.timer.reset(this.currentTask.getEvents(), 1000);

				this.fileCheckInfo.start();

				this.service.taskChanged(this.currentTask);
			}

		} else {

			this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "等待应答超时，任务结束处理"));

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

		DLT698Packet packet = getUpgradeReqPacket(this.currentTask);

		this.fepConnector.send((Packet) packet);
	}

	private void doSendFile(int index, boolean allowQuery) {
		this.fepConnector.send((Packet) getUpgradeDataReqPacket(this.currentTask, index, allowQuery));
	}

	private void doSupplyFile(int index, boolean allowQuery) {
		this.fepConnector.send((Packet) getUpgradeDataReqPacket(this.currentTask, index, allowQuery));
	}

	// 查询接收情况
	private void doCheckFile() {
		this.currentTask.addEvent(
				Event.create(EventType.STATE_CHANGED, "查询接收情况(" + (this.fileCheckInfo.currentQuerySeq() + 1) + ")"));

		this.fepConnector.send((Packet) getCheckFileReqPacket(this.currentTask));
	}

	private void doCheckUpgrade() {
		this.fepConnector.send((Packet) getCheckUpgradeReqPacket(this.currentTask));
	}

	private void doCancel() {
		this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "发送取消升级命令"));

		this.fepConnector.send((Packet) this.parser.packCancelUpgradeRequest(this.currentTask.getTerminalAddr(),
				this.upgradePassword, this.msta));
	}

	private void doResetSoft() {
		this.currentTask.setRemark("发送清除数据区命令");

		this.fepConnector.send((Packet) this.parser.packResetSoftRequest(this.currentTask.getTerminalAddr(),
				this.upgradePassword, this.msta));

		this.service.taskChanged(this.currentTask);
	}

	void handleEvent(Event event) {
		event.done();

		if (EventType.CUSTOMER_BREAK == event.type()) { // 用户取消
			this.currentTask.stop();
			return;
		}

		int state = this.currentTask.getState();

		if (this.currentTask.isUpgrading()) {
			if ((state & Task.STATE_CHECK_FILE) != 0) { // 检查升级文件

				if (EventType.START_TASK == event.type()) {

					if (this.allowQueryVersion
							&& this.currentTask.getOldVersion().compareTo(this.file.getVersion()) != 0) {

						doQueryVersion();

						this.timer.reset(this.currentTask.getEvents(), 60000);
					} else {
						this.currentTask.removeState(Task.STATE_CHECK_FILE | Task.STATE_START_UPGRADE);

						this.currentTask.getEvents().done();

						this.currentTask.addEvent(
								Event.create(EventType.STATE_CHANGED, "不允许查询版本号或升级前后版本相同，无法判断升级是否成功，升级任务将重新开始"));

						this.currentTask.addEvent(Event.createPending(EventType.START_TASK, "开始处理"));
					}
					return;
				}
				if (EventType.RECEIVE_RESPONSE == event.type()) {

					DLT698Packet packet = (DLT698Packet) this.parser.unpackReponse((Packet) event.attachment());
					if (packet.getType() == Packet.CONFIRM_RESP) {
						ResponseCode code = ResponseCode.get(((ConfirmPacket) packet).getState());

						if (ResponseCode.FINISH != code) {
							this.currentTask
									.addEvent(Event.create(EventType.STATE_CHANGED, "升级失败，终端返回：" + code.getRemark()));

							if (ResponseCode.ERROR_3 == code) {
								doResetSoft();
							}
							this.currentTask.removeState(Task.STATE_CHECK_FILE | Task.STATE_START_UPGRADE);

						} else if (this.affirmVersion) {
							if (this.restartFaultTask) {
								this.currentTask
										.addEvent(Event.create(EventType.STATE_CHANGED, "终端已回复确认，将在下一轮处理时核对版本号"));
							} else {
								this.currentTask
										.addEvent(Event.create(EventType.STATE_CHANGED, "终端已回复确认，请稍后重启任务以核对版本号"));
							}
						} else {
							this.currentTask.addState(Task.STATE_FINISH);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级完成"));
						}

						this.currentTask.stop();
						return;
					}
					if (packet.getType() == Packet.VERSION_RESP) {

						String version = ((QueryVersionRespPacket) packet).getVersion();

						this.currentTask.setCurrentVersion(version);

						if (version.compareTo(this.file.getVersion()) == 0) {
							this.currentTask.addState(Task.STATE_FINISH);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级成功，版本已相符"));
						} else if (this.skipLaterVersion && version.compareTo(this.file.getVersion()) > 0) {
							this.currentTask.addState(Task.STATE_FINISH);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级结束，终端当前版本大于目标版本"));
						} else {

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端当前版本与目标版本不符，升级失败"));

							this.currentTask.removeState(Task.STATE_CHECK_FILE | Task.STATE_START_UPGRADE);
						}

						this.currentTask.stop();

						return;
					}
				}
			} else if ((state & Task.STATE_START_UPGRADE) != 0) { // 正在升级

				if (EventType.START_TASK == event.type()) {

					this.fileCheckInfo.start();

					this.timer.clear();
					return;
				}
				if (EventType.RECEIVE_RESPONSE == event.type()) {

					DLT698Packet packet = (DLT698Packet) this.parser.unpackReponse((Packet) event.attachment());

					if (packet.getType() == Packet.CHECK_RCV_RESP) {

						this.fileCheckInfo.check(((CheckFileRespPacket) packet).getPs());

						if (this.fileCheckInfo.isNull()) {

							this.fileCheckInfo.clear();

							this.fileSupplyInfo.clear();

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端返回接收情况为空，需要检查当前版本号"));

							doQueryVersion();

							this.timer.reset(this.currentTask.getEvents(), 60000);
							return;
						}
						if (this.fileCheckInfo.isError()) {

							this.currentTask.addEvent(
									Event.create(EventType.STATE_CHANGED, "升级失败，终端可能无法接收大报文或者处理异常，请将分段长度改小后重试"));
							this.currentTask.stop();

							return;
						}

						if (this.fileCheckInfo.isCompletion()) {

							this.fileCheckInfo.clear();

							this.fileSupplyInfo.clear();

							this.currentTask.addState(Task.STATE_CHECK_FILE);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端已完整接收升级文件，查询校验结果"));

							doCheckUpgrade();

							this.timer.reset(this.currentTask.getEvents(), 60000);

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
					if (packet.getType() == Packet.CONFIRM_RESP) {
						ResponseCode code = ResponseCode.get(((ConfirmPacket) packet).getState());

						if (ResponseCode.FINISH == code) {

							if (this.affirmVersion)

							{
								if (this.restartFaultTask) {
									this.currentTask
											.addEvent(Event.create(EventType.STATE_CHANGED, "终端已回复确认，将在下一轮处理时核对版本号"));
								} else {
									this.currentTask
											.addEvent(Event.create(EventType.STATE_CHANGED, "终端已回复确认，请稍后重启任务以核对版本号"));
								}
							} else {
								this.currentTask.addState(Task.STATE_FINISH);

								this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级完成"));
							}

						} else {

							if (ResponseCode.ERROR_FILE_DELETED == code && this.allowQueryVersion
									&& this.currentTask.getOldVersion().compareTo(this.file.getVersion()) != 0) {

								this.fileCheckInfo.clear();

								this.fileSupplyInfo.clear();

								this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED,
										"终端返回：" + code.getRemark() + "，需要检查当前版本号"));

								doQueryVersion();

								this.timer.reset(this.currentTask.getEvents(), 60000);

								return;
							}

							this.currentTask
									.addEvent(Event.create(EventType.STATE_CHANGED, "升级失败，终端返回：" + code.getRemark()));
							if (ResponseCode.ERROR_3 == code) {
								doResetSoft();
							}
							this.currentTask.removeState(Task.STATE_CHECK_FILE | Task.STATE_START_UPGRADE);
						}
						this.currentTask.stop();
						return;
					}
					if (packet.getType() == Packet.VERSION_RESP) {

						String version = ((QueryVersionRespPacket) packet).getVersion();

						this.currentTask.setCurrentVersion(version);

						if (version.compareTo(this.file.getVersion()) == 0) {
							this.currentTask.addState(Task.STATE_FINISH);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级成功，版本已相符"));
						} else if (this.skipLaterVersion && version.compareTo(this.file.getVersion()) > 0) {
							this.currentTask.addState(Task.STATE_FINISH);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "升级结束，终端当前版本大于目标版本"));
						} else {

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端当前版本与目标版本不符，升级失败"));

							this.currentTask.removeState(Task.STATE_CHECK_FILE | Task.STATE_START_UPGRADE);
						}

						this.currentTask.stop();
					}
				}
			} else if ((state & Task.STATE_RETURN_VERSION) != 0) { //

				if (EventType.START_TASK == event.type()) {

					doStartUpgrade();
					this.autoCanceling = false;

					this.timer.reset(this.currentTask.getEvents(), 30000);
					return;
				}
				if (EventType.RECEIVE_RESPONSE == event.type()) {

					DLT698Packet packet = (DLT698Packet) this.parser.unpackReponse((Packet) event.attachment());

					if (packet.getType() == Packet.CONFIRM_RESP) {
						ResponseCode code = ResponseCode.get(((ConfirmPacket) packet).getState());
						if (!this.autoCanceling) {

							if (ResponseCode.FINISH != code) {

								if (ResponseCode.ERROR_3 == code) {

									this.currentTask.addEvent(
											Event.create(EventType.STATE_CHANGED, "升级失败，终端返回：" + code.getRemark()));

									doResetSoft();

									this.currentTask.removeState(Task.STATE_CHECK_FILE | Task.STATE_START_UPGRADE);

									this.currentTask.stop();

									return;
								}

								this.currentTask.addEvent(
										Event.create(EventType.STATE_CHANGED, "启动升级失败，终端返回：" + code.getRemark()));

								if (this.autoCancel && code == ResponseCode.ERROR_STARTED) {

									doCancel();

									this.autoCanceling = true;

									this.timer.reset(this.currentTask.getEvents(), 20000);
								} else {
									this.currentTask.stop();
								}

								return;
							}

							this.currentTask.addState(Task.STATE_START_UPGRADE);

							this.fileSendInfo.start();

							this.lastSendRate = 0.0F;

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED,
									"终端已启动升级，开始发送升级文件(" + this.fileSendInfo.count() + "包 " + this.file.getSplitLength() + "字节)"));

							this.timer.clear();

							return;
						}
						this.autoCanceling = false;

						if (ResponseCode.FINISH != code) {

							this.currentTask
									.addEvent(Event.create(EventType.STATE_CHANGED, "启动升级失败，终端返回：" + code.getRemark()));

							this.currentTask.stop();
						} else {

							doStartUpgrade();

							this.timer.reset(this.currentTask.getEvents(), 30000);
						}

					}
				}
			} else if ((state & ~Task.STATE_UPGRADING) == 0) {

				if (EventType.START_TASK == event.type()) {

					if (this.allowQueryVersion) {

						doQueryVersion();

						this.timer.reset(this.currentTask.getEvents(), 60000);
					} else {

						this.currentTask.addState(Task.STATE_RETURN_VERSION);

						this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "不允许查询版本号"));

						doStartUpgrade();

						this.timer.reset(this.currentTask.getEvents(), 30000);
					}
					return;
				}
				if (EventType.RECEIVE_RESPONSE == event.type()) {

					DLT698Packet packet = (DLT698Packet) this.parser.unpackReponse((Packet) event.attachment());

					if (packet.getType() == Packet.VERSION_RESP) {

						String version = ((QueryVersionRespPacket) packet).getVersion();

						this.currentTask.setOldVersion(version);

						this.currentTask.addState(Task.STATE_RETURN_VERSION);

						this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端返回版本：" + version));

						if (this.skipNeedlessUpgrade) {
							if (version.compareTo(this.file.getVersion()) == 0) {

								this.currentTask.addState(Task.STATE_FINISH);

								this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "版本一致，不需要重复升级"));

								this.currentTask.stop();

								return;
							}
						}

						if (this.skipLaterVersion && version.compareTo(this.file.getVersion()) > 0) {

							this.currentTask.addState(Task.STATE_FINISH);

							this.currentTask.addEvent(Event.create(EventType.STATE_CHANGED, "终端版本大于目标版本，结束升级"));

							this.currentTask.stop();

							return;
						}

						doStartUpgrade();

						this.timer.reset(this.currentTask.getEvents(), 30000);

						return;
					}
				}
			}
		} else if (this.currentTask.isCanceling()) {

			if (EventType.START_TASK == event.type()) {

				doCancel();

				this.timer.reset(this.currentTask.getEvents(), 20000);
				return;
			}
			if (EventType.RECEIVE_RESPONSE == event.type()) {

				DLT698Packet packet = (DLT698Packet) this.parser.unpackReponse((Packet) event.attachment());
				if (packet.getType() == Packet.CONFIRM_RESP) {
					ResponseCode code = ResponseCode.get(((ConfirmPacket) packet).getState());

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

	// 创建查询接收位图请求报文
	private DLT698Packet getCheckFileReqPacket(Task task) {
		this.fileCheckInfo.nextQuerySeq();
		return this.parser.packCheckFileRequest(this.currentTask.getTerminalAddr(), this.msta);
	}

	// 创建检查升级结果报文
	private DLT698Packet getCheckUpgradeReqPacket(Task task) {
		return this.parser.packCheckUpgradeRequest(this.currentTask.getTerminalAddr(), this.msta);
	}

	// 创建数据传输报文
	private DLT698Packet getUpgradeDataReqPacket(Task task, int index, boolean allowQuery) {
		byte[] section = this.file.getSections()[index - 1];
		return this.parser.packUpgradeDataRequest(this.currentTask.getTerminalAddr(), this.msta, index - 1, section,
				allowQuery);
	}

	// 创建启动升级报文
	private DLT698Packet getUpgradeReqPacket(Task task) {
		// this.file.getType()
		return this.parser.packStartUpgradeRequest(this.currentTask.getTerminalAddr(), this.msta, this.file.getSize(),
				this.file.getSplitLength(), 0);
	}
}
