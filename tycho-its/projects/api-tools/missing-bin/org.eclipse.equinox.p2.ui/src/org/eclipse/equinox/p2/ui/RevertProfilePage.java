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
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.about.InstallationPage;

/**
 * RevertProfilePage displays a profile's configuration history in an
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
public class RevertProfilePage extends InstallationPage implements ICopyable {

	/**
	 * Set the provisioning UI to use with this page
	 *
	 * @param value the provisioning ui to use
	 * @since 2.1
	 */
	public void setProvisioningUI(ProvisioningUI value) {
	}

	@Override
	public void createControl(Composite parent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void copyToClipboard(Control activeControl) {
		// TODO Auto-generated method stub

	}
}
