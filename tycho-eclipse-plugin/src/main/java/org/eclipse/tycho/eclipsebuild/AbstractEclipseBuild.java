package org.eclipse.tycho.eclipsebuild;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Abstract class for performing a build and producing a result
 * 
 * @param <Result>
 */
public abstract class AbstractEclipseBuild<Result extends EclipseBuildResult>
		implements Callable<Result>, Serializable, IProgressMonitor {
	private boolean debug;
	private String baseDir;

	protected AbstractEclipseBuild(Path projectDir, boolean debug) {
		this.debug = debug;
		this.baseDir = pathAsString(projectDir);
	}

	@Override
	public final Result call() throws Exception {
		Platform.addLogListener((status, plugin) -> debug(status.toString()));
		disableAutoBuild();
		deleteAllProjects();
		IProject project = importProject();
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, this);
		buildProject(project);
		Result result = createResult(project);
		for (IMarker marker : project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) {
			result.addMarker(marker);
			debug(marker.toString());
		}
		ResourcesPlugin.getWorkspace().save(true, this);
		return result;
	}

	protected void buildProject(IProject project) throws CoreException {
		project.build(IncrementalProjectBuilder.FULL_BUILD, this);
		while (!Job.getJobManager().isIdle()) {
			Thread.yield();
		}
	}

	protected abstract Result createResult(IProject project) throws Exception;

	static void disableAutoBuild() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(false);
		workspace.setDescription(desc);
	}

	private void deleteAllProjects() throws CoreException {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT | IResource.FORCE, this);
		}
	}

	private IProject importProject() throws CoreException, IOException {
		IPath projectPath = IPath.fromOSString(baseDir);
		IPath projectDescriptionFile = projectPath.append(IProjectDescription.DESCRIPTION_FILE_NAME);
		IProjectDescription projectDescription = ResourcesPlugin.getWorkspace()
				.loadProjectDescription(projectDescriptionFile);
		projectDescription.setLocation(projectPath);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectDescription.getName());
		project.create(projectDescription, this);
		project.open(this);
		return project;
	}

	protected void debug(String string) {
		if (debug) {
			System.out.println(string);
		}
	}

	protected void debug(String string, Throwable t) {
		if (debug) {
			StringWriter writer = new StringWriter();
			t.printStackTrace(new PrintWriter(writer));
			debug(string + System.lineSeparator() + writer);
		}
	}

	static String pathAsString(Path path) {
		if (path != null) {
			return path.toAbsolutePath().toString();
		}
		return null;
	}

	@Override
	public boolean isCanceled() {
		return Thread.currentThread().isInterrupted();
	}

	@Override
	public void beginTask(String name, int totalWork) {
		if (name != null && !name.isBlank()) {
			debug("> " + name);
		}
	}

	@Override
	public void subTask(String name) {
		if (name != null && !name.isBlank()) {
			debug(">> " + name);
		}
	}

	@Override
	public void setTaskName(String name) {
		if (name != null && !name.isBlank()) {
			debug("> " + name);
		}
	}

	@Override
	public void done() {
		// do nothing
	}

	@Override
	public void setCanceled(boolean value) {
		// do nothing
	}

	@Override
	public void internalWorked(double work) {
		// do nothing

	}

	@Override
	public void worked(int work) {
		// do nothing
	}

}
