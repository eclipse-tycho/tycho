/*******************************************************************************
 * Copyright (c) 2009,2011 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.p2.ui;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * A job that loads a set of metadata repositories and caches the loaded
 * repositories. This job can be used when repositories are loaded by a client
 * who wishes to maintain (and pass along) the in-memory references to the
 * repositories. For example, repositories can be loaded in the background and
 * then passed to another component, thus ensuring that the repositories remain
 * loaded in memory.
 *
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class LoadMetadataRepositoryJob extends ProvisioningJob {

	/**
	 * An object representing the family of jobs that load repositories.
	 */
	public static final Object LOAD_FAMILY = new Object();

	/**
	 * The key that should be used to set a property on a repository load job to
	 * indicate that authentication should be suppressed when loading the
	 * repositories.
	 */
	public static final QualifiedName SUPPRESS_AUTHENTICATION_JOB_MARKER = new QualifiedName("",
			"SUPPRESS_AUTHENTICATION_REQUESTS"); //$NON-NLS-1$

	/**
	 * The key that should be used to set a property on a repository load job to
	 * indicate that repository events triggered by this job should be suppressed so
	 * that clients will ignore all events related to the load.
	 */
	public static final QualifiedName SUPPRESS_REPOSITORY_EVENTS = new QualifiedName("", "SUPRESS_REPOSITORY_EVENTS"); //$NON-NLS-2$

	/**
	 * The key that should be used to set a property on a repository load job to
	 * indicate that a wizard receiving this job needs to schedule it. In some
	 * cases, a load job is finished before invoking a wizard. In other cases, the
	 * job has not yet been scheduled so that listeners can be set up first.
	 */
	public static final QualifiedName WIZARD_CLIENT_SHOULD_SCHEDULE = new QualifiedName("",
			"WIZARD_CLIENT_SHOULD_SCHEDULE"); //$NON-NLS-1$

	/**
	 * The key that should be used to set a property on a repository load job to
	 * indicate that load errors should be accumulated into a single status rather
	 * than reported as they occur.
	 */
	public static final QualifiedName ACCUMULATE_LOAD_ERRORS = new QualifiedName("", "ACCUMULATE_LOAD_ERRORS"); //$NON-NLS-2$

	private MultiStatus accumulatedStatus;
	private ProvisioningUI ui;

	/**
	 * Create a job that loads the metadata repositories known by the specified
	 * RepositoryTracker.
	 *
	 * @param ui the ProvisioningUI providing the necessary services
	 */
	public LoadMetadataRepositoryJob(ProvisioningUI ui) {
		super("", ui.getSession());
		this.ui = ui;
	}

	@Override
	public IStatus runModal(IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	protected boolean shouldAccumulateFailures() {
		return getProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS) != null;
	}

	/**
	 * Report the accumulated status for repository load failures. If there has been
	 * no status accumulated, or if the job has been cancelled, do not report
	 * anything. Detailed errors have already been logged.
	 */
	public void reportAccumulatedStatus() {
		IStatus status = getCurrentStatus();
		if (status.isOK() || status.getSeverity() == IStatus.CANCEL)
			return;

		// If user is unaware of individual sites, nothing to report here.
		if (!ui.getPolicy().getRepositoriesVisible())
			return;
		StatusManager.getManager().handle(status, StatusManager.SHOW);
		// Reset the accumulated status so that next time we only report the newly not
		// found repos.
		accumulatedStatus = null;
	}

	private IStatus getCurrentStatus() {
		if (accumulatedStatus != null) {
			// If there is only missing repo to report, use the specific message rather than
			// the generic.
			if (accumulatedStatus.getChildren().length == 1)
				return accumulatedStatus.getChildren()[0];
			return accumulatedStatus;
		}
		return Status.OK_STATUS;
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == LOAD_FAMILY;
	}
}
