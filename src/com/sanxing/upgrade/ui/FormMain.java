package com.sanxing.upgrade.ui;

import com.sanxing.upgrade.business.UpgradeService;
import com.sanxing.upgrade.core.Event;
import com.sanxing.upgrade.core.ITaskChangedListener;
import com.sanxing.upgrade.core.ProtocolType;
import com.sanxing.upgrade.core.Queue;
import com.sanxing.upgrade.core.Task;
import com.sanxing.upgrade.core.TaskList;
import com.sanxing.upgrade.core.TasksStatInfo;
import com.sanxing.upgrade.core.UpgradeFileType;
import com.sanxing.upgrade.util.Logger;
import com.sanxing.upgrade.util.Resources;
import com.sanxing.upgrade.util.StructuredContentProviderAdapter;
import com.sanxing.upgrade.util.SysUtils;
import com.sanxing.upgrade.util.TableLabelProviderAdapter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class FormMain extends Shell {
	private UpgradeService upgradeService = UpgradeService.getInstance();

	private Queue<Task> refreshQueue = new Queue<Task>();

	private CTabFolder folder;

	private ToolItem btnConnect;

	private ToolItem btnStartAll;

	private ToolItem btnDisconnect;

	private ToolItem btnClear;

	private ToolItem btnApply;

	private MenuItem miStart;

	private MenuItem miBreak;

	private MenuItem miCancel;

	private MenuItem miDelete;

	private MenuItem miClearDone;

	private MenuItem miSprAddr;

	private MenuItem miDecAddr;

	private TableViewer tvTask;

	private CTabFolder folderBottom;

	private StyledText textSysInfo;

	private TaskEventListViewer textTaskInfo;

	private Combo cbProtocolType;

	private Text textHostname;

	private Text textPort;

	private Text textMSTA;

	private Label lbMSTA;

	private Button btnAllowLoginFeps;

	private Text textPassword;

	private Text textHeartbeatInterval;

	private Button btnDynamicPWD;

	private Text textUpgradePassword;

	private Button btnSpecialChannel;

	private Text textMaxTaskCount;

	private Text textSendInterval;

	private Button btnAutoCancel;

	private Button btnAllowQueryVersion;

	private Button btnSkipNeedlessUpgrade;

	private Button btnSkipLaterVersion;

	private Button btnAffirmVersion;

	private Button btnRestartTerminalFaultTask;

	private Text textFilename;

	private Button btnSelectFile;

	private Combo cbFileType;

	private Text textFileVersion;

	private Label lbFileVersion;

	private Text textSplitLength;

	private Button btnZip;

	private Button btnRestartTerminal;

	private Text textRestartTerminalDelay;

	public FormMain(Display display) {
		super(display, 1264);
		createContents();
	}

	protected void createContents() {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 2;
		layout.marginTop = 2;
		layout.marginBottom = 0;
		setLayout((Layout) layout);

		setText("终端升级工具");

		setSize(600, 500);
		setImage(Resources.getImage("IMAGE_APPLICATION"));

		createMenu();

		this.folder = new CTabFolder((Composite) this, 2048);
		this.folder.setSimple(false);
		this.folder.setUnselectedImageVisible(true);
		this.folder.setUnselectedCloseVisible(true);
		this.folder.setSelectionBackground(Resources.getSystemColor(35));
		this.folder.setTabHeight(22);
		this.folder.setLayoutData(new GridData(1808));

		createUpgradeTabItem();

		createConfigurationTabItem();

		this.folder.setSelection(0);

		this.tvTask.getTable().forceFocus();

		addShellListener((ShellListener) new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				if (!FormMain.this.upgradeService.isClosed()) {
					int count = (FormMain.this.upgradeService.getTasks().getStatInfo()).runningCount;
					if (count > 0) {
						if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意",
								"还有" + count + "个任务正在运行中，是否确定结束处理？")) {
							e.doit = false;
							return;
						}
					}
					FormMain.this.upgradeService.disconnect();
				}
			}
		});

		Logger.init(this.textSysInfo);

		setMaximized(true);
	}

	private void checkConfiguration() throws Exception {
		ProtocolType type = ProtocolType.getByRemark(this.cbProtocolType.getText());

		if (!SysUtils.isIP(this.textHostname.getText().trim())) {
			this.textHostname.forceFocus();
			throw new Exception("请正确输入前置机地址。");
		}

		if (!SysUtils.isInteger(this.textPort.getText().trim())) {
			this.textPort.forceFocus();
			throw new Exception("请正确输入端口。");
		}

		if (!this.textMSTA.getText().trim().matches("\\d+")) {
			this.textMSTA.forceFocus();
			throw new Exception("请正确输入主站编号。");
		}

		int i = Integer.valueOf(this.textMSTA.getText().trim()).intValue();
		if (i < 1 || (type != ProtocolType.SB && i > 127) || (type == ProtocolType.SB && i > 63)) {
			this.textMSTA.forceFocus();
			throw new Exception("请正确输入主站编号。");
		}

		boolean allowLogin = this.btnAllowLoginFeps.getSelection();

		if (allowLogin && ProtocolType.SB == type && !this.textPassword.getText().trim().matches("\\d{6}")) {
			this.textPassword.forceFocus();
			throw new Exception("请正确输入登录密码。");
		}

		if (allowLogin && !this.textHeartbeatInterval.getText().trim().matches("\\d+")) {
			this.textHeartbeatInterval.forceFocus();
			throw new Exception("请正确输入心跳间隔。");
		}

		if (ProtocolType.SB == type && !this.textUpgradePassword.getText().trim().matches("\\d{6}")) {
			this.textUpgradePassword.forceFocus();
			throw new Exception("请正确输入终端升级密码。");
		}

		if (!SysUtils.isInteger(this.textMaxTaskCount.getText().trim())) {
			this.textMaxTaskCount.forceFocus();
			throw new Exception("请正确输入同时升级终端数。");
		}
		i = Integer.valueOf(this.textMaxTaskCount.getText().trim()).intValue();
		if (1 > i || 999 < i) {
			this.textMaxTaskCount.forceFocus();
			throw new Exception("请正确输入同时升级终端数。");
		}

		if (!SysUtils.isInteger(this.textSendInterval.getText().trim())) {
			this.textSendInterval.forceFocus();
			throw new Exception("请正确输入升级文件发送间隔。");
		}

		if (this.textFilename.getText().trim().isEmpty()) {
			this.btnSelectFile.forceFocus();
			throw new Exception("请选择目标文件。");
		}
		File file = new File(this.textFilename.getText().trim());
		if (!file.isFile()) {
			this.btnSelectFile.forceFocus();
			throw new Exception("目标文件不存在，请重新选择。");
		}

		String[] strs = this.textFilename.getText().trim().split("\\\\");
		if (25 < strs[strs.length - 1].length()) {
			this.btnSelectFile.forceFocus();
			throw new Exception("目标文件名长度不能超出25个字符。");
		}

		if (-1 == this.cbFileType.getSelectionIndex()) {
			this.cbFileType.forceFocus();
			throw new Exception("请选择文件类型。");
		}

		if ((ProtocolType.SB != type && (this.textFileVersion.getText().trim().isEmpty()
				|| this.textFileVersion.getText().trim().length() > 4))
				|| (ProtocolType.SB == type && !this.textFileVersion.getText().trim().matches("\\d{16}"))) {
			this.textFileVersion.forceFocus();
			throw new Exception("请正确输入目标版本。");
		}

		if (!SysUtils.isInteger(this.textSplitLength.getText().trim())) {
			this.textSplitLength.forceFocus();
			throw new Exception("请正确输入分包长度。");
		}
		i = Integer.valueOf(this.textSplitLength.getText().trim()).intValue();
		if (200 > i || 2048 < i) {
			this.textSplitLength.forceFocus();
			throw new Exception("请正确输入分包长度。");
		}

		if (this.btnRestartTerminal.getSelection()) {
			if (!SysUtils.isInteger(this.textRestartTerminalDelay.getText().trim())) {
				this.textRestartTerminalDelay.forceFocus();
				throw new Exception("请正确输入重启延时时间。");
			}
			i = Integer.valueOf(this.textRestartTerminalDelay.getText().trim()).intValue();
			if (5 > i || 255 < i) {
				this.textRestartTerminalDelay.forceFocus();
				throw new Exception("请正确输入重启延时时间。");
			}
		}
	}

	private void saveConfiguration() {
		ProtocolType type = ProtocolType.getByRemark(this.cbProtocolType.getText());
		Resources.setProperty("PROP_PROTOCOL_TYPE", String.valueOf(this.cbProtocolType.getSelectionIndex()));

		Resources.setProperty("PROP_HOSTNAME", this.textHostname.getText().trim());

		Resources.setProperty("PROP_PORT", this.textPort.getText().trim());

		Resources.setProperty("PROP_MSTA", this.textMSTA.getText().trim());

		boolean allowLogin = this.btnAllowLoginFeps.getSelection();
		Resources.setProperty("PROP_ALLOW_LOGIN_FEPS", String.valueOf(allowLogin));

		if (allowLogin) {

			if (type == ProtocolType.SB) {
				Resources.setProperty("PROP_PASSWORD", this.textPassword.getText().trim());
			}

			Resources.setProperty("PROP_HEARTBEAT_INTERVAL", this.textHeartbeatInterval.getText().trim());
		}

		if (type == ProtocolType.SB) {

			Resources.setProperty("PROP_DYNAMIC_PWD", String.valueOf(this.btnDynamicPWD.getSelection()));

			Resources.setProperty("PROP_UPGRADE_PASSWORD", this.textUpgradePassword.getText().trim());
		}

		Resources.setProperty("PROP_SPECIAL_CHANNEL", String.valueOf(this.btnSpecialChannel.getSelection()));

		Resources.setProperty("PROP_MAX_TASK_COUNT", this.textMaxTaskCount.getText().trim());

		Resources.setProperty("PROP_SEND_INTERVAL", this.textSendInterval.getText().trim());

		Resources.setProperty("PROP_AUTO_CANCEL", String.valueOf(this.btnAutoCancel.getSelection()));

		Resources.setProperty("PROP_ALLOW_QUERY_VERSION", String.valueOf(this.btnAllowQueryVersion.getSelection()));

		Resources.setProperty("PROP_SKIP_NEEDLESS_UPGRADE", String.valueOf(this.btnSkipNeedlessUpgrade.getSelection()));

		Resources.setProperty("PROP_SKIP_LATER_VERSION", String.valueOf(this.btnSkipLaterVersion.getSelection()));

		Resources.setProperty("PROP_AFFIRM_VERSION", String.valueOf(this.btnAffirmVersion.getSelection()));

		Resources.setProperty("PROP_RESTART_FAULT_TASK",
				String.valueOf(this.btnRestartTerminalFaultTask.getSelection()));

		Resources.setProperty("PROP_FILENAME", this.textFilename.getText().trim());

		Resources.setProperty("PROP_FILE_TYPE", String.valueOf(this.cbFileType.getSelectionIndex()));

		Resources.setProperty("PROP_FILE_VERSION", this.textFileVersion.getText().trim());

		Resources.setProperty("PROP_SPLIT_LENGTH", this.textSplitLength.getText().trim());

		Resources.setProperty("PROP_ZIP", String.valueOf(this.btnZip.getSelection()));

		Resources.setProperty("PROP_RESTART_TERMINAL", String.valueOf(this.btnRestartTerminal.getSelection()));

		if (this.btnRestartTerminal.getSelection()) {
			Resources.setProperty("PROP_RESTART_TERMINAL_DELAY", this.textRestartTerminalDelay.getText().trim());
		}

		try {
			Resources.saveCustomProperty();
		} catch (Exception e) {
			ErrorDialog.openError(null, "注意", "操作失败", (IStatus) new Status(4, "uptools", "无法保存参数", e), 4);
			return;
		}
	}

	private void showTaskInfo(Task task) {
		this.textTaskInfo.setInput(task);
	}

	private void createConfigurationTabItem() {
		CTabItem tabItem = new CTabItem(this.folder, 0);
		tabItem.setText("参数设置");
		Composite parent = new Composite((Composite) this.folder, 0);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 2;
		layout.marginTop = 2;
		layout.marginBottom = 0;

		parent.setLayout((Layout) layout);
		tabItem.setControl((Control) parent);

		ToolBar toolBar = new ToolBar(parent, 8519680);
		toolBar.setLayoutData(new GridData(256));

		this.btnApply = new ToolItem(toolBar, 8);
		this.btnApply.setText("应用");
		this.btnApply.setImage(Resources.getImage("IMAGE_SAVE"));
		this.btnApply.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					FormMain.this.checkConfiguration();
				} catch (Exception e1) {
					MessageDialog.openWarning(FormMain.this.getShell(), "注意", e1.getMessage());

					return;
				}
				if (!FormMain.this.upgradeService.isClosed()) {
					if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意", "应用参数前需要先关闭与前置机的连接，是否关闭并应用？"))
						return;
					if (!FormMain.this.disconnect()) {
						return;
					}
				} else if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意", "是否应用当前参数？")) {
					return;
				}

				FormMain.this.saveConfiguration();
			}
		});

		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, 2816);
		scrolledComposite.setLayoutData(new GridData(1808));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		Composite composite = new Composite((Composite) scrolledComposite, 0);
		scrolledComposite.setContent((Control) composite);

		composite.setLayoutData(new GridData(1808));
		layout = new GridLayout(3, false);
		layout.marginLeft = 3;
		layout.marginRight = 3;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 8;
		layout.marginTop = 5;
		layout.marginBottom = 5;

		composite.setLayout((Layout) layout);

		Label label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("规约类型");

		this.cbProtocolType = new Combo(composite, 2056);
		this.cbProtocolType.setLayoutData(new GridData(768));
		this.cbProtocolType.setVisibleItemCount(10);
		this.cbProtocolType.setItems(ProtocolType.getRemarks());
		this.cbProtocolType.select(Integer.valueOf(Resources.getProperty("PROP_PROTOCOL_TYPE")).intValue());
		this.cbProtocolType.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.changeUIState();
			}
		});

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));

		label = new Label(composite, 258);
		GridData gridData = new GridData(800);

		gridData.horizontalSpan = 3;
		label.setLayoutData(gridData);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("前置机地址");

		this.textHostname = new Text(composite, 2048);
		this.textHostname.setLayoutData(new GridData(768));
		this.textHostname.setText(Resources.getProperty("PROP_HOSTNAME"));
		this.textHostname.setTextLimit(20);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("端口");

		this.textPort = new Text(composite, 2048);
		this.textPort.setLayoutData(new GridData(768));
		this.textPort.setText(Resources.getProperty("PROP_PORT"));
		this.textPort.setTextLimit(6);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("主站编号");

		this.textMSTA = new Text(composite, 2048);
		this.textMSTA.setLayoutData(new GridData(768));
		this.textMSTA.setText(Resources.getProperty("PROP_MSTA"));

		this.lbMSTA = new Label(composite, 0);
		this.lbMSTA.setLayoutData(new GridData(32));

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		this.btnAllowLoginFeps = new Button(composite, 32);
		this.btnAllowLoginFeps.setText("需要升级工具登录前置机并维持心跳");
		gridData = new GridData(32);
		gridData.horizontalSpan = 2;
		this.btnAllowLoginFeps.setLayoutData(gridData);
		this.btnAllowLoginFeps
				.setSelection(Boolean.valueOf(Resources.getProperty("PROP_ALLOW_LOGIN_FEPS")).booleanValue());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("登录密码");

		this.textPassword = new Text(composite, 2048);
		this.textPassword.setLayoutData(new GridData(768));
		this.textPassword.setText(Resources.getProperty("PROP_PASSWORD"));
		this.textPassword.setTextLimit(6);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));
		label.setText("(6位数字)");

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("心跳间隔");

		this.textHeartbeatInterval = new Text(composite, 2048);
		this.textHeartbeatInterval.setLayoutData(new GridData(768));
		this.textHeartbeatInterval.setText(Resources.getProperty("PROP_HEARTBEAT_INTERVAL"));
		this.textHeartbeatInterval.setTextLimit(4);
		this.textHeartbeatInterval.setEditable(this.btnAllowLoginFeps.getSelection());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));
		label.setText("秒");

		this.btnAllowLoginFeps.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.changeUIState();
			}
		});

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		this.btnDynamicPWD = new Button(composite, 32);
		this.btnDynamicPWD.setText("动态升级密码(按日期自动变换，请确认当前计算机的日期是否正确)");
		gridData = new GridData(32);
		gridData.horizontalSpan = 2;
		this.btnDynamicPWD.setLayoutData(gridData);
		this.btnDynamicPWD.setSelection(Boolean.valueOf(Resources.getProperty("PROP_DYNAMIC_PWD")).booleanValue());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("升级密码");

		this.textUpgradePassword = new Text(composite, 2048);
		this.textUpgradePassword.setLayoutData(new GridData(768));
		this.textUpgradePassword.setEditable(!this.btnDynamicPWD.getSelection());
		this.textUpgradePassword.setText(Resources.getProperty("PROP_UPGRADE_PASSWORD"));
		this.textUpgradePassword.setTextLimit(6);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));
		label.setText("(6位数字)");

		this.btnDynamicPWD.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.changeUIState();
			}
		});

		label = new Label(composite, 258);
		gridData = new GridData(800);

		gridData.horizontalSpan = 3;
		label.setLayoutData(gridData);

		CLabel clabel = new CLabel(composite, 0);
		clabel.setImage(Resources.getImage("IMAGE_WARNING_SMALL"));
		gridData = new GridData(800);

		gridData.horizontalSpan = 3;
		clabel.setLayoutData(gridData);
		clabel.setFont(Resources.getBoldFont("FONT_DEFAULT"));
		clabel.setText("修改以下参数将影响升级性能，请根据运行环境情况谨慎修改");

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("同时升级终端数");

		this.textMaxTaskCount = new Text(composite, 2048);
		this.textMaxTaskCount.setLayoutData(new GridData(768));
		this.textMaxTaskCount.setText(Resources.getProperty("PROP_MAX_TASK_COUNT"));
		this.textMaxTaskCount.setTextLimit(3);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));
		label.setText("(1-999)");

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("文件发送间隔");

		this.textSendInterval = new Text(composite, 2048);
		this.textSendInterval.setLayoutData(new GridData(768));
		this.textSendInterval.setText(Resources.getProperty("PROP_SEND_INTERVAL"));
		this.textSendInterval.setTextLimit(4);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));
		label.setText("毫秒(0-9999)");

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		this.btnAutoCancel = new Button(composite, 32);
		this.btnAutoCancel.setText("启动升级时，如果终端返回“正在升级”，则先取消再启动");
		gridData = new GridData(32);
		gridData.horizontalSpan = 2;
		this.btnAutoCancel.setLayoutData(gridData);

		this.btnAutoCancel.setSelection(Boolean.valueOf(Resources.getProperty("PROP_AUTO_CANCEL")).booleanValue());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		this.btnAllowQueryVersion = new Button(composite, 32);
		this.btnAllowQueryVersion.setText("允许查询终端软件版本");
		gridData = new GridData(32);
		gridData.horizontalSpan = 2;
		this.btnAllowQueryVersion.setLayoutData(gridData);
		this.btnAllowQueryVersion
				.setSelection(Boolean.valueOf(Resources.getProperty("PROP_ALLOW_QUERY_VERSION")).booleanValue());
		this.btnAllowQueryVersion.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.changeUIState();
			}
		});

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		Composite cpVerOpt = new Composite(composite, 2048);
		gridData = new GridData(256);
		cpVerOpt.setLayoutData(gridData);
		layout = new GridLayout();
		layout.marginTop = 0;
		layout.marginBottom = 0;
		cpVerOpt.setLayout((Layout) layout);

		this.btnSkipNeedlessUpgrade = new Button(cpVerOpt, 32);
		this.btnSkipNeedlessUpgrade.setEnabled(this.btnAllowQueryVersion.getSelection());
		this.btnSkipNeedlessUpgrade.setText("终端当前版本与目标版本相同时不做升级");
		gridData = new GridData(32);
		this.btnSkipNeedlessUpgrade.setLayoutData(gridData);
		this.btnSkipNeedlessUpgrade
				.setSelection(Boolean.valueOf(Resources.getProperty("PROP_SKIP_NEEDLESS_UPGRADE")).booleanValue());

		this.btnSkipLaterVersion = new Button(cpVerOpt, 32);
		this.btnSkipLaterVersion.setEnabled(this.btnAllowQueryVersion.getSelection());
		this.btnSkipLaterVersion.setText("终端当前版本大于目标版本时不做升级");
		gridData = new GridData(32);
		this.btnSkipLaterVersion.setLayoutData(gridData);
		this.btnSkipLaterVersion
				.setSelection(Boolean.valueOf(Resources.getProperty("PROP_SKIP_LATER_VERSION")).booleanValue());

		this.btnAffirmVersion = new Button(cpVerOpt, 32);
		this.btnAffirmVersion.setEnabled(this.btnAllowQueryVersion.getSelection());
		this.btnAffirmVersion.setText("升级完成后检查版本号，注意：需等待下一轮处理(允许空闲重试时)，或手动重启任务");
		gridData = new GridData(32);
		this.btnAffirmVersion.setLayoutData(gridData);
		this.btnAffirmVersion
				.setSelection(Boolean.valueOf(Resources.getProperty("PROP_AFFIRM_VERSION")).booleanValue());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		this.btnRestartTerminalFaultTask = new Button(composite, 32);
		this.btnRestartTerminalFaultTask.setText("空闲时重试失败的任务");
		gridData = new GridData(32);
		gridData.horizontalSpan = 2;
		this.btnRestartTerminalFaultTask.setLayoutData(gridData);
		this.btnRestartTerminalFaultTask
				.setSelection(Boolean.valueOf(Resources.getProperty("PROP_RESTART_FAULT_TASK")).booleanValue());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		this.btnSpecialChannel = new Button(composite, 32);
		this.btnSpecialChannel.setText("为每个终端建立专用通道(效率低)");
		gridData = new GridData(32);
		gridData.horizontalSpan = 2;
		this.btnSpecialChannel.setLayoutData(gridData);
		this.btnSpecialChannel
				.setSelection(Boolean.valueOf(Resources.getProperty("PROP_SPECIAL_CHANNEL")).booleanValue());

		label = new Label(composite, 258);
		gridData = new GridData(800);

		gridData.horizontalSpan = 3;
		label.setLayoutData(gridData);

		clabel = new CLabel(composite, 0);
		clabel.setImage(Resources.getImage("IMAGE_ERROR_SMALL"));
		gridData = new GridData(800);

		gridData.horizontalSpan = 3;
		clabel.setLayoutData(gridData);
		clabel.setFont(Resources.getBoldFont("FONT_DEFAULT"));
		clabel.setText("修改以下参数，将导致目前未完成的任务重新开始升级");

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("目标文件");

		this.textFilename = new Text(composite, 2048);
		this.textFilename.setLayoutData(new GridData(768));
		this.textFilename.setEditable(false);
		this.textFilename.setText(Resources.getProperty("PROP_FILENAME"));

		this.btnSelectFile = new Button(composite, 8);
		gridData = new GridData(32);
		gridData.widthHint = 120;
		this.btnSelectFile.setLayoutData(gridData);
		this.btnSelectFile.setText("选择文件 ...");
		this.btnSelectFile.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(FormMain.this.getShell(), 4096);

				dialog.setFilterPath(System.getProperty("user.dir"));

				dialog.setFilterExtensions(new String[] { "*.*" });
				dialog.setFilterNames(new String[] { "升级文件(*.*)" });

				String filename = dialog.open();
				if (filename != null) {
					FormMain.this.textFilename.setText(filename);
				}
			}
		});

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		this.btnZip = new Button(composite, 32);
		this.btnZip.setText("终端支持文件解压缩");
		gridData = new GridData(32);
		gridData.horizontalSpan = 2;
		this.btnZip.setLayoutData(gridData);
		this.btnZip.setSelection(Boolean.valueOf(Resources.getProperty("PROP_ZIP")).booleanValue());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("文件类型");

		this.cbFileType = new Combo(composite, 2056);
		this.cbFileType.setLayoutData(new GridData(768));
		this.cbFileType.setVisibleItemCount(10);
		this.cbFileType.setItems(UpgradeFileType.getRemarks());
		this.cbFileType.select(Integer.valueOf(Resources.getProperty("PROP_FILE_TYPE")).intValue());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("目标版本");

		this.textFileVersion = new Text(composite, 2048);
		this.textFileVersion.setLayoutData(new GridData(768));
		this.textFileVersion.setText(Resources.getProperty("PROP_FILE_VERSION"));

		this.lbFileVersion = new Label(composite, 0);
		this.lbFileVersion.setLayoutData(new GridData(32));

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("分包长度");

		this.textSplitLength = new Text(composite, 2048);
		this.textSplitLength.setLayoutData(new GridData(768));
		this.textSplitLength.setText(Resources.getProperty("PROP_SPLIT_LENGTH"));
		this.textSplitLength.setTextLimit(4);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));
		label.setText("字节(200-2048)");

		label = new Label(composite, 258);
		gridData = new GridData(800);

		gridData.horizontalSpan = 3;
		label.setLayoutData(gridData);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		this.btnRestartTerminal = new Button(composite, 32);
		this.btnRestartTerminal.setText("要求终端在升级完成后重启");
		this.btnRestartTerminal.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.changeUIState();
			}
		});
		this.btnRestartTerminal
				.setSelection(Boolean.valueOf(Resources.getProperty("PROP_RESTART_TERMINAL")).booleanValue());

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("重启延时时间");

		this.textRestartTerminalDelay = new Text(composite, 2048);
		this.textRestartTerminalDelay.setLayoutData(new GridData(768));
		this.textRestartTerminalDelay.setEditable(this.btnRestartTerminal.getSelection());
		this.textRestartTerminalDelay.setText(Resources.getProperty("PROP_RESTART_TERMINAL_DELAY"));
		this.textRestartTerminalDelay.setTextLimit(3);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));
		label.setText("秒(5-255)");

		changeUIState();

		scrolledComposite.setMinSize(composite.computeSize(-1, -1));
	}

	private void changeUIState() {
		ProtocolType type = ProtocolType.getByRemark(this.cbProtocolType.getText());

		boolean allowLogin = this.btnAllowLoginFeps.getSelection();

		if (type == ProtocolType.SB) {

			this.textMSTA.setTextLimit(2);
			this.lbMSTA.setText("(1-63) ");

			this.textPassword.setEditable(allowLogin);
			this.textHeartbeatInterval.setEditable(allowLogin);

			this.btnDynamicPWD.setEnabled(true);

			this.textUpgradePassword.setEditable(!this.btnDynamicPWD.getSelection());

			this.btnSpecialChannel.setEnabled(false);

			this.textFileVersion.setTextLimit(16);
			this.lbFileVersion.setText("(16位数字，请根据文件内部版本正确填写)");
		} else {

			this.textMSTA.setTextLimit(3);
			this.lbMSTA.setText("(1-127)");

			this.textPassword.setEditable(false);

			this.textHeartbeatInterval.setEditable(allowLogin);

			this.btnDynamicPWD.setEnabled(false);

			this.textUpgradePassword.setEditable(false);

			this.btnSpecialChannel.setEnabled(true);

			this.textFileVersion.setTextLimit(4);
			this.lbFileVersion.setText("(4位字符，请根据目标文件版本准确填写) ");
		}

		this.btnSkipNeedlessUpgrade.setEnabled(this.btnAllowQueryVersion.getSelection());

		this.btnSkipLaterVersion.setEnabled(this.btnAllowQueryVersion.getSelection());

		this.btnAffirmVersion.setEnabled(this.btnAllowQueryVersion.getSelection());

		this.textRestartTerminalDelay.setEditable(this.btnRestartTerminal.getSelection());
	}

	private void createUpgradeTabItem() {
		CTabItem tabItem = new CTabItem(this.folder, 0);
		tabItem.setImage(Resources.getImage("IMAGE_UPGRADE"));
		tabItem.setText("升级操作");

		Composite parent = new Composite((Composite) this.folder, 0);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 2;
		layout.marginTop = 2;
		layout.marginBottom = 0;

		parent.setLayout((Layout) layout);
		tabItem.setControl((Control) parent);

		ToolBar toolBar = new ToolBar(parent, 8519680);
		toolBar.setLayoutData(new GridData(256));

		ToolItem btnNew = new ToolItem(toolBar, 8);
		btnNew.setText("新建任务...");
		btnNew.setImage(Resources.getImage("IMAGE_NEW"));
		btnNew.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DialogNewTask dialog = new DialogNewTask(FormMain.this.getShell(), 34912, FormMain.this.tvTask);
				dialog.open();
			}
		});

		this.btnConnect = new ToolItem(toolBar, 8);
		this.btnConnect.setText("连接前置机");
		this.btnConnect.setImage(Resources.getImage("IMAGE_CONNECT"));

		this.btnStartAll = new ToolItem(toolBar, 8);
		this.btnStartAll.setText("开始所有未成功任务");
		this.btnStartAll.setImage(Resources.getImage("IMAGE_START_ALL"));
		this.btnStartAll.setEnabled(false);

		this.btnDisconnect = new ToolItem(toolBar, 8);
		this.btnDisconnect.setText("关闭连接");
		this.btnDisconnect.setImage(Resources.getImage("IMAGE_DISCONNECT"));
		this.btnDisconnect.setEnabled(false);

		this.btnClear = new ToolItem(toolBar, 8);
		this.btnClear.setText("清空");
		this.btnClear.setImage(Resources.getImage("IMAGE_CLEAR"));

		this.btnConnect.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					FormMain.this.upgradeService.connect();
				} catch (Exception e1) {
					ErrorDialog.openError(null, "注意", "操作失败", (IStatus) new Status(4, "uptools", "无法连接前置机", e1), 4);

					return;
				}
				FormMain.this.btnConnect.setEnabled(false);

				FormMain.this.btnStartAll.setEnabled(true);

				FormMain.this.btnDisconnect.setEnabled(true);

				FormMain.this.btnClear.setEnabled(false);
			}
		});

		this.btnStartAll.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int count = (FormMain.this.upgradeService.getTasks().getStatInfo()).canStartCount;

				if (count == 0) {
					MessageDialog.openWarning(FormMain.this.getShell(), "注意", "目前没有可以开始的任务。");
					return;
				}
				if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意", "目前可以开始执行" + count + "个升级任务，是否确定此操作？"))
					return;
				FormMain.this.upgradeService.startAll();
				FormMain.this.tvTask.getTable().setRedraw(false);
				FormMain.this.tvTask.refresh();
				FormMain.this.tvTask.getTable().setRedraw(true);
			}
		});

		this.btnDisconnect.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.disconnect();
			}
		});

		this.btnClear.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (FormMain.this.upgradeService.getTasks().isDone()) {
					if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意", "是否确定删除当前所有任务？")) {
						return;
					}
				} else if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意", "还有任务尚未完成，是否确定删除当前所有任务？")) {
					return;
				}
				FormMain.this.upgradeService.clearTask();
				FormMain.this.tvTask.getTable().setRedraw(false);
				FormMain.this.tvTask.refresh();
				FormMain.this.tvTask.getTable().setRedraw(true);
			}
		});

		CSashForm sashForm = new CSashForm(parent, 8389120, 4);
		sashForm.setLayoutData(new GridData(1808));

		this.tvTask = new TableViewer(sashForm, 67586);

		Table table = this.tvTask.getTable();
		table.setLayoutData(new GridData(1808));

		SelectionAdapter adapter = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				((TaskViewerSorter) FormMain.this.tvTask.getSorter()).setSortColumn((TableColumn) e.widget);
				FormMain.this.tvTask.getTable().setRedraw(false);
				FormMain.this.tvTask.refresh();
				FormMain.this.tvTask.getTable().setRedraw(true);
			}
		};

		String[] columnNames = { "终端地址", "状态", "最近事件", "发生时间", "升级前版本", "当前版本", "进度" };
		for (int i = 0; i < columnNames.length; i++) {
			TableColumn column = new TableColumn(table, 16384);
			column.setText(columnNames[i]);
			column.addSelectionListener((SelectionListener) adapter);
			switch (i) {
			case 0:
				column.setWidth(150);
				break;
			case 1:
				column.setWidth(120);
				break;
			case 2:
				column.setWidth(450);
				break;
			case 3:
				column.setWidth(130);
				break;
			case 4:
				column.setWidth(120);
				break;
			case 5:
				column.setWidth(120);
				break;
			case 6:
				column.setWidth(110);
				break;
			}

		}
		this.tvTask.setContentProvider((IContentProvider) new StructuredContentProviderAdapter() {
			public Object[] getElements(Object inputElement) {
				TaskList list = (TaskList) inputElement;
				if (list != null) {
					return list.toArray();
				}
				return null;
			}
		});

		this.tvTask.setLabelProvider((IBaseLabelProvider) new TableLabelProviderAdapter() {
			public Image getColumnImage(Object element, int columnIndex) {
				if (columnIndex == 0) {
					Task task = (Task) element;
					if (task.isNew())
						return Resources.getImage("IMAGE_TASK_STATE_NEW");
					if (task.isFinish())
						return Resources.getImage("IMAGE_TASK_STATE_FINISH");
					if (task.isWaiting())
						return Resources.getImage("IMAGE_TASK_STATE_WAITING");
					if (task.isUpgrading() || task.isCanceling()) {
						return Resources.getImage("IMAGE_TASK_STATE_UPGRADING");
					}

					return Resources.getImage("IMAGE_TASK_STATE_STOP");
				}
				return null;
			}

			public String getColumnText(Object element, int columnIndex) {
				String addr1, addr2;
				Task task = (Task) element;
				Event event = task.lastEvent();
				switch (columnIndex) {
				case 0:
					addr1 = task.getTerminalAddr().substring(0, 4);
					addr2 = task.getTerminalAddr().substring(4, 8);

					if (FormMain.this.miDecAddr != null && FormMain.this.miDecAddr.getSelection()) {
						addr2 = SysUtils.formatString(Integer.toString(Integer.valueOf(addr2, 16).intValue(), 10), '0',
								5);
					}

					if (FormMain.this.miSprAddr != null && FormMain.this.miSprAddr.getSelection()) {
						return String.valueOf(addr1) + "-" + addr2;
					}
					return String.valueOf(addr1) + addr2;

				case 1:
					return task.getStateRemark();
				case 2:
					return task.getRemark();
				case 3:
					if (task.getLastTime() != null)
						return SysUtils.timeToStr(task.getLastTime());
					return "";
				case 4:
					return task.getOldVersion();
				case 5:
					return task.getCurrentVersion();
				case 6:
					return String.valueOf(String.valueOf(task.getRate())) + "%";
				}
				return "";
			}
		});

		this.tvTask.setInput(this.upgradeService.getTasks());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		this.tvTask.setSorter(new TaskViewerSorter(this.tvTask));

		this.tvTask.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Task task = (Task) ((StructuredSelection) event.getSelection()).getFirstElement();

				FormMain.this.showTaskInfo(task);
			}
		});

		this.tvTask.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				FormMain.this.folderBottom.setSelection(1);
			}
		});

		final Runnable runnable = new Runnable() {
			public void run() {
				if (FormMain.this.tvTask.getTable().isDisposed())
					return;
				while (true) {
					Task task = (Task) FormMain.this.refreshQueue.take();
					if (task == null) {
						return;
					}
					FormMain.this.tvTask.refresh(task);

					if (task == (Task) ((StructuredSelection) FormMain.this.tvTask.getSelection()).getFirstElement()) {
						FormMain.this.showTaskInfo(task);
					}
				}
			}
		};

		this.upgradeService.addTaskChangedListener(new ITaskChangedListener() {
			public void taskChanged(Task task) {
				FormMain.this.refreshQueue.put(task);
				Display.getDefault().asyncExec(runnable);
			}
		});

		Menu menu = new Menu((Control) table);
		table.setMenu(menu);

		this.miStart = new MenuItem(menu, 0);
		this.miStart.setText("开始升级");
		this.miStart.setImage(Resources.getImage("IMAGE_START"));
		this.miStart.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StructuredSelection selection = (StructuredSelection) FormMain.this.tvTask.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意",
						"是否确定开始升级当前所选的" + selection.size() + "个任务？")) {
					return;
				}

				Iterator<Task> iter = selection.iterator();
				while (iter.hasNext()) {
					Task task = iter.next();
					if (task == null) {
						continue;
					}
					if (task.isFinish())
						if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意",
								"终端地址为" + task.getTerminalAddr() + "的任务已执行并完成升级，是否确定要重新开始？")) {
							continue;
						}
					FormMain.this.upgradeService.startTask(task);
					FormMain.this.tvTask.refresh(task);
				}
			}
		});

		this.miBreak = new MenuItem(menu, 0);
		this.miBreak.setText("中断操作");
		this.miBreak.setImage(Resources.getImage("IMAGE_TASK_STATE_STOP"));
		this.miBreak.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StructuredSelection selection = (StructuredSelection) FormMain.this.tvTask.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意",
						"是否确定中断当前所选的" + selection.size() + "个任务？")) {
					return;
				}
				Iterator<Task> iter = selection.iterator();
				while (iter.hasNext()) {
					Task task = iter.next();
					if (task == null)
						continue;
					if (task.isBreakPending()) {
						MessageDialog.openWarning(FormMain.this.getShell(), "注意",
								"终端地址为" + task.getTerminalAddr() + "的任务已请求中断，并将在稍后得到处理。");
						continue;
					}
					FormMain.this.upgradeService.pauseTask(task);
					FormMain.this.tvTask.refresh(task);
				}
			}
		});

		this.miCancel = new MenuItem(menu, 0);
		this.miCancel.setText("取消升级");
		this.miCancel.setImage(Resources.getImage("IMAGE_CANCEL"));
		this.miCancel.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StructuredSelection selection = (StructuredSelection) FormMain.this.tvTask.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意",
						"是否确定取消当前所选的" + selection.size() + "个任务？")) {
					return;
				}
				Iterator<Task> iter = selection.iterator();
				while (iter.hasNext()) {
					Task task = iter.next();
					if (task == null) {
						continue;
					}

					if (task.isFinish())
						if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意",
								"终端地址为" + task.getTerminalAddr() + "的任务已成功升级，是否确定发送取消命令？")) {
							continue;
						}
					FormMain.this.upgradeService.cancelTask(task);
					FormMain.this.tvTask.refresh(task);
				}
			}
		});

		this.miDelete = new MenuItem(menu, 0);
		this.miDelete.setText("删除");
		this.miDelete.setImage(Resources.getImage("IMAGE_DELETE"));
		this.miDelete.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StructuredSelection selection = (StructuredSelection) FormMain.this.tvTask.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意",
						"是否确定删除当前所选的" + selection.size() + "个任务？注意：未停止的任务将不会删除。")) {
					return;
				}
				Iterator<Task> iter = selection.iterator();
				while (iter.hasNext()) {
					Task task = iter.next();
					if (task == null)
						continue;
					if (!task.isCanceling() && !task.isUpgrading())
						FormMain.this.upgradeService.removeTask(task);
				}
				FormMain.this.tvTask.getTable().setRedraw(false);
				FormMain.this.tvTask.refresh();
				FormMain.this.tvTask.getTable().setRedraw(true);
			}
		});

		this.miClearDone = new MenuItem(menu, 0);
		this.miClearDone.setText("清除所有已成功任务");
		this.miClearDone.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog.openQuestion(FormMain.this.getShell(), "注意", "是否确定清除所有已成功任务？")) {
					return;
				}
				FormMain.this.upgradeService.clearDone();
				FormMain.this.tvTask.getTable().setRedraw(false);
				FormMain.this.tvTask.refresh();
				FormMain.this.tvTask.getTable().setRedraw(true);
			}
		});

		MenuItem miView = new MenuItem(menu, 64);
		miView.setText("查看");

		Menu childMenu = new Menu(miView);
		miView.setMenu(childMenu);

		this.miSprAddr = new MenuItem(childMenu, 32);
		this.miSprAddr.setText("地址分隔符");
		this.miSprAddr.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.tvTask.getTable().setRedraw(false);
				FormMain.this.tvTask.refresh();
				FormMain.this.tvTask.getTable().setRedraw(true);
			}
		});

		this.miDecAddr = new MenuItem(childMenu, 32);
		this.miDecAddr.setText("按十进制显示");
		this.miDecAddr.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.tvTask.getTable().setRedraw(false);
				FormMain.this.tvTask.refresh();
				FormMain.this.tvTask.getTable().setRedraw(true);
			}
		});

		MenuItem miExport = new MenuItem(menu, 64);
		miExport.setText("导出终端地址");

		childMenu = new Menu(miExport);
		miExport.setMenu(childMenu);

		MenuItem menuItem = new MenuItem(childMenu, 0);
		menuItem.setText("所有任务...");
		menuItem.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] tasks = FormMain.this.upgradeService.getTasks().toArray();
				FormMain.this.exportTerminalAddr(tasks);
			}
		});

		menuItem = new MenuItem(childMenu, 0);
		menuItem.setText("所有未完成任务...");
		menuItem.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] tasks = FormMain.this.upgradeService.getTasks().unfinishedTasks();
				FormMain.this.exportTerminalAddr(tasks);
			}
		});

		menuItem = new MenuItem(childMenu, 0);
		menuItem.setText("当前所选任务...");
		menuItem.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] tasks = ((StructuredSelection) FormMain.this.tvTask.getSelection()).toArray();
				FormMain.this.exportTerminalAddr(tasks);
			}
		});

		menuItem = new MenuItem(menu, 8);
		menuItem.setText("统计");
		menuItem.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TasksStatInfo info = FormMain.this.upgradeService.getTasks().getStatInfo();
				StringBuffer buffer = new StringBuffer();
				buffer.append("任务完成情况：");
				buffer.append(info.finishedCount);
				buffer.append("/");
				buffer.append(info.count);
				buffer.append("(");
				buffer.append(info.finishedRate);
				buffer.append("%)");
				buffer.append("\n");
				buffer.append("已开始任务：");
				buffer.append(info.runningCount);
				buffer.append("，等待开始任务：");
				buffer.append(info.canStartCount);

				MessageDialog.openInformation(FormMain.this.getShell(), "统计", buffer.toString());
			}
		});

		menu.addMenuListener((MenuListener) new MenuAdapter() {

			public void menuShown(MenuEvent e) {
				FormMain.this.miClearDone.setEnabled(true);

				boolean opened = !FormMain.this.upgradeService.isClosed();

				Task task = (Task) ((StructuredSelection) FormMain.this.tvTask.getSelection()).getFirstElement();

				boolean enabled = (task != null && opened && !task.isUpgrading() && !task.isCanceling());

				FormMain.this.miStart.setEnabled(enabled);

				FormMain.this.miCancel.setEnabled(enabled);

				FormMain.this.miBreak
						.setEnabled((task != null && opened && (task.isUpgrading() || task.isCanceling())));

				FormMain.this.miDelete.setEnabled((task != null && !task.isUpgrading() && !task.isCanceling()));
			}
		});

		this.folderBottom = new CTabFolder(sashForm, 2048);
		this.folderBottom.setSimple(false);
		this.folderBottom.setTabPosition(1024);
		this.folderBottom.setUnselectedImageVisible(true);
		this.folderBottom.setUnselectedCloseVisible(true);
		this.folderBottom.setSelectionBackground(Resources.getSystemColor(35));
		this.folderBottom.setTabHeight(22);
		this.folderBottom.setLayoutData(new GridData(1808));

		tabItem = new CTabItem(this.folderBottom, 0);
		tabItem.setText("运行信息");

		this.textSysInfo = new StyledText((Composite) this.folderBottom, 8389378);

		this.textSysInfo.setLineSpacing(4);
		this.textSysInfo.setIndent(5);
		this.textSysInfo.setEditable(false);
		this.textSysInfo.setLayoutData(new GridData(1808));
		tabItem.setControl((Control) this.textSysInfo);

		tabItem = new CTabItem(this.folderBottom, 0);
		tabItem.setText("任务信息");

		this.textTaskInfo = new TaskEventListViewer((Composite) this.folderBottom, 8389378);

		this.textTaskInfo.setLineSpacing(4);
		this.textTaskInfo.setIndent(5);
		this.textTaskInfo.setEditable(false);
		this.textTaskInfo.setLayoutData(new GridData(1808));
		tabItem.setControl((Control) this.textTaskInfo);

		this.folderBottom.setSelection(0);

		sashForm.setControlSize((Control) this.folderBottom, 200);
	}

	private void createMenu() {
		Menu menubar = new Menu((Decorations) this, 2);
		setMenuBar(menubar);

		MenuItem miFile = new MenuItem(menubar, 64);
		miFile.setText("文件");

		Menu menu = new Menu(miFile);
		miFile.setMenu(menu);

		MenuItem miExit = new MenuItem(menu, 0);
		miExit.setText("退出");

		miExit.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FormMain.this.close();
			}
		});

		MenuItem miHelp = new MenuItem(menubar, 64);
		miHelp.setText("帮助");

		menu = new Menu(miHelp);
		miHelp.setMenu(menu);

		MenuItem miAbout = new MenuItem(menu, 0);
		miAbout.setText("关于");

		miAbout.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				(new DialogAbout(FormMain.this.getShell(), 34912)).open();
			}
		});
	}

	protected void checkSubclass() {
	}

	private boolean disconnect() {
		int count = (this.upgradeService.getTasks().getStatInfo()).runningCount;

		if (count > 0)
			if (!MessageDialog.openQuestion(getShell(), "注意", "还有" + count + "个任务正在运行中，是否确定关闭连接？")) {
				return false;
			}
		this.upgradeService.disconnect();

		this.btnConnect.setEnabled(true);

		this.btnStartAll.setEnabled(false);

		this.btnDisconnect.setEnabled(false);

		this.btnClear.setEnabled(true);

		this.tvTask.getTable().setRedraw(false);
		this.tvTask.refresh();
		this.tvTask.getTable().setRedraw(true);

		return true;
	}

	private void exportTerminalAddr(Object[] tasks) {
		if (tasks.length == 0) {
			return;
		}

		FileDialog dialog = new FileDialog(getShell(), 8192);

		dialog.setFileName("终端列表.txt");
		dialog.setFilterExtensions(new String[] { "*.txt" });
		dialog.setFilterNames(new String[] { "文本文件(*.txt)" });

		dialog.setOverwrite(true);

		String filename = dialog.open();

		if (filename != null)

			try {

				BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
				for (int i = 0; i < tasks.length; i++) {
					writer.write(((Task) tasks[i]).getTerminalAddr());
					writer.newLine();
				}
				writer.flush();
				writer.close();
			} catch (IOException e1) {
				MessageDialog.openError(getShell(), "注意", "无法创建指定的文件。");
				return;
			}
	}
}
