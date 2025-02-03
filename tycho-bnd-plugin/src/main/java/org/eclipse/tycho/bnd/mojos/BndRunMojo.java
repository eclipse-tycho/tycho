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
 *******************************************************************************/
package org.eclipse.tycho.bnd.mojos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.tycho.bnd.BndRunFile;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.exporter.executable.ExecutableJarExporter;
import aQute.bnd.exporter.runbundles.RunbundlesExporter;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.ResolveProcess;

@Mojo(name = BndRunMojo.NAME, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndRunMojo extends AbstractBndMojo {

	public static final String NAME = "run";

	public static final String BNDRUN_EXPORTS_NAME = "exports";

	public static final String BNDRUN_EXPORTS_PROPERTY = "bndrun.exports";

	public static final String BNDRUN = ".bndrun";

	@Parameter(name = BNDRUN_EXPORTS_NAME, property = BNDRUN_EXPORTS_PROPERTY)
	private Set<String> exports;

	@Parameter
	private Map<String, String> options;

	@Parameter(property = "bndrun.exporter", defaultValue = ExecutableJarExporter.EXECUTABLE_JAR)
	private String exporter;

	@Parameter(property = "bndrun.attatch", defaultValue = "true")
	private boolean attatch;

	@Parameter(property = "bndrun.resolve", defaultValue = "false")
	private boolean resolve;

	@Component
	MavenProjectHelper helper;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (exports == null || exports.isEmpty()) {
			return;
		}
		List<BndRunFile> bndRuns = getBndRuns(mavenProject.getBasedir().toPath(), exports);
		if (bndRuns.isEmpty()) {
			return;
		}
		Workspace workspace = getWorkspace();
		checkResult(workspace, workspace.isFailOk());
		for (BndRunFile bndRunFile : bndRuns) {
			getLog().info(String.format("Exporting %s ...", bndRunFile.name()));
			try (Bndrun run = getBndRun(workspace, bndRunFile)) {
				if (resolve) {
					try {
						getLog().info(String.format("Resolve %s...", bndRunFile.name()));
						resolveRun(run);
					} catch (ResolutionException e) {
						String msg = ResolveProcess.format(e, false);
						getLog().error(msg);
						throw new MojoFailureException("resolve bnd run failed: " + msg, e);
					}
				}
				Path export = export(run, bndRunFile);
				if (export != null) {
					getLog().info(String.format("Exported to %s", export));
					if (attatch && Files.isRegularFile(export)) {
						helper.attachArtifact(mavenProject, "jar", bndRunFile.name(), export.toFile());
					}
				}
			} catch (IOException e) {
				getLog().error("failed to close Bndrun", e);
			}
		}
	}

	private void resolveRun(Bndrun run) throws ResolutionException, MojoFailureException {
		try {
			String runBundles = run.resolve(false, false);
			if (runBundles != null && run.isOk()) {
				run.setProperty(Constants.RUNBUNDLES, runBundles);
				Collection<Container> collection = run.getRunbundles();
				if (collection.size() > 0) {
					String collect = collection.stream()
							.map(cont -> cont.getBundleSymbolicName() + " " + cont.getVersion())
							.collect(Collectors.joining(System.lineSeparator() + "\t"));
					getLog().info(
							String.format("%s:%s\t%s", Constants.RUNBUNDLES, System.lineSeparator(), collect));
				}
			}
		} catch (ResolutionException re) {
			throw re;
		} catch (Exception e) {
			throw new MojoFailureException("Error resolving bnd run", e);
		}

	}

	private Path export(Bndrun run, BndRunFile bndRunFile) throws MojoFailureException {
		try {
			Entry<String, Resource> result = run.export(exporter, options);
			try (Resource export = result.getValue()) {
				Path exportBasePath = Path.of(mavenProject.getBuild().getDirectory()).resolve(getOutputFolder());
				Files.createDirectories(exportBasePath);
				if (exporter.equals(RunbundlesExporter.RUNBUNDLES)) {
					if (export instanceof JarResource jar) {
						Path exportPath = exportBasePath.resolve(bndRunFile.name());
						Files.createDirectories(exportPath);
						jar.getJar().writeFolder(exportPath.toFile());
						return exportPath;
					}
					return null;
				} else {
					Path exportPath = exportBasePath.resolve(result.getKey());
					export.write(exportPath);
					Files.setLastModifiedTime(exportPath, FileTime.fromMillis(export.lastModified()));
					return exportPath;
				}
			}
		} catch (Exception e) {
			throw new MojoFailureException("error exporting bnd run!", e);
		}
	}

	private String getOutputFolder() {
		if (exporter.equals(RunbundlesExporter.RUNBUNDLES)) {
			return "runbundles";
		} else if (exporter.equals(ExecutableJarExporter.EXECUTABLE_JAR)) {
			return "executable";
		}
		return "export";
	}

	private Bndrun getBndRun(Workspace workspace, BndRunFile run) throws MojoFailureException {
		Bndrun bndRun;
		try {
			bndRun = Bndrun.createBndrun(workspace, run.path().toFile());
			Project project = workspace.getProject(mavenProject.getBasedir().getName());
			if (project != null) {
				bndRun.setParent(project);
			}
		} catch (Exception e) {
			throw new MojoFailureException("error creating bnd run!", e);
		}
		checkResult(bndRun, workspace.isFailOk());
		return bndRun;
	}

	public static List<BndRunFile> getBndRuns(Path path, Collection<String> match) throws MojoExecutionException {
		List<BndRunFile> bndRuns = new ArrayList<>();
		try {
			Iterator<Path> iterator = Files.list(path).iterator();
			while (iterator.hasNext()) {
				Path child = iterator.next();
				String fn = child.getFileName().toString();
				if (fn.endsWith(BNDRUN)) {
					String key = fn.substring(0, fn.length() - BNDRUN.length());
					if (match.contains(key)) {
						bndRuns.add(new BndRunFile(key, child));
					}
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
		return bndRuns;
	}

}
