/*******************************************************************************
 *  Copyright (c) 2008, 2020 IBM Corporation and others.
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
 *     Sonatype, Inc. - ongoing development
 *     Red Hat,Inc. - filter installed softwares
 *******************************************************************************/

package org.eclipse.equinox.p2.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.about.InstallationPage;
import org.eclipse.ui.menus.AbstractContributionFactory;

/**
 * InstalledSoftwarePage displays a profile's IInstallableUnits in an
 * Installation Page. Clients can use this class as the implementation class for
 * an installationPages extension.
 *
 * @see InstallationPage
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.0
 *
 */
public class InstalledSoftwarePage extends InstallationPage implements ICopyable {

	private static final int UPDATE_ID = IDialogConstants.CLIENT_ID;
	private static final int UNINSTALL_ID = IDialogConstants.CLIENT_ID + 1;
	private static final int PROPERTIES_ID = IDialogConstants.CLIENT_ID + 2;
	private static final String BUTTON_ACTION = "org.eclipse.equinox.p2.ui.buttonAction"; //$NON-NLS-1$

	AbstractContributionFactory factory;
	Text detailsArea;
	String profileId;
	Button updateButton, uninstallButton, propertiesButton;
	ProvisioningUI ui;

	@Override
	public void createControl(Composite parent) {
	}

	@Override
	public void createPageButtons(Composite parent) {
	}

	void updateDetailsArea() {
	}

	void updateEnablement() {
	}

	@Override
	public void copyToClipboard(Control activeControl) {
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case UPDATE_ID:
			((Action) updateButton.getData(BUTTON_ACTION)).run();
			break;
		case UNINSTALL_ID:
			((Action) uninstallButton.getData(BUTTON_ACTION)).run();
			break;
		case PROPERTIES_ID:
			((Action) propertiesButton.getData(BUTTON_ACTION)).run();
			break;
		default:
			super.buttonPressed(buttonId);
			break;
		}
	}

	ProvisioningUI getProvisioningUI() {
		// if a UI has not been set then assume that the current default UI is the right
		// thing
		if (ui == null)
			return ui = ProvisioningUI.getDefaultUI();
		return ui;
	}

	/**
	 * Set the provisioning UI to use with this page
	 *
	 * @param value the provisioning ui to use
	 * @since 2.1
	 */
	public void setProvisioningUI(ProvisioningUI value) {
		ui = value;
	}

}
