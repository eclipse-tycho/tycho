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
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.sbom.plugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspace;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.eclipse.tycho.osgi.framework.Features;
import org.eclipse.tycho.p2maven.transport.TransportCacheConfig;
import org.osgi.framework.BundleException;

@Mojo(name = "generator", threadSafe = true, requiresProject = false)
public class GeneratorMojo extends AbstractMojo {

	@Component
	private MavenProject project;

	@Component
	private EclipseWorkspaceManager workspaceManager;
	@Component
	private EclipseApplicationManager applicationManager;

	@Component
	private TransportCacheConfig transportCacheConfig;

	@Parameter()
	private Repository generatorRepository;

	@Parameter(property = "sbom.verbose")
	private boolean verbose;

	@Parameter(property = "xml-outputs", defaultValue = "${project.build.directory}")
	private File xmlOutputs;

	@Parameter(property = "json-outputs", defaultValue = "${project.build.directory}")
	private File jsonOutputs;

	@Parameter(property = "index", defaultValue = "${project.build.directory}/index.html")
	private File index;

	@Parameter(property = "cache")
	private File cache;

	@Parameter(property = "installations", required = true)
	private File installations;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Generate SBOM for project " + project.getName());
		MavenRepositoryLocation repository = EclipseApplicationManager.getRepository(generatorRepository,
				URI.create("https://download.eclipse.org/cbi/updates/p2-sbom/products/nightly/"));
		EclipseApplication application = applicationManager.getApplication(repository,
				Bundles.of(), Features.of(), "SBOM Generator");
		application.addProduct("org.eclipse.cbi.p2repo.sbom.cli.product");
		EclipseWorkspace<?> workspace = workspaceManager.getWorkspace(repository.getURL(), this);
		List<String> arguments = new ArrayList<String>();
		arguments.add(EclipseApplication.ARG_APPLICATION);
		arguments.add("org.eclipse.cbi.p2repo.sbom.generator");
		arguments.add("-consoleLog");
		if (verbose) {
			arguments.add("-verbose");
		}
		arguments.add("-xml-outputs");
		String xmlPath = getAbsolutePath(xmlOutputs, true, false);
		getLog().info("XML is written to " + xmlPath);
		arguments.add(xmlPath);
		arguments.add("-json-outputs");
		String jsonPath = getAbsolutePath(jsonOutputs, true, false);
		getLog().info("JSON is written to " + jsonPath);
		arguments.add(jsonPath);
		arguments.add("-index");
		String indexPath = getAbsolutePath(index, false, true);
		getLog().info("Index is written to " + indexPath);
		arguments.add(indexPath);
		arguments.add("-cache");
		File cachePath;
		if (cache == null) {
			cachePath = new File(transportCacheConfig.getCacheLocation(), ".sbom");
		} else {
			cachePath = cache;
		}
		getLog().info("Using cache directory " + cachePath);
		arguments.add(cachePath.getAbsolutePath());
		arguments.add("-installations");
		arguments.add(installations.getAbsolutePath());
		getLog().info("Calling application with arguments: " + arguments);
		try (EclipseFramework framework = application.startFramework(workspace, arguments)) {
			framework.start();
		} catch (BundleException e) {
			throw new MojoFailureException("Can't start framework!", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause.getClass().getName().equals(CoreException.class.getName())
					|| cause.getClass().getName().equals(ProvisionException.class.getName())) {
				throw new MojoFailureException(cause.getMessage(), cause);
			}
			throw new MojoExecutionException(cause);
		} catch (Exception e) {
			throw new MojoExecutionException(e);
		}
	}

	private String getAbsolutePath(File file, boolean createPath, boolean createParent) {
		String absolutePath = file.getAbsolutePath();
		if (absolutePath.contains("${project.basedir}")) {
			// when called from CLI we want to replace placeholders manually
			absolutePath = absolutePath.replace("${project.basedir}", ".");
			file = new File(absolutePath);
		}
		if (createParent) {
			file.getParentFile().mkdirs();
		}
		if (createPath) {
			file.mkdirs();
		}
		return absolutePath;
	}

}
