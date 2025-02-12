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
package org.eclipse.tycho.build.bnd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.bndlib.JdtProjectBuilder;
import org.eclipse.tycho.pomless.AbstractTychoMapping;
import org.eclipse.tycho.pomless.NoParentPomFound;
import org.eclipse.tycho.pomless.ParentModel;
import org.eclipse.tycho.version.TychoVersion;
import org.sonatype.maven.polyglot.mapping.Mapping;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;

@Component(role = Mapping.class, hint = "bnd")
public class BndProjectMapping extends AbstractTychoMapping {

	private static final String TYCHO_BND_PLUGIN = "tycho-bnd-plugin";
	@Requirement(role = Lifecycle.class)
	private Map<String, Lifecycle> lifecycles;

	@Override
	public float getPriority() {
		return 100;
	}

	@Override
	protected boolean isValidLocation(Path location) {
		try {
			return getFileName(location).equals(Project.BNDFILE)
					&& Workspace.findWorkspace(location.getParent().toFile()) != null;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected File getPrimaryArtifact(File dir) {
		File bndFile = new File(dir, Project.BNDFILE);
		try {
			if (bndFile.exists() && Workspace.findWorkspace(dir.getParentFile()) != null) {
				return bndFile;
			}
		} catch (Exception e) {
		}
		return null;
	}

	@Override
	protected String getPackaging() {
		return "jar";
	}

	@Override
	public String getFlavour() {
		return "bnd";
	}

	@Override
	protected Properties getEnhancementProperties(Path file) throws IOException {
		Properties bnd = new Properties();
		try (InputStream stream = Files.newInputStream(file)) {
			bnd.load(stream);
			return bnd;
		}
	}

	@Override
	protected void initModel(Model model, Reader artifactReader, Path artifactFile) throws IOException {
		try {
			Workspace.setDriver(TychoConstants.DRIVER_NAME);
			Project project = Workspace.getProject(artifactFile.getParent().toFile());
			if (project.getSubProjects().isEmpty()) {
				model.setPackaging(getPackaging());
			} else {
				model.setPackaging("pom");
			}
			String g = project.getProperty(TychoConstants.PROP_GROUP_ID);
			String a = project.getProperty(TychoConstants.PROP_ARTIFACT_ID);
			String v = project.getProperty(TychoConstants.PROP_VERSION);
			if (g != null) {
				model.setGroupId(g);
			}
			if (a == null || v == null) {
				try (ProjectBuilder builder = createBuilder(project)) {
					if (a == null) {
						a = builder.getBsn();
					}
					if (v == null) {
						v = builder.getVersion();
					}
				}
			}
			model.setArtifactId(a);
			model.setVersion(v);
			Build build = getBuild(model);
			build.setDirectory(path(project.getTarget()));
			build.setOutputDirectory(path(project.getSrcOutput()));
			build.setTestOutputDirectory(path(project.getTestOutput()));
			@SuppressWarnings("deprecation")
			File src = project.getSrc();
			build.setSourceDirectory(path(src));
			build.setTestSourceDirectory(path(project.getTestSrc()));
			Plugin bndPlugin = getPlugin(model, TYCHO_GROUP_ID, TYCHO_BND_PLUGIN);
			bndPlugin.setExtensions(true);
			bndPlugin.setVersion(TychoVersion.getTychoVersion());
			addPluginExecution(bndPlugin, execution -> {
				execution.setId("initialize");
				execution.addGoal("initialize");
			});
			addPluginExecution(bndPlugin, execution -> {
				execution.setId("clean");
				execution.addGoal("clean");
			});
			addPluginExecution(bndPlugin, execution -> {
				execution.setId("compile");
				execution.addGoal("compile");
			});
			addPluginExecution(bndPlugin, execution -> {
				execution.setId("testCompile");
				execution.addGoal("test-compile");
			});
			addPluginExecution(bndPlugin, execution -> {
				execution.setId("integration-test");
				execution.addGoal("integration-test");
			});
			addPluginExecution(bndPlugin, execution -> {
				execution.setId("build");
				execution.addGoal("build");
				execution.addGoal("run");
			});
		} catch (Exception e) {
			if (e instanceof IOException io) {
				throw io;
			}
			if (e instanceof RuntimeException rt) {
				throw rt;
			}
			throw new IOException(e);
		}

	}

	private static ProjectBuilder createBuilder(Project project) throws Exception {
		ProjectBuilder builder = new JdtProjectBuilder(project);
		builder.setBase(project.getBase());
		builder.use(project);
		builder.setFailOk(true);
		return builder;
	}

	private static String path(File file) throws Exception {
		if (file != null) {
			return file.getAbsolutePath();
		}
		return null;
	}

	@Override
	protected ParentModel loadParent(Path projectRoot, Path fileOrFolder) throws NoParentPomFound, IOException {

		if (isCnfDir(projectRoot)) {
			throw new NoParentPomFound(
					"The workspace configuration folder '" + projectRoot
							+ "' can not contain a bnd-project, consider moving it to an own project or place a pom.xml in the config folder to manually configure the maven execution");
		}
		return super.loadParent(projectRoot, fileOrFolder);
	}

	private boolean isCnfDir(Path projectRoot) {
		try {
			Workspace workspace = Workspace.findWorkspace(projectRoot.toFile());
			File buildDir = workspace.getBuildDir();
			if (Files.isSameFile(projectRoot, buildDir.toPath())) {
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

}
