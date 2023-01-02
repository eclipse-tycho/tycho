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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.ds;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.DeclarativeServicesConfiguration;
import org.eclipse.tycho.core.osgitools.AbstractSpecificationClasspathContributor;
import org.osgi.framework.Version;

@Component(role = ClasspathContributor.class, hint = "ds-annotations")
@SessionScoped
public class DeclarativeServicesClasspathContributor extends AbstractSpecificationClasspathContributor
		implements ClasspathContributor {

	private static final String DS_ANNOTATIONS_PACKAGE = "org.osgi.service.component.annotations";

	private static final String DS_ANNOTATIONS_GROUP_ID = "org.osgi";
	private static final String DS_ANNOTATIONS_ARTIFACT_ID = "org.osgi.service.component.annotations";

	@Requirement
	DeclarativeServiceConfigurationReader configurationReader;

	@Inject
	public DeclarativeServicesClasspathContributor(MavenSession session) {
		super(session, DS_ANNOTATIONS_PACKAGE, DS_ANNOTATIONS_GROUP_ID, DS_ANNOTATIONS_ARTIFACT_ID);
	}

	@Override
	protected Version getSpecificationVersion(ReactorProject project) {
		try {
			DeclarativeServicesConfiguration configuration = configurationReader.getConfiguration(project);
			if (configuration != null) {
				return configuration.getSpecificationVersion();
			}
		} catch (IOException e) {
			// can't determine the minimum specification version then...
		}
		return Version.parseVersion(DeclarativeServiceConfigurationReader.DEFAULT_DS_VERSION);
	}

}
