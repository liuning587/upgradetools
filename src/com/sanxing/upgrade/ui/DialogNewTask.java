package com.sanxing.upgrade.ui;

import com.sanxing.upgrade.business.UpgradeService;
import com.sanxing.upgrade.util.Resources;
import com.sanxing.upgrade.util.SysUtils;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogNewTask extends Dialog {
	private UpgradeService upgradeService = UpgradeService.getInstance();

	private TableViewer tvTask;

	protected Shell shell;

	private CTabFolder folder;

	private Text textA;

	private Text textStart;

	private Text textEnd;

	private Text textFilename;

	private Button btnSelectFile;

	private Button btnBcd;

	private Button btnHex;

	private Button btnDec;

	private Button btnDecFile;
	private Button btnContinue;
	private Label lbStart;
	private Label lbEnd;

	public DialogNewTask(Shell parent, int style, TableViewer tvTask) {
		super(parent, style);
		setText("新建任务");
		this.tvTask = tvTask;
	}

	public void open() {
		createContents();
		this.shell.open();
		this.shell.layout();
		Display display = getParent().getDisplay();
		while (!this.shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void createContents() {
		this.shell = new Shell(getParent(), getStyle());
		this.shell.setSize(550, 450);
		this.shell.setText(getText());

		GridLayout layout = new GridLayout();
		layout.marginTop = 8;
		layout.marginBottom = 10;
		layout.marginLeft = 5;
		layout.marginRight = 5;

		this.shell.setLayout((Layout) layout);

		CLabel clabel = new CLabel((Composite) this.shell, 0);
		clabel.setImage(Resources.getImage("IMAGE_INFORMATION_SMALL"));
		clabel.setFont(Resources.getBoldFont("FONT_DEFAULT"));
		clabel.setText("如果对应任务已经存在，将忽略处理。");

		this.folder = new CTabFolder((Composite) this.shell, 2048);
		this.folder.setSimple(false);
		this.folder.setSelectionBackground(Resources.getSystemColor(35));
		this.folder.setTabHeight(22);
		this.folder.setLayoutData(new GridData(768));

		CTabItem tabItem = new CTabItem(this.folder, 0);
		tabItem.setText("输入");

		Composite composite = new Composite((Composite) this.folder, 0);
		layout = new GridLayout(3, false);
		layout.marginTop = 2;
		layout.marginBottom = 2;
		layout.marginLeft = 2;
		layout.marginRight = 2;
		composite.setLayout((Layout) layout);

		tabItem.setControl((Control) composite);

		Label label = new Label(composite, 64);
		label.setText("　　输入终端的起始和结束地址可以批量创建任务，单个任务只要输入起始地址即可。");
		GridData gridData = new GridData();
		gridData.horizontalSpan = 3;
		gridData.widthHint = 355;
		label.setLayoutData(gridData);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setAlignment(16384);
		label.setText(" 行政区划码");

		this.textA = new Text(composite, 2048);
		this.textA.setLayoutData(new GridData(768));
		this.textA.setTextLimit(4);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(32));
		label.setText("4位HEX");

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("起始地址");

		this.textStart = new Text(composite, 2048);
		this.textStart.setLayoutData(new GridData(768));
		this.textStart.setTextLimit(4);

		this.lbStart = new Label(composite, 0);
		this.lbStart.setLayoutData(new GridData(32));
		this.lbStart.setText("1-4位");

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("结束地址");

		this.textEnd = new Text(composite, 2048);
		this.textEnd.setLayoutData(new GridData(768));
		this.textEnd.setTextLimit(4);

		this.lbEnd = new Label(composite, 0);
		this.lbEnd.setLayoutData(new GridData(32));
		this.lbEnd.setText("1-4位");

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("地址格式");

		Composite cpFormat = new Composite(composite, 0);
		gridData = new GridData(768);
		cpFormat.setLayoutData(gridData);

		layout = new GridLayout(3, true);
		layout.marginTop = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.marginBottom = 0;
		cpFormat.setLayout((Layout) layout);

		this.btnBcd = new Button(cpFormat, 16);
		this.btnBcd.setText("BCD");
		this.btnBcd.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DialogNewTask.this.changeUIState();
			}
		});
		this.btnBcd.setSelection(true);

		this.btnHex = new Button(cpFormat, 16);
		this.btnHex.setText("HEX");
		this.btnHex.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DialogNewTask.this.changeUIState();
			}
		});

		this.btnDec = new Button(cpFormat, 16);
		this.btnDec.setText("十进制");
		this.btnDec.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DialogNewTask.this.changeUIState();
			}
		});

		tabItem = new CTabItem(this.folder, 0);
		tabItem.setText("导入文件");

		composite = new Composite((Composite) this.folder, 0);
		layout = new GridLayout(3, false);
		layout.marginTop = 2;
		layout.marginBottom = 2;
		layout.marginLeft = 2;
		layout.marginRight = 2;
		composite.setLayout((Layout) layout);

		label = new Label(composite, 64);
		label.setText("　　请选择一个文本文件，文件的格式为每行一个终端地址，8或9位数字，注意：数字中间不含其它字符。");
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		gridData.widthHint = 355;
		label.setLayoutData(gridData);

		label = new Label(composite, 0);
		label.setLayoutData(new GridData(128));
		label.setText("文件名");

		this.textFilename = new Text(composite, 2048);
		this.textFilename.setLayoutData(new GridData(768));
		this.textFilename.setEditable(false);

		this.btnSelectFile = new Button(composite, 8);
		gridData = new GridData(32);
		gridData.widthHint = 120;
		this.btnSelectFile.setLayoutData(gridData);
		this.btnSelectFile.setText("选择 ...");
		this.btnSelectFile.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(DialogNewTask.this.shell, 4096);

				dialog.setFilterExtensions(new String[] { "*.txt" });
				dialog.setFilterNames(new String[] { "文本文件(*.txt)" });

				String filename = dialog.open();
				if (filename != null) {
					DialogNewTask.this.textFilename.setText(filename);
				}
			}
		});

		this.btnDecFile = new Button(composite, 32);
		this.btnDecFile.setText("地址为9位十进制");
		gridData = new GridData(32);
		gridData.horizontalSpan = 3;
		this.btnDecFile.setLayoutData(gridData);

		tabItem.setControl((Control) composite);

		this.folder.setSelection(0);
		this.textA.forceFocus();

		composite = new Composite((Composite) this.shell, 0);
		composite.setLayout((Layout) new FormLayout());
		gridData = new GridData(1812);

		composite.setLayoutData(gridData);

		this.btnContinue = new Button(composite, 32);
		this.btnContinue.setText("确定后继续添加");
		FormData formData = new FormData();
		formData.top = new FormAttachment(10);
		formData.left = new FormAttachment(0, 10);
		formData.width = 180;
		this.btnContinue.setLayoutData(formData);

		Button btnOK = new Button(composite, 0);
		btnOK.setSelection(true);
		formData = new FormData();
		formData.top = new FormAttachment(10);
		formData.left = new FormAttachment(0, 222);
		formData.width = 75;
		btnOK.setLayoutData(formData);
		btnOK.setText("确定");
		btnOK.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int radix = DialogNewTask.this.btnHex.getSelection() ? 16 : 10;
				int count = 1;
				String[] addrs = (String[]) null;
				if (DialogNewTask.this.folder.getSelectionIndex() == 0){
					if (!DialogNewTask.this.textA.getText().trim().matches("(?i)[0-9a-f]{4}")) {
						MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "请正确输入地市区县码。");
						DialogNewTask.this.textA.forceFocus();
						return;
					}
					String strA = DialogNewTask.this.textA.getText().toUpperCase();
					String strStart = DialogNewTask.this.textStart.getText().trim().toUpperCase();
					String strEnd = DialogNewTask.this.textEnd.getText().trim().toUpperCase();

					if (strStart.isEmpty()) {
						MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "请正确输入起始地址。");
						DialogNewTask.this.textStart.forceFocus();

						return;
					}

					if (DialogNewTask.this.btnHex.getSelection()) {

						if (!strStart.matches("(?i) *[0-9a-f]+ *")) {

							MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "请正确输入起始地址。");
							DialogNewTask.this.textStart.forceFocus();

							return;
						}
						if (!strEnd.isEmpty() && !strEnd.matches("(?i) *[0-9a-f]+ *")) {

							MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "请正确输入结束地址。");
							DialogNewTask.this.textEnd.forceFocus();

							return;
						}
					} else {
						if (!strStart.matches(" *\\d+ *")
								|| (DialogNewTask.this.btnBcd.getSelection() && strStart.length() > 4)
								|| (DialogNewTask.this.btnDec.getSelection()
										&& Integer.valueOf(strStart).intValue() > 65535)) {

							MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "请正确输入起始地址。");
							DialogNewTask.this.textStart.forceFocus();

							return;
						}
						if (!strEnd.isEmpty() && (!strEnd.matches(" *\\d+ *")
								|| (DialogNewTask.this.btnBcd.getSelection() && strEnd.length() > 4)
								|| (DialogNewTask.this.btnDec.getSelection()
										&& Integer.valueOf(strEnd).intValue() > 65535))) {

							MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "请正确输入结束地址。");
							DialogNewTask.this.textEnd.forceFocus();
							return;
						}
					}
					if (!strEnd.isEmpty())
						count = Integer.valueOf(strEnd, radix).intValue() - Integer.valueOf(strStart, radix).intValue()
								+ 1;
					if (count < 0) {
						MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "结束地址应大于起始地址。");
						DialogNewTask.this.textEnd.forceFocus();

						return;
					}
					int limit = DialogNewTask.this.textStart.getTextLimit();
					if (strEnd.isEmpty()) {
						addrs = new String[count];
						if (DialogNewTask.this.btnDec.getSelection()) {
							addrs[0] = String.valueOf(strA) + SysUtils.formatString(
									Integer.toString(Integer.valueOf(strStart).intValue(), 16).toUpperCase(), '0', limit);
						} else {
							addrs[0] = String.valueOf(strA) + SysUtils.formatString(strStart, '0', limit);
						}
					} else {
						addrs = new String[count];
						int start = Integer.valueOf(strStart, radix).intValue();
						for (int j = 0; j < count; j++) {
							if (DialogNewTask.this.btnDec.getSelection()) {
								addrs[j] = String.valueOf(strA)
										+ SysUtils.formatString(Integer.toString(start + j, 16).toUpperCase(), '0', limit);
							} else {
								addrs[j] = String.valueOf(strA) + SysUtils
										.formatString(Integer.toString(start + j, radix).toUpperCase(), '0', limit);
							}
						}
					}
				} else {
					BufferedReader reader;
					if (DialogNewTask.this.textFilename.getText().isEmpty()) {
						MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "请选择要导入的文件。");

						return;
					}
					try {
						reader = new BufferedReader(new FileReader(DialogNewTask.this.textFilename.getText()));
					} catch (FileNotFoundException e1) {
						MessageDialog.openError(DialogNewTask.this.shell, "注意", "无法打开所选择的文件。");
						return;
					}
					StringBuffer buffer = new StringBuffer();

//					try {
//					} catch (IOException e1) {
//						MessageDialog.openError(DialogNewTask.this.shell, "注意", "读取所选择的文件失败。");
//						return;
//					} finally {
//						if (reader != null)
//							try {
//								reader.close();
//							} catch (IOException iOException) {
//							}
//					}
					if (reader != null)
						try {
							reader.close();
						} catch (IOException iOException) {
						}

					if (buffer.length() == 0) {
						MessageDialog.openWarning(DialogNewTask.this.shell, "注意", "没有任务可以添加。");
						return;
					}
					addrs = buffer.toString().split(",");
				}

				if (!MessageDialog.openQuestion(DialogNewTask.this.shell, "注意", "将创建" + addrs.length + "个升级任务，是否确定操作？"))
					return;
				for (int i = 0; i < addrs.length; i++) {
					DialogNewTask.this.upgradeService.appendTask(addrs[i]);
				}
				addrs = (String[]) null;

				DialogNewTask.this.tvTask.getTable().setRedraw(false);
				DialogNewTask.this.tvTask.refresh();
				DialogNewTask.this.tvTask.getTable().setRedraw(true);

				if (DialogNewTask.this.btnContinue.getSelection()) {
					if (DialogNewTask.this.folder.getSelectionIndex() == 0) {
						DialogNewTask.this.textStart.setText("");
						DialogNewTask.this.textEnd.setText("");
						DialogNewTask.this.textStart.forceFocus();
					}
				} else {
					DialogNewTask.this.shell.close();
				}
			}
		});
		Button btnCancel = new Button(composite, 0);
		formData = new FormData();

		formData.top = new FormAttachment(10);
		formData.left = new FormAttachment((Control) btnOK, 2);
		formData.width = 75;
		btnCancel.setLayoutData(formData);
		btnCancel.setText("取消");

		btnCancel.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DialogNewTask.this.shell.close();
			}
		});

		this.shell.setDefaultButton(btnOK);

		Rectangle parentBounds = getParent().getBounds();
		Rectangle shellBounds = this.shell.getBounds();

		this.shell.setLocation(parentBounds.x + (parentBounds.width - shellBounds.width) / 2,
				parentBounds.y + (parentBounds.height - shellBounds.height) / 2);
	}

	private void changeUIState() {
		if (this.btnDec.getSelection()) {
			this.textStart.setTextLimit(5);
			this.lbStart.setText("1-5位");
			this.textEnd.setTextLimit(5);
			this.lbEnd.setText("1-5位");
		} else if (this.btnHex.getSelection()) { // 698地址长度6字节
			this.textStart.setTextLimit(8);
			this.lbStart.setText("1-8位");
			this.textEnd.setTextLimit(8);
			this.lbEnd.setText("1-8位");
		} else {
			this.textStart.setTextLimit(4);
			this.lbStart.setText("1-4位");
			this.textEnd.setTextLimit(4);
			this.lbEnd.setText("1-4位");
		}
	}
}
