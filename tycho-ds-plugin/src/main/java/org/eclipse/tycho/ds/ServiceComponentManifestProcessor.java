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

import java.util.jar.Manifest;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.packaging.ManifestProcessor;

@Named("scr")
@Singleton
public class ServiceComponentManifestProcessor implements ManifestProcessor {

	@Override
	public void processManifest(MavenProject mavenProject, Manifest manifest) {
		ReactorProject project = DefaultReactorProject.adapt(mavenProject);
		String header = (String) project.getContextValue(DeclarativeServicesMojo.CONTEXT_KEY_MANIFEST_HEADER);
		if (header != null) {
			manifest.getMainAttributes().putValue(DeclarativeServicesMojo.SERVICE_COMPONENT_HEADER, header);
		}
	}

}
