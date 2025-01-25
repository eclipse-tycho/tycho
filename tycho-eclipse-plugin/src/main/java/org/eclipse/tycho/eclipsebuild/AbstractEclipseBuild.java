package org.eclipse.tycho.eclipsebuild;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
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
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
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
		debug("Building...");
		executeWithJobs(this, m -> project.build(IncrementalProjectBuilder.FULL_BUILD, m));
	}

	protected abstract Result createResult(IProject project) throws Exception;

	static void disableAutoBuild() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(false);
		workspace.setDescription(desc);
	}

	public static void executeWithJobs(IProgressMonitor monitor, ICoreRunnable runnable)
			throws CoreException {
		Set<Job> scheduledJobs = recordJobs(new LinkedHashSet<>(), monitor, runnable);
		for (Job job : scheduledJobs) {
			if (monitor != null) {
				monitor.subTask("Wait for job " + job.getName() + " to finish");
			}
			try {
				job.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	public static Set<Job> recordJobs(Set<Job> scheduledJobs, IProgressMonitor monitor, ICoreRunnable runnable)
			throws CoreException {
		IJobChangeListener listener = new IJobChangeListener() {

			@Override
			public void sleeping(IJobChangeEvent event) {

			}

			@Override
			public void scheduled(IJobChangeEvent event) {
				scheduledJobs.add(event.getJob());
			}

			@Override
			public void running(IJobChangeEvent event) {

			}

			@Override
			public void done(IJobChangeEvent event) {
				scheduledJobs.remove(event.getJob());
			}

			@Override
			public void awake(IJobChangeEvent event) {

			}

			@Override
			public void aboutToRun(IJobChangeEvent event) {

			}
		};
		Job.getJobManager().addJobChangeListener(listener);
		IProgressMonitor safe = IProgressMonitor.nullSafe(monitor);
		runnable.run(safe);
		Job.getJobManager().removeJobChangeListener(listener);
		return scheduledJobs;
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
		if (t == null) {
			debug(string);
			return;
		}
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
