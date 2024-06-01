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
package org.eclipse.tycho.eclipsebuild;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

public class EclipseBuild implements Callable<EclipseBuildResult>, Serializable {

	private boolean debug;
	private String baseDir;

	EclipseBuild(Path projectDir, boolean debug) {
		this.debug = debug;
		this.baseDir = pathAsString(projectDir);
	}

	@Override
	public EclipseBuildResult call() throws Exception {
		EclipseBuildResult result = new EclipseBuildResult();
		Platform.addLogListener((status, plugin) -> debug(status.toString()));
		disableAutoBuild();
		deleteAllProjects();
		IProject project = importProject();
		IProgressMonitor debugMonitor = new IProgressMonitor() {

			@Override
			public void worked(int work) {

			}

			@Override
			public void subTask(String name) {
				debug("SubTask: " + name);
			}

			@Override
			public void setTaskName(String name) {
				debug("Task: " + name);
			}

			@Override
			public void setCanceled(boolean value) {

			}

			@Override
			public boolean isCanceled() {
				return false;
			}

			@Override
			public void internalWorked(double work) {

			}

			@Override
			public void done() {

			}

			@Override
			public void beginTask(String name, int totalWork) {
				setTaskName(name);
			}
		};
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, debugMonitor);
		project.build(IncrementalProjectBuilder.FULL_BUILD, debugMonitor);
		for (IMarker marker : project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) {
			result.addMarker(marker);
			debug(marker.toString());
		}
		ResourcesPlugin.getWorkspace().save(true, new NullProgressMonitor());
		return result;
	}

	static void disableAutoBuild() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(false);
		workspace.setDescription(desc);
	}

	private void deleteAllProjects() throws CoreException {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT | IResource.FORCE, new NullProgressMonitor());
		}
	}

	private IProject importProject() throws CoreException, IOException {
		IPath projectPath = IPath.fromOSString(baseDir);
		IPath projectDescriptionFile = projectPath.append(IProjectDescription.DESCRIPTION_FILE_NAME);
		IProjectDescription projectDescription = ResourcesPlugin.getWorkspace()
				.loadProjectDescription(projectDescriptionFile);
		projectDescription.setLocation(projectPath);
//        projectDescription.setBuildSpec(new ICommand[0]);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectDescription.getName());
		project.create(projectDescription, new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		return project;
	}

	private void debug(String string) {
		if (debug) {
			System.out.println(string);
		}
	}

	static String pathAsString(Path path) {
		if (path != null) {
			return path.toAbsolutePath().toString();
		}
		return null;
	}

}
