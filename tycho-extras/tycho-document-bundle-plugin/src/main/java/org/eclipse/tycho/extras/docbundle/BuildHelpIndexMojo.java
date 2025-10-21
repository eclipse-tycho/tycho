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
package org.eclipse.tycho.extras.docbundle;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.extras.docbundle.runner.BuildHelpIndexRunner;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.DefaultEclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspace;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.eclipse.tycho.osgi.framework.Features;
import org.osgi.framework.BundleException;

/**
 * A Mojo to pre-build search help index for a plug-in.
 *
 */
@Mojo(name = "build-help-index", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class BuildHelpIndexMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.basedir}/plugin.xml", required = true)
	private File manifest;

	@Parameter(defaultValue = "${project.build.directory}", required = true)
	private File outputDirectory;

	@Parameter()
	private Repository buildToolsRepository;

	@Component
	private EclipseApplicationManager applicationManager;

	@Component
	private EclipseWorkspaceManager workspaceManager;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (manifest == null || !manifest.exists()) {
			throw new MojoExecutionException("Manifest is not a file: " + manifest);
		}
		MavenRepositoryLocation repository = DefaultEclipseApplicationManager.getRepository(buildToolsRepository);
		EclipseApplication application = applicationManager.getApplication(repository,
				Bundles.of(Bundles.BUNDLE_ECLIPSE_HELP_BASE), Features.of(), "Build Document Index");
		EclipseWorkspace<?> workspace = workspaceManager.getWorkspace(repository.getURL(), this);
		try (EclipseFramework framework = application.startFramework(workspace, List.of())) {
			outputDirectory.mkdirs();
			framework.execute(new BuildHelpIndexRunner(manifest, outputDirectory));
		} catch (BundleException e) {
			throw new MojoFailureException("Can't start framework!", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause.getClass().getName().equals(CoreException.class.getName())) {
				throw new MojoFailureException(cause.getMessage(), cause);
			}
			throw new MojoExecutionException(cause);
		}
	}

}
