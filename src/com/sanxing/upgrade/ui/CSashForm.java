package com.sanxing.upgrade.ui;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Sash;

public class CSashForm extends Composite {
	private int sashStyle;
	private Sash sash;
	private final int DRAG_MINIMUM = 20;
	private int sashWidth;
	private Control control;

	public CSashForm(Composite parent, int style, int sashWidth) {
		super(parent, style & 0x6000800);
		this.sashStyle = ((style & 0x200) != 0) ? 256 : 512;
		if ((style & 0x800) != 0)
			this.sashStyle |= 0x800;
		if ((style & 0x10000) != 0)
			this.sashStyle |= 0x10000;
		this.sashWidth = sashWidth;
	}

	public void setControlSize(Control control, int size) {
		if (this.control != null) {
			return;
		}
		if (2 != (getChildren()).length)
			return;
		this.control = control;
		setLayout((Layout) new FormLayout());
		this.sash = new Sash(this, this.sashStyle);

		Control control1 = getChildren()[0];
		Control control2 = getChildren()[1];
		if (control1 != control && control2 != control)
			return;
		if ((this.sashStyle & 0x200) != 0) {
			FormData fdControl1 = new FormData();
			FormData fdSash = new FormData();
			FormData fdControl2 = new FormData();

			fdControl1.top = new FormAttachment(0, 0);
			fdControl1.bottom = new FormAttachment(100, 0);
			fdSash.top = new FormAttachment(0, 0);
			fdSash.bottom = new FormAttachment(100, 0);
			fdControl2.top = new FormAttachment(0, 0);
			fdControl2.bottom = new FormAttachment(100, 0);

			fdSash.width = this.sashWidth;

			fdControl1.left = new FormAttachment(0, 0);

			fdControl2.right = new FormAttachment(100, 0);

			if (control1 == control) {

				fdControl1.width = size;

				fdSash.left = new FormAttachment(control1);

				fdControl2.left = new FormAttachment((Control) this.sash);
			} else {

				fdControl2.width = size;

				fdSash.right = new FormAttachment(control2);

				fdControl1.right = new FormAttachment((Control) this.sash);
			}
			control1.setLayoutData(fdControl1);
			this.sash.setLayoutData(fdSash);
			control2.setLayoutData(fdControl2);

			this.sash.addMouseListener((MouseListener) new MouseAdapter() {
				public void mouseUp(MouseEvent e) {
					Control leftControl = CSashForm.this.getChildren()[0];
					Control rightControl = CSashForm.this.getChildren()[1];
					FormData formData = (FormData) CSashForm.this.control.getLayoutData();

					if (CSashForm.this.control == leftControl) {
						formData.width += e.x;
					} else {
						formData.width -= e.x;
					}

					if (20 > formData.width) {
						formData.width = 20;
					}

					if (formData.width + leftControl.getBorderWidth() * 2 + (CSashForm.this.sash.getBounds()).width
							+ rightControl.getBorderWidth() * 2 + 20 > (CSashForm.this.getClientArea()).width) {
						formData.width = (CSashForm.this.getClientArea()).width
								- (CSashForm.this.sash.getBounds()).width - leftControl.getBorderWidth() * 2
								- rightControl.getBorderWidth() * 2 - 20;
					}
					CSashForm.this.layout();
				}
			});
		} else {
			FormData fdControl1 = new FormData();
			FormData fdSash = new FormData();
			FormData fdControl2 = new FormData();

			fdControl1.left = new FormAttachment(0, 0);
			fdControl1.right = new FormAttachment(100, 0);
			fdSash.left = new FormAttachment(0, 0);
			fdSash.right = new FormAttachment(100, 0);
			fdControl2.left = new FormAttachment(0, 0);
			fdControl2.right = new FormAttachment(100, 0);

			fdSash.width = this.sashWidth;

			fdControl1.top = new FormAttachment(0, 0);

			fdControl2.bottom = new FormAttachment(100, 0);

			if (control1 == control) {

				fdControl1.height = size;

				fdSash.top = new FormAttachment(control1);

				fdControl2.top = new FormAttachment((Control) this.sash);
			} else {

				fdControl2.height = size;

				fdSash.bottom = new FormAttachment(control2);

				fdControl1.bottom = new FormAttachment((Control) this.sash);
			}
			control1.setLayoutData(fdControl1);
			this.sash.setLayoutData(fdSash);
			control2.setLayoutData(fdControl2);

			this.sash.addMouseListener((MouseListener) new MouseAdapter() {
				public void mouseUp(MouseEvent e) {
					Control topControl = CSashForm.this.getChildren()[0];
					Control bottomControl = CSashForm.this.getChildren()[1];
					FormData formData = (FormData) CSashForm.this.control.getLayoutData();

					if (CSashForm.this.control == topControl) {
						formData.height += e.y;
					} else {
						formData.height -= e.y;
					}
					if (formData.height < 0) {
						formData.height = 0;
					}
					if (formData.height + topControl.getBorderWidth() * 2 + (CSashForm.this.sash.getBounds()).height
							+ bottomControl.getBorderWidth() * 2 > (CSashForm.this.getClientArea()).height) {
						formData.height = (CSashForm.this.getClientArea()).height
								- (CSashForm.this.sash.getBounds()).height - topControl.getBorderWidth() * 2
								- bottomControl.getBorderWidth() * 2;
					}
					CSashForm.this.layout();
				}
			});
		}
	}
}
