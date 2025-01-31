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
package org.eclipse.tycho.bnd.mojos;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.SubProject;
import aQute.bnd.osgi.Jar;

@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndBuildMojo extends AbstractBndProjectMojo {

	@Component
	MavenProjectHelper helper;

	@Override
	protected void execute(Project project) throws Exception {
		List<SubProject> subProjects = project.getSubProjects();
		if (subProjects.isEmpty()) {
			getLog().info(String.format("Building bundle '%s'", project.getName()));
			File[] files = project.build();
			if (files != null && files.length > 0) {
				Artifact artifact = mavenProject.getArtifact();
				artifact.setFile(files[0]);
			}
		} else {
			for (SubProject subProject : subProjects) {
				ProjectBuilder builder = subProject.getProjectBuilder();
				getLog().info(String.format("Building sub bundle '%s'", subProject.getName()));
				Jar jar = builder.build();
				checkResult(builder, project.getWorkspace().isFailOk());
				String jarName = jar.getName();
				String name = subProject.getName();
				File file = new File(mavenProject.getBuild().getDirectory(), String.format("%s.jar", jarName));
				file.getParentFile().mkdirs();
				jar.write(file);
				helper.attachArtifact(mavenProject, "jar", name, file);
			}
		}

	}

}
