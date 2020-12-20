package com.sanxing.upgrade.ui;

import com.sanxing.upgrade.util.Resources;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

public class DialogAbout extends Dialog {
	protected Object result;
	protected Shell shell;

	public DialogAbout(Shell parent, int style) {
		super(parent, style);
		setText("关于");
	}

	public Object open() {
		createContents();
		this.shell.open();
		this.shell.layout();
		Display display = getParent().getDisplay();
		while (!this.shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return this.result;
	}

	private void createContents() {
		this.shell = new Shell(getParent(), getStyle());
		this.shell.setSize(453, 202);
		this.shell.setText(getText());

		GridLayout layout = new GridLayout(2, false);
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 12;

		this.shell.setLayout((Layout) layout);

		Label label = new Label((Composite) this.shell, 0);
		label.setImage(Resources.getImage("IMAGE_DUKE"));
		GridData gridData = new GridData();
		gridData.verticalSpan = 2;
		label.setLayoutData(gridData);

		label = new Label((Composite) this.shell, 0);
		label.setLayoutData(new GridData(32));
		label.setText("终端升级工具 (0.1.11)");
		label.setFont(Resources.getBoldFont("FONT_DEFAULT"));

		label = new Label((Composite) this.shell, 0);
		label.setLayoutData(new GridData(32));
		label.setText("Copyright 2010 Sanxing.All Rights Reserved.");

		label = new Label((Composite) this.shell, 0);
		label = new Label((Composite) this.shell, 0);

		label = new Label((Composite) this.shell, 258);
		gridData = new GridData(768);
		gridData.horizontalSpan = 3;
		label.setLayoutData(gridData);

		Button btnOK = new Button((Composite) this.shell, 0);
		gridData = new GridData(128);
		gridData.horizontalSpan = 2;
		gridData.widthHint = 80;
		btnOK.setLayoutData(gridData);
		btnOK.setText("确定");

		btnOK.addSelectionListener((SelectionListener) new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DialogAbout.this.shell.close();
			}
		});

		this.shell.setDefaultButton(btnOK);

		Rectangle parentBounds = getParent().getBounds();
		Rectangle shellBounds = this.shell.getBounds();

		this.shell.setLocation(parentBounds.x + (parentBounds.width - shellBounds.width) / 2,
				parentBounds.y + (parentBounds.height - shellBounds.height) / 2);
	}
}
