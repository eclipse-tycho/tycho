/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - bug 398539
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Page that allows users to update, add, remove, import, and export
 * repositories. This page can be hosted inside a preference dialog or inside
 * its own dialog.
 *
 * When hosting this page inside a non-preference dialog, some of the dialog
 * methods will likely have to call page methods. The following snippet shows
 * how to host this page inside a TitleAreaDialog.
 *
 * <pre>
 * TitleAreaDialog dialog = new TitleAreaDialog(shell) {
 *
 * 	RepositoryManipulationPage page;
 *
 * 	protected Control createDialogArea(Composite parent) {
 * 		page = new RepositoryManipulationPage();
 * 		page.setProvisioningUI(ProvisioningUI.getDefaultUI());
 * 		page.createControl(parent);
 * 		this.setTitle("Software Sites");
 * 		this.setMessage("The enabled sites will be searched for software.  Disabled sites are ignored.");
 * 		return page.getControl();
 * 	}
 *
 * 	protected void okPressed() {
 * 		if (page.performOk())
 * 			super.okPressed();
 * 	}
 *
 * 	protected void cancelPressed() {
 * 		if (page.performCancel())
 * 			super.cancelPressed();
 * 	}
 * };
 * dialog.open();
 * </pre>
 *
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 2.0
 */
public class RepositoryManipulationPage extends PreferencePage implements IWorkbenchPreferencePage, ICopyable {

	@Override
	public void copyToClipboard(Control activeControl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Control createContents(Composite parent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Set the provisioning UI that provides the session, policy, and other services
	 * for the UI. This method must be called before the contents are created or it
	 * will have no effect.
	 *
	 * @param ui the provisioning UI to use for this page.
	 */
	public void setProvisioningUI(ProvisioningUI ui) {
	}
}
