/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
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
 *     Ericsson AB (Hamdan Msheik) - Bug 396420 - Control Install dialog through preference customization
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

/**
 * The Policy class is used to specify application specific policies that should
 * be used in the standard p2 UI class libraries. The default policy is acquired
 * using the OSGi service model.
 *
 * Policy allows clients to specify things such as how repositories are
 * manipulated in the standard wizards and dialogs, and how the repositories or
 * the installation itself should be traversed when displaying content.
 *
 * In some cases, the Policy is used only to define a default value that can be
 * overridden by user choice and subsequently stored in dialog settings.
 *
 * Client applications should ensure that their Policy is registered before any
 * of the p2 UI objects access the default Policy.
 *
 * @since 2.0
 */
public class Policy {

	/**
	 * A constant indicating that restart should be forced (without confirmation)
	 * immediately after completion of a provisioning operation.
	 *
	 */
	public static final int RESTART_POLICY_FORCE = 1;

	/**
	 * A constant indicating that the changes should be applied dynamically to the
	 * profile (without confirmation) immediately after completion of a provisioning
	 * operation.
	 */
	public static final int RESTART_POLICY_FORCE_APPLY = 2;

	/**
	 * A constant indicating that the user should be prompted to restart after
	 * completion of a provisioning operation.
	 */
	public static final int RESTART_POLICY_PROMPT = 3;

	/**
	 * A constant indicating that, where possible, the user should be given the
	 * option to restart or dynamically apply the changes after completion of a
	 * provisioning operation.
	 */
	public static final int RESTART_POLICY_PROMPT_RESTART_OR_APPLY = 4;

	/**
	 * A constant indicating that the user should be presented with an update wizard
	 * that shows a list of IUs that can be updated. Even when only one update is
	 * available, a list showing the single update will be used. This option is
	 * recommended when the user's view of the system is a set of component updates
	 * and the user is expected to have knowledge of the composition of the system.
	 *
	 * @since 2.1
	 */
	public static final int UPDATE_STYLE_MULTIPLE_IUS = 1;

	/**
	 * A constant indicating that the user should be presented with an update wizard
	 * that shows detail about a single IU that can be updated. If more than one IU
	 * can be updated, the user will be shown a list; however, this option is
	 * recommended to be used only when the user's view of the system is a single
	 * application that can be updated, and only one IU is expected to be available
	 * for update.
	 *
	 * @since 2.1
	 */
	public static final int UPDATE_STYLE_SINGLE_IUS = 2;

	private IQuery<IInstallableUnit> visibleAvailableIUQuery = QueryUtil.createIUGroupQuery();
	private IQuery<IInstallableUnit> visibleInstalledIUQuery = new UserVisibleRootQuery();
	private boolean groupByCategory = true;
	private boolean allowDrilldown = true;
	private boolean repositoriesVisible = true;
	private boolean contactAllSites = true;
	private boolean hideAlreadyInstalled = true;
	private boolean showLatestVersionsOnly = true;
	private int restartPolicy = RESTART_POLICY_PROMPT;
	private String repoPrefPageId;
	private String repoPrefPageName;
	private boolean filterOnEnv = false;
	private int updateWizardStyle = UPDATE_STYLE_MULTIPLE_IUS;
	private Point wizardDetailsPreferredSize = null;
	private boolean checkAgainstCurrentExecutionEnvironment;

	/**
	 * Answer a boolean indicating whether the caller should continue to work with
	 * the specified operation. This method is used when an operation has been
	 * resolved, but the UI may have further restrictions on continuing with it.
	 *
	 * @param operation the operation in question. It must already be resolved.
	 * @param shell     the shell to use for any interaction with the user
	 * @return <code>true</code> if processing of the operation should continue,
	 *         <code>false</code> if not. It is up to the implementor to report any
	 *         errors to the user when answering <code>false</code>.
	 */
	public boolean continueWorkingWithOperation(ProfileChangeOperation operation, Shell shell) {
		return true;
	}

	/**
	 * Return a status that can be used to describe the failure to retrieve a
	 * profile.
	 *
	 * @return a status describing a failure to retrieve a profile, or
	 *         <code>null</code> if there is no such status.
	 */
	public IStatus getNoProfileChosenStatus() {
		return null;
	}

	/**
	 * Return a query that can be used to obtain the IInstallableUnits that should
	 * be presented to the user from the software repositories.
	 *
	 * @return the query used to retrieve user visible available IUs
	 */
	public IQuery<IInstallableUnit> getVisibleAvailableIUQuery() {
		return visibleAvailableIUQuery;
	}

	/**
	 * Set the query that can be used to obtain the IInstallableUnits that should be
	 * presented to the user.
	 *
	 * @param query the query used to retrieve user visible available IUs
	 */
	public void setVisibleAvailableIUQuery(IQuery<IInstallableUnit> query) {
		visibleAvailableIUQuery = query;
	}

	/**
	 * Return a query that can be used to obtain the IInstallableUnits in the
	 * profile that should be presented to the user.
	 *
	 * @return the query used to retrieve user visible installed IUs
	 */
	public IQuery<IInstallableUnit> getVisibleInstalledIUQuery() {
		return visibleInstalledIUQuery;
	}

	/**
	 * Set the query that can be used to obtain the IInstallableUnits in the profile
	 * that should be presented to the user.
	 *
	 * @param query the query used to retrieve user visible installed IUs
	 */
	public void setVisibleInstalledIUQuery(IQuery<IInstallableUnit> query) {
		visibleInstalledIUQuery = query;
	}

	/**
	 * Get the restart policy that should be used when the provisioning UI
	 * determines that a restart is required.
	 *
	 * @return an integer constant describing the restart policy
	 *
	 * @see #RESTART_POLICY_FORCE
	 * @see #RESTART_POLICY_FORCE_APPLY
	 * @see #RESTART_POLICY_PROMPT
	 * @see #RESTART_POLICY_PROMPT_RESTART_OR_APPLY
	 */
	public int getRestartPolicy() {
		return restartPolicy;
	}

	/**
	 * Set the restart policy that should be used when the provisioning UI
	 * determines that a restart is required.
	 *
	 * @param restartPolicy an integer constant describing the restart policy
	 *
	 * @see #RESTART_POLICY_FORCE
	 * @see #RESTART_POLICY_FORCE_APPLY
	 * @see #RESTART_POLICY_PROMPT
	 * @see #RESTART_POLICY_PROMPT_RESTART_OR_APPLY
	 */
	public void setRestartPolicy(int restartPolicy) {
		this.restartPolicy = restartPolicy;
	}

	/**
	 * Return a boolean indicating whether the repositories should be visible to the
	 * user, such that the user can add, remove, and otherwise manipulate the
	 * software site list.
	 *
	 * @return <code>true</code> if repositories are visible to the end user,
	 *         <code>false</code> if they are not.
	 */
	public boolean getRepositoriesVisible() {
		return repositoriesVisible;
	}

	/**
	 * Set a boolean indicating whether the repositories should be visible to the
	 * user, such that the user can add, remove, and otherwise manipulate the
	 * software site list.
	 *
	 * @param visible <code>true</code> if repositories are visible to the end user,
	 *                <code>false</code> if they are not.
	 */
	public void setRepositoriesVisible(boolean visible) {
		this.repositoriesVisible = visible;
	}

	/**
	 * Return a boolean indicating whether only the latest versions of updates and
	 * available software should be shown to the user.
	 *
	 * @return <code>true</code> if only the latest versions are shown,
	 *         <code>false</code> if all versions should be shown.
	 */
	public boolean getShowLatestVersionsOnly() {
		return showLatestVersionsOnly;
	}

	/**
	 * Set a boolean indicating whether only the latest versions of updates and
	 * available software should be shown to the user.
	 *
	 * @param showLatest <code>true</code> if only the latest versions are shown,
	 *                   <code>false</code> if all versions should be shown.
	 */
	public void setShowLatestVersionsOnly(boolean showLatest) {
		this.showLatestVersionsOnly = showLatest;
	}

	/**
	 * Return a boolean indicating whether the user should be allowed drill down
	 * from a visible update or installed item into the requirements.
	 *
	 * @return <code>true</code> if drilldown is allowed, <code>false</code> if it
	 *         is not.
	 */
	public boolean getShowDrilldownRequirements() {
		return allowDrilldown;
	}

	/**
	 * Set a boolean indicating whether the user should be allowed drill down from a
	 * visible update or installed item into the requirements.
	 *
	 * @param drilldown <code>true</code> if drilldown is allowed,
	 *                  <code>false</code> if it is not.
	 */
	public void setShowDrilldownRequirements(boolean drilldown) {
		this.allowDrilldown = drilldown;
	}

	/**
	 * Return a boolean value indicating whether or not the list of available
	 * software should be filtered based on the environment settings of the profile.
	 *
	 * @return <code>true</code> if the results should be filtered and
	 *         <code>false</code> otherwise.
	 * @since 2.1
	 */
	public boolean getFilterOnEnv() {
		return filterOnEnv;
	}

	/**
	 * Set a boolean value indicating whether or not the list of available software
	 * should be filtered based on the environment settings of the profile.
	 *
	 * @param filterOnEnv <code>true</code> if the results should be filtered and
	 *                    <code>false</code> otherwise.
	 * @since 2.1
	 */
	public void setFilterOnEnv(boolean filterOnEnv) {
		this.filterOnEnv = filterOnEnv;
	}

	/**
	 * Return a boolean indicating whether available software should be grouped by
	 * category.
	 *
	 * @return <code>true</code> if items should be grouped by category,
	 *         <code>false</code> if categories should not be shown.
	 */
	public boolean getGroupByCategory() {
		return groupByCategory;
	}

	/**
	 * Set a boolean indicating whether available software should be grouped by
	 * category.
	 *
	 * @param group <code>true</code> if items should be grouped by category,
	 *              <code>false</code> if categories should not be shown.
	 */
	public void setGroupByCategory(boolean group) {
		this.groupByCategory = group;
	}

	/**
	 * Get the id of the preference page that should be used to link to the software
	 * sites page.
	 *
	 * @return the preference page id, or <code>null</code> if there is no
	 *         preference page id showing the software sites.
	 */
	public String getRepositoryPreferencePageId() {
		return repoPrefPageId;
	}

	/**
	 * Set the id of the preference page that should be used to link to the software
	 * sites page.
	 *
	 * @param id the preference page id, or <code>null</code> if there is no
	 *           preference page id showing the software sites.
	 */

	public void setRepositoryPreferencePageId(String id) {
		this.repoPrefPageId = id;
	}

	/**
	 * Get the localized name of the preference page that should be displayed in
	 * links to the software sites page.
	 *
	 * @return the preference page name, or <code>null</code> if there is no
	 *         preference page.
	 */
	public String getRepositoryPreferencePageName() {
		return repoPrefPageName;
	}

	/**
	 * Set the localized name of the preference page that should be displayed in
	 * links to the software sites page. This name is ignored if no id is specified
	 * for the preference page.
	 *
	 * @param name the preference page name, or <code>null</code> if there is no
	 *             preference page.
	 *
	 * @see Policy#setRepositoryPreferencePageId(String)
	 */

	public void setRepositoryPreferencePageName(String name) {
		this.repoPrefPageName = name;
	}

	/**
	 * Get the update wizard style that should be used to determine what to show the
	 * user when updates are available.
	 *
	 * @return an integer constant describing the update wizard style
	 *
	 * @see #UPDATE_STYLE_SINGLE_IUS
	 * @see #UPDATE_STYLE_MULTIPLE_IUS
	 *
	 * @since 2.1
	 */

	public int getUpdateWizardStyle() {
		return updateWizardStyle;
	}

	/**
	 * Get the update wizard style that should be used to determine what to show the
	 * user when updates are available.
	 *
	 * @param updateWizardStyle an integer constant describing the update wizard
	 *                          style
	 *
	 * @see #UPDATE_STYLE_SINGLE_IUS
	 * @see #UPDATE_STYLE_MULTIPLE_IUS
	 *
	 * @since 2.1
	 */

	public void setUpdateWizardStyle(int updateWizardStyle) {
		this.updateWizardStyle = updateWizardStyle;
	}

	/**
	 * Get a point describing the preferred size of the details area shown in single
	 * IU update wizards. This value may be null, in which case the wizard may
	 * compute a default size.
	 *
	 * @return a Point describing the preferred size of the update wizard details
	 *         area.
	 *
	 * @see #UPDATE_STYLE_SINGLE_IUS
	 *
	 * @since 2.1
	 */

	public Point getUpdateDetailsPreferredSize() {
		return wizardDetailsPreferredSize;
	}

	/**
	 * Set the preferred size of the details area shown in update wizards which
	 * notify the user of a single update. Clients can use this value to ensure that
	 * their product's branded update notifier is sized to fit the expected content.
	 *
	 * @param preferredSize a Point describing the preferred size
	 *
	 * @see #UPDATE_STYLE_SINGLE_IUS
	 *
	 * @since 2.1
	 */

	public void setUpdateDetailsPreferredSize(Point preferredSize) {
		this.wizardDetailsPreferredSize = preferredSize;
	}

	/**
	 * Get a boolean value indicating whether to contact all sites.
	 *
	 * @return <code>true</code> true if all sites need to be contacted,
	 *         <code>false</code> otherwise.
	 *
	 * @since 2.3
	 */
	public boolean getContactAllSites() {
		return this.contactAllSites;
	}

	/**
	 * Set a boolean indicating whether all sites need to be contacted.
	 *
	 * @param contactAll <code>true</code> if all sites need to be contacted,
	 *                   <code>false</code> otherwise.
	 *
	 * @since 2.3
	 */
	public void setContactAllSites(boolean contactAll) {
		this.contactAllSites = contactAll;
	}

	/**
	 * Get a boolean value indicating whether to hide already installed units.
	 *
	 * @return <code>true</code> if already installed units are to be hidden,
	 *         <code>false</code> otherwise.
	 *
	 * @since 2.3
	 */
	public boolean getHideAlreadyInstalled() {
		return this.hideAlreadyInstalled;
	}

	/**
	 * Set a boolean indicating whether to hide already installed units.
	 *
	 * @param hide <code>true</code> if already installed units need to be hidden,
	 *             <code>false</code> otherwise.
	 *
	 * @since 2.3
	 */
	public void setHideAlreadyInstalled(boolean hide) {
		this.hideAlreadyInstalled = hide;
	}

	/**
	 * Get a boolean value indicating whether the provisioning operation should
	 * check compatibility with current execution environment
	 *
	 * @return <code>true</code> if operation should check compatibility with
	 *         current execution environment, <code>false</code> otherwise.
	 *
	 * @since 2.6
	 */
	public boolean getCheckAgainstCurrentExecutionEnvironment() {
		return this.checkAgainstCurrentExecutionEnvironment;
	}

	/**
	 * Set a boolean indicating whether the provisioning operation should check
	 * compatibility with current execution environment.
	 *
	 * @param check <code>true</code> if operation should check compatibility with
	 *              current execution environment, <code>false</code> otherwise.
	 *
	 * @since 2.6
	 */
	public void setCheckAgainstCurrentExecutionEnvironment(boolean check) {
		this.checkAgainstCurrentExecutionEnvironment = check;
	}
}
