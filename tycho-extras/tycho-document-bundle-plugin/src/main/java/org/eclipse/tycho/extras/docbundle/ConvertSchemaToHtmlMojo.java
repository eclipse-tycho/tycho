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
import java.net.URL;
import java.util.List;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.extras.docbundle.runner.ConvertSchemaToHtmlResult;
import org.eclipse.tycho.extras.docbundle.runner.ConvertSchemaToHtmlRunner;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspace;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.osgi.framework.BundleException;

/**
 * This mojo provides the functionality of
 * org.eclipse.pde.internal.core.ant.ConvertSchemaToHTML
 */
@Mojo(name = "schema-to-html", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ConvertSchemaToHtmlMojo extends AbstractMojo {

	@Parameter()
	private Repository pdeToolsRepository;

	@Parameter()
	private File manifest;
	@Parameter()
	private List<File> manifests;
	@Parameter()
	private File destination;
	@Parameter()
	private URL cssURL;
	@Parameter()
	private String additionalSearchPaths;

	@Parameter(property = "project")
	private MavenProject project;

	@Component
	private EclipseWorkspaceManager workspaceManager;
	@Component
	private PdeApplicationManager applicationManager;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		MavenRepositoryLocation repository = PdeApplicationManager.getRepository(pdeToolsRepository);
		EclipseApplication application = applicationManager.getApplication(repository);
		EclipseWorkspace<?> workspace = workspaceManager.getWorkspace(repository.getURL(), this);
		try (EclipseFramework framework = application.startFramework(workspace, List.of())) {
			ConvertSchemaToHtmlResult result = framework.execute(new ConvertSchemaToHtmlRunner(getManifestList(),
					destination, cssURL, additionalSearchPaths, project.getBasedir()));
			Log log = getLog();
			result.errors().forEach(log::error);
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

	private List<File> getManifestList() {
		if (manifests != null && !manifests.isEmpty()) {
			return manifests;
		}
		if (manifest != null) {
			return List.of(manifest);
		}
		return List.of();
	}

}
