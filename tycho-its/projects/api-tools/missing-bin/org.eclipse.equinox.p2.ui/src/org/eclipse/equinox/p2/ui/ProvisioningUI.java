/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
 *     Sonatype, Inc. - ongoing development
 *     Red Hat Inc. - Bug 460967
 ******************************************************************************/

package org.eclipse.equinox.p2.ui;

import java.net.URI;
import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.swt.widgets.Shell;

/**
 * ProvisioningUI defines the provisioning session, UI policy, and related
 * services for a provisioning UI.
 *
 * @since 2.0
 */
public class ProvisioningUI {

	/**
	 * Return the default ProvisioningUI.
	 *
	 * @return the default Provisioning UI.
	 */
	public static ProvisioningUI getDefaultUI() {
		return null;
	}

	private Policy policy;
	private ProvisioningSession session;
	private String profileId;

	/**
	 * Creates a new instance of the provisioning user interface.
	 *
	 * @param session   The current provisioning session
	 * @param profileId The profile that this user interface is operating on
	 * @param policy    The user interface policy settings to use
	 */
	public ProvisioningUI(ProvisioningSession session, String profileId, Policy policy) {
	}

	/**
	 * Return the UI policy used for this instance of the UI.
	 *
	 * @return the UI policy, must not be <code>null</code>
	 */
	public Policy getPolicy() {
		return policy;
	}

	/**
	 * Return the provisioning session that should be used to obtain provisioning
	 * services.
	 *
	 * @return the provisioning session, must not be <code>null</code>
	 */
	public ProvisioningSession getSession() {
		return session;
	}

	/**
	 * Return the license manager that should be used to remember accepted user
	 * licenses.
	 *
	 * @return the license manager. May be <code>null</code> if licenses are not to
	 *         be remembered.
	 */
	public LicenseManager getLicenseManager() {
		return null;
	}

	/**
	 * Return the repository tracker that should be used to add, remove, and track
	 * the statuses of known repositories.
	 *
	 * @return the repository tracker, must not be <code>null</code>
	 */
	public RepositoryTracker getRepositoryTracker() {
		return session.getProvisioningAgent().getService(RepositoryTracker.class);
	}

	/**
	 * Return the profile id that should be assumed for this ProvisioningUI if no
	 * other id is otherwise specified. Some UI classes are assigned a profile id,
	 * while others are not. For those classes that are not assigned a current
	 * profile id, this id can be used to obtain one.
	 *
	 * @return a profile id
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * Return an install operation that describes installing the specified
	 * IInstallableUnits from the provided list of repositories.
	 *
	 * @param iusToInstall the IInstallableUnits to be installed
	 * @param repositories the repositories to use for the operation
	 * @return the install operation
	 */
	public InstallOperation getInstallOperation(Collection<IInstallableUnit> iusToInstall, URI[] repositories) {
		InstallOperation op = new InstallOperation(getSession(), iusToInstall);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	/**
	 * Return an update operation that describes updating the specified
	 * IInstallableUnits from the provided list of repositories.
	 *
	 * @param iusToUpdate  the IInstallableUnits to be updated
	 * @param repositories the repositories to use for the operation
	 * @return the update operation
	 */
	public UpdateOperation getUpdateOperation(Collection<IInstallableUnit> iusToUpdate, URI[] repositories) {
		UpdateOperation op = new UpdateOperation(getSession(), iusToUpdate);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	/**
	 * Return an uninstall operation that describes uninstalling the specified
	 * IInstallableUnits, using the supplied repositories to replace any metadata
	 * that must be retrieved for the uninstall.
	 *
	 * @param iusToUninstall the IInstallableUnits to be installed
	 * @param repositories   the repositories to use for the operation
	 * @return the uninstall operation
	 */
	public UninstallOperation getUninstallOperation(Collection<IInstallableUnit> iusToUninstall, URI[] repositories) {
		UninstallOperation op = new UninstallOperation(getSession(), iusToUninstall);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	private ProvisioningContext makeProvisioningContext(URI[] repos) {
		if (repos != null) {
			ProvisioningContext context = new ProvisioningContext(getSession().getProvisioningAgent());
			context.setMetadataRepositories(repos);
			context.setArtifactRepositories(repos);
			return context;
		}
		// look everywhere
		return new ProvisioningContext(getSession().getProvisioningAgent());
	}

	/**
	 * Open an install wizard for installing the specified IInstallableUnits
	 *
	 * @param initialSelections the IInstallableUnits that should be selected when
	 *                          the wizard opens. May be <code>null</code>.
	 * @param operation         the operation describing the proposed install. If
	 *                          this operation is not <code>null</code>, then a
	 *                          wizard showing only the IInstallableUnits described
	 *                          in the operation will be shown. If the operation is
	 *                          <code>null</code>, then a wizard allowing the user
	 *                          to browse the repositories will be opened.
	 * @param job               a repository load job that is loading or has already
	 *                          loaded the repositories. Can be used to pass along
	 *                          an in-memory repository reference to the wizard.
	 *
	 * @return the wizard return code
	 */
	public int openInstallWizard(Collection<IInstallableUnit> initialSelections, InstallOperation operation,
			LoadMetadataRepositoryJob job) {
		return openInstallWizard(initialSelections, operation, null, job);
	}

	/**
	 * Open an install wizard for installing the specified IInstallableUnits and
	 * remediationOperation.
	 *
	 * @param initialSelections    the IInstallableUnits that should be selected
	 *                             when the wizard opens. May be <code>null</code>.
	 * @param operation            the operation describing the proposed install. If
	 *                             this operation is not <code>null</code>, then a
	 *                             wizard showing only the IInstallableUnits
	 *                             described in the operation will be shown. If the
	 *                             operation is <code>null</code>, then a wizard
	 *                             allowing the user to browse the repositories will
	 *                             be opened.
	 * @param remediationOperation the alternate operations if the proposed update
	 *                             failed. May be <code>null</code>.
	 * @param job                  a repository load job that is loading or has
	 *                             already loaded the repositories. Can be used to
	 *                             pass along an in-memory repository reference to
	 *                             the wizard.
	 *
	 * @return the wizard return code
	 * @see RemediationOperation
	 * @since 2.3
	 */
	public int openInstallWizard(Collection<IInstallableUnit> initialSelections, InstallOperation operation,
			RemediationOperation remediationOperation, LoadMetadataRepositoryJob job) {
		return 0;
	}

	/**
	 * Open an update wizard for the specified update operation.
	 *
	 * @param skipSelectionsPage <code>true</code> if the selection page should be
	 *                           skipped so that the user is viewing the resolution
	 *                           results. <code>false</code> if the update selection
	 *                           page should be shown first.
	 * @param operation          the operation describing the proposed update. Must
	 *                           not be <code>null</code>.
	 * @param job                a repository load job that is loading or has
	 *                           already loaded the repositories. Can be used to
	 *                           pass along an in-memory repository reference to the
	 *                           wizard.
	 *
	 * @return the wizard return code
	 */
	public int openUpdateWizard(boolean skipSelectionsPage, UpdateOperation operation, LoadMetadataRepositoryJob job) {
		return openUpdateWizard(skipSelectionsPage, operation, null, job);

	}

	/**
	 * Open an update wizard for the specified update operation and
	 * remediationOperation.
	 *
	 * @param skipSelectionsPage   <code>true</code> if the selection page should be
	 *                             skipped so that the user is viewing the
	 *                             resolution results. <code>false</code> if the
	 *                             update selection page should be shown first.
	 * @param operation            the operation describing the proposed update.
	 *                             Must not be <code>null</code>.
	 * @param remediationOperation the alternate operations if the proposed update
	 *                             failed. May be <code>null</code>.
	 * @param job                  a repository load job that is loading or has
	 *                             already loaded the repositories. Can be used to
	 *                             pass along an in-memory repository reference to
	 *                             the wizard.
	 *
	 * @return the wizard return code
	 * @since 2.3
	 */
	public int openUpdateWizard(boolean skipSelectionsPage, UpdateOperation operation,
			RemediationOperation remediationOperation, LoadMetadataRepositoryJob job) {
		return 0;

	}

	/**
	 * Open an uninstall wizard for the specified uninstall operation.
	 *
	 * @param initialSelections the IInstallableUnits that should be selected when
	 *                          the wizard opens. May be <code>null</code>.
	 * @param operation         the operation describing the proposed uninstall.
	 *                          Must not be <code>null</code>.
	 * @param job               a repository load job that is loading or has already
	 *                          loaded the repositories. Can be used to pass along
	 *                          an in-memory repository reference to the wizard.
	 *
	 * @return the wizard return code
	 */
	public int openUninstallWizard(Collection<IInstallableUnit> initialSelections, UninstallOperation operation,
			LoadMetadataRepositoryJob job) {
		return 0;
	}

	/**
	 * Open a UI that allows the user to manipulate the repositories.
	 *
	 * @param shell the shell that should parent the UI
	 */
	public void manipulateRepositories(Shell shell) {
	}

	/**
	 * Schedule a job to execute the supplied ProvisioningOperation.
	 *
	 * @param job        The operation to execute
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public void schedule(final ProvisioningJob job, final int errorStyle) {
	}

	/**
	 * Manage the supplied job as a provisioning operation. This will allow the
	 * ProvisioningUI to be aware that a provisioning job is running, as well as
	 * manage the restart behavior for the job.
	 *
	 * @param job              the job to be managed
	 * @param jobRestartPolicy an integer constant specifying whether the supplied
	 *                         job should cause a restart of the system. The UI
	 *                         Policy's restart policy is used in conjunction with
	 *                         this constant to determine what actually occurs when
	 *                         a job completes.
	 *
	 * @see ProvisioningJob#RESTART_NONE
	 * @see ProvisioningJob#RESTART_ONLY
	 * @see ProvisioningJob#RESTART_OR_APPLY
	 */
	public void manageJob(Job job, final int jobRestartPolicy) {
	}

	/**
	 * Return a boolean indicating whether the receiver has scheduled any operations
	 * for the profile under management.
	 *
	 * @return <code>true</code> if other provisioning operations have been
	 *         scheduled, <code>false</code> if there are no operations scheduled.
	 */
	public boolean hasScheduledOperations() {
		return getSession().hasScheduledOperationsFor(profileId);
	}

	/**
	 * This method is for automated testing only.
	 *
	 * @return the provisioning operation that can suppress restart for automated
	 *         testing.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public ProvisioningOperationRunner getOperationRunner() {
		return null;
	}

	/**
	 * Signal that a repository operation is about to begin. This allows clients to
	 * ignore intermediate events until the operation is completed. Callers are
	 * responsible for ensuring that a corresponding operation ending event is
	 * signaled.
	 */
	public void signalRepositoryOperationStart() {
	}

	/**
	 * Signal that a repository operation has completed.
	 *
	 * @param event  a {@link RepositoryEvent} that describes the overall operation.
	 *               May be <code>null</code>, which indicates that there was no
	 *               single event that can describe the operation.
	 * @param update <code>true</code> if the event should be reflected in the UI,
	 *               false if it should be ignored.
	 */
	public void signalRepositoryOperationComplete(RepositoryEvent event, boolean update) {
	}

	/**
	 * Load the specified metadata repository, signaling a repository operation
	 * start event before loading, and a repository operation complete event after
	 * loading.
	 *
	 * @param location the location of the repository
	 * @param notify   <code>true</code> if the UI should be updated as a result of
	 *                 the load, <code>false</code> if it should not
	 * @param monitor  the progress monitor to be used
	 * @return the repository
	 * @throws ProvisionException if the repository could not be loaded
	 */

	public IMetadataRepository loadMetadataRepository(URI location, boolean notify, IProgressMonitor monitor)
			throws ProvisionException {
		IMetadataRepository repo = null;
		return repo;
	}

	/**
	 * Load the specified artifact repository, signaling a repository operation
	 * start event before loading, and a repository operation complete event after
	 * loading.
	 *
	 * @param location the location of the repository
	 * @param update   <code>true</code> if the UI should be updated as a result of
	 *                 the load, <code>false</code> if it should not
	 * @param monitor  the progress monitor to be used
	 * @return the repository
	 * @throws ProvisionException if the repository could not be loaded
	 */
	public IArtifactRepository loadArtifactRepository(URI location, boolean update, IProgressMonitor monitor)
			throws ProvisionException {
		return null;
	}
}
