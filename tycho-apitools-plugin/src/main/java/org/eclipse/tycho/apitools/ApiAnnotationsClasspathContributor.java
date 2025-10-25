/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.apitools;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.AbstractSpecificationClasspathContributor;
import org.eclipse.tycho.model.project.EclipseProject;
import org.osgi.framework.VersionRange;

@Named("apitools-annotations")
@SessionScoped
public class ApiAnnotationsClasspathContributor extends AbstractSpecificationClasspathContributor {

	private static final String PACKAGE_NAME = "org.eclipse.pde.api.tools.annotations";
	private static final String GROUP_ID = "org.eclipse.pde";
	private static final String ARTIFACT_ID = "org.eclipse.pde.api.tools.annotations";
	private static final VersionRange VERSION = new VersionRange("[1,2)");
	private TychoProjectManager projectManager;

	@Inject
	public ApiAnnotationsClasspathContributor(MavenSession session, TychoProjectManager projectManager) {
		super(session, PACKAGE_NAME, GROUP_ID, ARTIFACT_ID);
		this.projectManager = projectManager;
	}

	@Override
	protected VersionRange getSpecificationVersion(MavenProject project) {
		return VERSION;
	}

	@Override
	protected boolean isValidProject(MavenProject project) {
		Optional<EclipseProject> eclipseProject = projectManager.getEclipseProject(project);
		if (eclipseProject.isPresent()) {
			return eclipseProject.get().hasNature(ApiPlugin.NATURE_ID);
		}
		return false;
	}

}
