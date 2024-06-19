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
import java.io.Reader;
import java.nio.file.Path;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.pomless.AbstractTychoMapping;
import org.eclipse.tycho.version.TychoVersion;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

@Named("bnd")
@Singleton
public class BndProjectMapping extends AbstractTychoMapping {
	

	private static final String TYCHO_BND_PLUGIN = "tycho-bnd-plugin";

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
	protected void initModel(Model model, Reader artifactReader, Path artifactFile) throws IOException {
		try {
			Project project = Workspace.getProject(artifactFile.getParent().toFile());
			String g = project.getProperty(TychoConstants.PROP_GROUP_ID);
			String a = project.getProperty(TychoConstants.PROP_ARTIFACT_ID);
			String v = project.getProperty(TychoConstants.PROP_VERSION);
			if (g != null) {
				model.setGroupId(g);
			}
			if (a == null) {
				model.setArtifactId(project.getName());
			} else {
				model.setArtifactId(a);
			}
			if (v == null) {
				model.setVersion(project.getBundleVersion());
			} else {
				model.setVersion(v);
			}

			model.setVersion(project.getBundleVersion());
			Plugin bndPlugin = getPlugin(model, TYCHO_GROUP_ID, TYCHO_BND_PLUGIN);
			bndPlugin.setExtensions(true);
			bndPlugin.setVersion(TychoVersion.getTychoVersion());
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

}
