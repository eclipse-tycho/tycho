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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.pomless.AbstractTychoMapping;
import org.eclipse.tycho.pomless.NoParentPomFound;
import org.eclipse.tycho.pomless.ParentModel;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

@Named("bnd-workspace")
@Singleton
public class BndWorkspaceMapping extends AbstractTychoMapping {

	private Map<String, List<String>> modulesCache = new ConcurrentHashMap<String, List<String>>();

	@Override
	public float getPriority() {
		return -20;
	}

	@Override
	protected boolean isValidLocation(Path polyglotFile) {
		return hasBndWorkspace(polyglotFile.getParent());
	}

	public static boolean hasBndWorkspace(Path dir) {
		Path cnfPath = dir.resolve(Workspace.CNFDIR);
		return Files.isDirectory(cnfPath) && Files.isRegularFile(cnfPath.resolve(Workspace.BUILDFILE));
	}

	@Override
	protected File getPrimaryArtifact(File dir) {
		File cnfDir = new File(dir, Workspace.CNFDIR);
		if (new File(cnfDir, Workspace.BUILDFILE).isFile()) {
			return cnfDir;
		}
		return null;
	}

	@Override
	public String getFlavour() {
		return "bnd-workspace";
	}

	@Override
	protected String getPackaging() {
		return "pom";
	}

	@Override
	protected void initModel(Model model, Reader artifactReader, Path cnfFolder) throws IOException {
		Path workspaceRoot = cnfFolder.getParent();
		List<String> projects = modulesCache.computeIfAbsent(workspaceRoot.toAbsolutePath().toString(), path -> {
			try (Stream<Path> listing = Files.list(workspaceRoot)) {
				logger.debug("Scanning " + path + "...");
				List<String> modules = listing.filter(Files::isDirectory)//
						.filter(dir -> Files.isRegularFile(dir.resolve(Project.BNDFILE)))//
						.map(Path::getFileName).map(String::valueOf).toList();
				for (String module : modules) {
					logger.debug("Found BND project " + module + ".");
				}
				return modules;
			} catch (IOException e) {
				if (logger.isDebugEnabled()) {
					logger.error("Can't determine BND projects for path " + path + "!", e);
				} else {
					logger.warn("Can't determine BND projects for path " + path + "! (" + e.getMessage() + ")");
				}
				return List.of();
			}
		});
		model.setModules(projects);
		model.setGroupId(workspaceRoot.getFileName().toString());
		model.setArtifactId("bnd-parent");
		model.setVersion("1.0.0");
		try {
			Workspace workspace = Workspace.findWorkspace(cnfFolder.toFile());
			String g = workspace.getProperty(TychoConstants.PROP_GROUP_ID);
			if (g != null) {
				model.setGroupId(g);
			}
			String a = workspace.getProperty(TychoConstants.PROP_ARTIFACT_ID);
			if (a != null) {
				model.setArtifactId(a);
			}
			String v = workspace.getProperty(TychoConstants.PROP_VERSION);
			if (v != null) {
				model.setVersion(v);
			}
			String bundleVersion = workspace.getProperty("Bundle-Version");
			if (bundleVersion != null) {
				model.setVersion(bundleVersion);
			}
		} catch (Exception e) {
			logger.debug("Can't derive properties from project!");
		}

	}

	@Override
	protected ParentModel findParent(Path projectRoot, Map<String, ?> projectOptions) throws IOException {
		try {
			return super.findParent(projectRoot, projectOptions);
		} catch (NoParentPomFound e) {
			// this can happen in 100% pomless mode!
			return new ParentModel(null, null);
		}
	}
}
