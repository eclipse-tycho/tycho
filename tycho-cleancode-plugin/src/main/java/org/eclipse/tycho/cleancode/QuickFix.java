/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.cleancode;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuild;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionRelevance;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.UIJob;

/**
 * Applies the QuickFix with the highest relevance to a warning, because
 * QuickFixes are not really optimized to run in a CI build we need to be very
 * forgiving here and handle errors gracefully.
 */
public class QuickFix extends AbstractEclipseBuild<QuickFixResult> {

	QuickFix(Path projectDir, boolean debug) {
		super(projectDir, debug);
	}

	@Override
	protected QuickFixResult createResult(IProject project) throws Exception {
		QuickFixResult result = new QuickFixResult();
		while (fixOneMarker(project, result)) {
			runInUI("Save Editors", m -> {
				if (PlatformUI.isWorkbenchRunning()) {
					PlatformUI.getWorkbench().saveAllEditors(false);
				}
			});
			debug("### Perform build to update markers ###");
			buildProject(project);
		}
		return result;
	}

	private boolean fixOneMarker(IProject project, QuickFixResult result) throws CoreException {

		debug("### Check for marker with resolutions...");
		IMarker[] markers = getCurrentMarker(project, true);
		for (IMarker marker : markers) {
			if (result.tryFix(marker)) {
				debug("Check Marker: " + getInfo(marker));
				try {
					IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
					debug("\tMarker has " + resolutions.length + " resolutions");
					IMarkerResolution resolution = Arrays.stream(resolutions)
							.max(Comparator.comparingInt(r -> getRelevance(r))).orElse(null);
					if (resolution != null) {
						for (IMarkerResolution r : resolutions) {
							debug(String.format("\t\t- (%d): %s", getRelevance(r), getInfo(r, false)));
						}
						LinkedHashSet<Job> jobs = new LinkedHashSet<>();
						IStatus status = runInUI("fix marker " + getInfo(marker), m -> {
							AbstractEclipseBuild.recordJobs(jobs, m, nil -> {
								debug("\tApply best resolution to marker: " + getInfo(resolution, true));
								resolution.run(marker);
							});
						});
						for (Job job : jobs) {
							debug("Wait for Job '" + job.getName() + "' scheduled during marker resolution...");
							job.join();
						}
						if (status.isOK()) {
							String fix = buildFixMessage(marker);
							debug("\t" + fix);
							result.addFix(fix);
							return true;
						} else {
							debug("\tMarker could not be applied!", status.getException());
						}
					}
				} catch (Throwable t) {
					debug("\tMarker resolutions could not be computed!", t);
				}
			}
		}
		return false;
	}

	private String getInfo(IMarker marker) {
		return marker.getAttribute(IMarker.MESSAGE, "") + " @ " + marker.getResource() + ":"
				+ marker.getAttribute(IMarker.LINE_NUMBER, -1);
	}

	private IMarker[] getCurrentMarker(IProject project, boolean rebuildOnError) throws CoreException {
		IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
				if (rebuildOnError) {
					debug("Found error marker, try rebuild ...");
					buildProject(project);
					return getCurrentMarker(project, false);
				}
				debug("Found error, can't apply fixes: " + getInfo(marker));
				return new IMarker[0];
			}
		}
		return markers;
	}

	private String buildFixMessage(IMarker marker) {
		StringBuilder sb = new StringBuilder(marker.getAttribute(IMarker.MESSAGE, "Unknown Problem"));
		IResource resource = marker.getResource();
		if (resource != null) {
			sb.append(" in ");
			sb.append(resource.getFullPath());
			int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
			if (line > 0) {
				sb.append(" at line ");
				sb.append(line);
			}
		}
		return sb.toString();
	}

	private String getInfo(IMarkerResolution resolution, boolean withDescription) {
		if (withDescription && resolution instanceof IMarkerResolution2 res2) {
			return resolution.getClass().getName() + ": " + getLabel(resolution) + " // " + getDescription(res2);
		} else {
			return resolution.getClass().getName() + ": " + getLabel(resolution);
		}
	}

	private int getRelevance(IMarkerResolution resolution) {
		try {
			if (resolution instanceof IMarkerResolutionRelevance relevance) {
				return relevance.getRelevanceForResolution();
			}
		} catch (RuntimeException e) {
		}
		return 0;
	}

	private String getDescription(IMarkerResolution2 markerResolution) {
		try {
			return markerResolution.getDescription();
		} catch (RuntimeException e) {
			return null;
		}
	}

	private String getLabel(IMarkerResolution resolution) {
		try {
			return resolution.getLabel();
		} catch (RuntimeException e) {
			return resolution.getClass().getName();
		}
	}

	private static IStatus runInUI(String action, ICoreRunnable runnable) throws InterruptedException {
		UIJob job = UIJob.create(action, m -> {
			try {
				runnable.run(m);
			} catch (CoreException e) {
				return e.getStatus();
			} catch (Throwable e) {
				return Status.error("Run failed", e);
			}
			return Status.OK_STATUS;
		});
		job.schedule();
		job.join();
		return job.getResult();
	}

}
