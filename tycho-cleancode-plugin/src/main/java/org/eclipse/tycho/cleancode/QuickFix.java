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
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuild;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionRelevance;
import org.eclipse.ui.ide.IDE;

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
		IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		result.setNumberOfMarker(markers.length);
		for (IMarker marker : markers) {
			debug("Marker: " + marker);
			try {
				IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
				debug("Marker has " + resolutions.length + " resolutions");
				IMarkerResolution resolution = Arrays.stream(resolutions)
						.max(Comparator.comparingInt(r -> getRelevance(r))).orElse(null);
				if (resolution != null) {
					debug("Apply best resolution to marker: " + getInfo(resolution));
					// must use an own thread to make sure it is not called as a job
					AtomicReference<Throwable> error = new AtomicReference<Throwable>();
					Thread thread = new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								resolution.run(marker);
							} catch (Throwable t) {
								error.set(t);
							}
						}
					});
					thread.start();
					thread.join();
					Throwable t = error.get();
					if (t == null) {
						result.addFix(buildFixMessage(marker));
					} else {
						debug("Marker could not be applied!", t);
					}
				}
			} catch (Throwable t) {
				debug("Marker resolutions could not be computed!", t);
			}
		}
		return result;
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

	private String getInfo(IMarkerResolution resolution) {
		if (resolution instanceof IMarkerResolution2 res2) {
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
		return -1;
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

}
