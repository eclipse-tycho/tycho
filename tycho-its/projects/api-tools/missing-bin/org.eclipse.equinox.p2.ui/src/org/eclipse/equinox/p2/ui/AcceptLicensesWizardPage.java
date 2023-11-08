/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

/**
 * AcceptLicensesWizardPage shows a list of the IU's that have licenses that
 * have not been approved by the user, and allows the user to approve them.
 *
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class AcceptLicensesWizardPage extends WizardPage {

	class IUWithLicenseParent {
		IInstallableUnit iu;
		ILicense license;

		IUWithLicenseParent(ILicense license, IInstallableUnit iu) {
			this.license = license;
			this.iu = iu;
		}
	}

	class LicenseContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof ILicense))
				return new Object[0];

			if (licensesToIUs.containsKey(parentElement)) {
				List<IInstallableUnit> iusWithLicense = licensesToIUs.get(parentElement);
				IInstallableUnit[] ius = iusWithLicense.toArray(new IInstallableUnit[iusWithLicense.size()]);
				IUWithLicenseParent[] children = new IUWithLicenseParent[ius.length];
				for (int i = 0; i < ius.length; i++) {
					children[i] = new IUWithLicenseParent((ILicense) parentElement, ius[i]);
				}
				return children;
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof IUWithLicenseParent) {
				return ((IUWithLicenseParent) element).license;
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return licensesToIUs.containsKey(element);
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return licensesToIUs.keySet().toArray();
		}

		@Override
		public void dispose() {
			// Nothing to do
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Nothing to do
		}
	}

	class LicenseLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof License) {
				return getFirstLine(((License) element).getBody());
			} else if (element instanceof IUWithLicenseParent) {
				return getIUName(((IUWithLicenseParent) element).iu);
			} else if (element instanceof IInstallableUnit) {
				return getIUName((IInstallableUnit) element);
			}
			return ""; //$NON-NLS-1$
		}

		private String getFirstLine(String body) {
			int i = body.indexOf('\n');
			int j = body.indexOf('\r');
			if (i > 0) {
				if (j > 0)
					return body.substring(0, i < j ? i : j);
				return body.substring(0, i);
			} else if (j > 0) {
				return body.substring(0, j);
			}
			return body;
		}
	}

	TreeViewer iuViewer;
	Text licenseTextBox;
	Button acceptButton;
	Button declineButton;
	SashForm sashForm;
	HashMap<ILicense, List<IInstallableUnit>> licensesToIUs; // License -> IU Name
	private LicenseManager manager;

	static String getIUName(IInstallableUnit iu) {
		StringBuilder buf = new StringBuilder();
		String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		if (name != null)
			buf.append(name);
		else
			buf.append(iu.getId());
		buf.append(" "); //$NON-NLS-1$
		buf.append(iu.getVersion().toString());
		return buf.toString();
	}

	/**
	 * Create a license acceptance page for showing licenses to the user.
	 *
	 * @param manager   the license manager that should be used to check for already
	 *                  accepted licenses. May be <code>null</code>.
	 * @param ius       the IInstallableUnits for which licenses should be checked
	 * @param operation the provisioning operation describing what changes are to
	 *                  take place on the profile
	 */
	public AcceptLicensesWizardPage(LicenseManager manager, IInstallableUnit[] ius, ProfileChangeOperation operation) {
		super("AcceptLicenses"); //$NON-NLS-1$
		setTitle("");
		this.manager = manager;
		update(ius, operation);
	}

	@Override
	public void createControl(Composite parent) {
	}

	void handleSelectionChanged(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			Object selected = selection.getFirstElement();
			if (selected instanceof License)
				licenseTextBox.setText(((License) selected).getBody());
			else if (selected instanceof IUWithLicenseParent)
				licenseTextBox.setText(((IUWithLicenseParent) selected).license.getBody());
		}
	}

	/**
	 * The wizard is finishing. Perform any necessary processing.
	 *
	 * @return <code>true</code> if the finish can proceed, <code>false</code> if it
	 *         should not.
	 */
	public boolean performFinish() {
		rememberAcceptedLicenses();
		return true;
	}

	/**
	 * Return a boolean indicating whether there are licenses that must be accepted
	 * by the user.
	 *
	 * @return <code>true</code> if there are licenses that must be accepted, and
	 *         <code>false</code> if there are no licenses that must be accepted.
	 */
	public boolean hasLicensesToAccept() {
		return licensesToIUs != null && licensesToIUs.size() > 0;
	}

	/**
	 * Update the current page to show the licenses that must be approved for the
	 * selected IUs and the provisioning plan.
	 *
	 * Clients using this page in conjunction with a {@link ProfileChangeOperation}
	 * should instead use
	 * {@link #update(IInstallableUnit[], ProfileChangeOperation)}. This method is
	 * intended for clients who are working with a low-level provisioning plan
	 * rather than an {@link InstallOperation} or {@link UpdateOperation}.
	 *
	 * @param theIUs the installable units to be installed for which licenses must
	 *               be checked
	 * @param plan   the provisioning plan that describes a resolved install
	 *               operation
	 *
	 * @see #update(IInstallableUnit[], ProfileChangeOperation)
	 */

	public void updateForPlan(IInstallableUnit[] theIUs, IProvisioningPlan plan) {
		updateLicenses(theIUs, plan);
	}

	private void updateLicenses(IInstallableUnit[] theIUs, IProvisioningPlan plan) {
		if (theIUs == null)
			licensesToIUs = new HashMap<>();
		else
			findUnacceptedLicenses(theIUs, plan);
		setDescription();
		setPageComplete(licensesToIUs.size() == 0);
		if (getControl() != null) {
			Composite parent = getControl().getParent();
			getControl().dispose();
			iuViewer = null;
			sashForm = null;
			createControl(parent);
			parent.layout(true);
		}
	}

	/**
	 * Update the page for the specified IInstallableUnits and operation.
	 *
	 * @param theIUs    the IInstallableUnits for which licenses should be checked
	 * @param operation the operation describing the pending profile change
	 */
	public void update(IInstallableUnit[] theIUs, ProfileChangeOperation operation) {
		if (operation != null && operation.hasResolved()) {
			int sev = operation.getResolutionResult().getSeverity();
			if (sev != IStatus.ERROR && sev != IStatus.CANCEL) {
				updateLicenses(theIUs, operation.getProvisioningPlan());
			} else {
				updateLicenses(new IInstallableUnit[0], null);
			}
		}
	}

	private void findUnacceptedLicenses(IInstallableUnit[] selectedIUs, IProvisioningPlan plan) {
		IInstallableUnit[] iusToCheck = selectedIUs;
		if (plan != null) {
			iusToCheck = plan.getAdditions().query(QueryUtil.createIUAnyQuery(), null).toArray(IInstallableUnit.class);
		}

		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=218532
		// Current metadata generation can result with a feature group IU and the
		// feature jar IU
		// having the same name and license. We will weed out duplicates if the license
		// and name are both
		// the same.
		licensesToIUs = new HashMap<>();// map of License->ArrayList of IUs with that license
		HashMap<ILicense, HashSet<String>> namesSeen = new HashMap<>(); // map of License->HashSet of names with that
																		// license
		for (IInstallableUnit iu : iusToCheck) {
			for (ILicense license : iu.getLicenses(null)) {
				if (manager != null && !manager.isAccepted(license)) {
					String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
					if (name == null)
						name = iu.getId();
					// Have we already found this license?
					if (licensesToIUs.containsKey(license)) {
						HashSet<String> names = namesSeen.get(license);
						if (!names.contains(name)) {
							names.add(name);
							((ArrayList<IInstallableUnit>) licensesToIUs.get(license)).add(iu);
						}
					} else {
						ArrayList<IInstallableUnit> list = new ArrayList<>(1);
						list.add(iu);
						licensesToIUs.put(license, list);
						HashSet<String> names = new HashSet<>(1);
						names.add(name);
						namesSeen.put(license, names);
					}
				}
			}
		}
	}

	private void rememberAcceptedLicenses() {
		if (licensesToIUs == null || manager == null)
			return;
		for (ILicense license : licensesToIUs.keySet())
			manager.accept(license);
	}

	private void setDescription() {
		// No licenses but the page is open. Shouldn't happen, but just in case...
	}

	/**
	 * Save any settings related to the current size and location of the wizard
	 * page.
	 */
	public void saveBoundsRelatedSettings() {
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && hasLicensesToAccept() && iuViewer != null) {
			iuViewer.setSelection(new StructuredSelection(iuViewer.getTree().getItem(0).getData()), true);
		}
	}
}
