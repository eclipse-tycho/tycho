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
package org.eclipse.tycho.apitools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.BundleListTargetLocation;
import org.eclipse.pde.api.tools.internal.FilterStore;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.builder.BuildContext;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.model.SystemLibraryApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ApiAnalysis implements Serializable, Callable<ApiAnalysisResult> {

	private Collection<String> baselineBundles;
	private Collection<String> targetBundles;
	private String baselineName;
	private String apiFilterFile;
	private String projectDir;
	private boolean debug;
	private String apiPreferences;
	private String binaryArtifact;

	public ApiAnalysis(Collection<Path> baselineBundles, Collection<Path> dependencyBundles, String baselineName,
			Path apiFilterFile, Path apiPreferences, Path projectDir, boolean debug, Path binaryArtifact) {
		this.targetBundles = dependencyBundles.stream().map(ApiAnalysis::pathAsString).toList();
		this.baselineBundles = baselineBundles.stream().map(ApiAnalysis::pathAsString).toList();
		this.baselineName = baselineName;
		this.apiFilterFile = pathAsString(apiFilterFile);
		this.apiPreferences = pathAsString(apiPreferences);
		this.projectDir = pathAsString(projectDir);
		this.binaryArtifact = pathAsString(binaryArtifact);
		this.debug = debug;
	}

	@Override
	public ApiAnalysisResult call() throws Exception {
		ApiAnalysisResult result = new ApiAnalysisResult();
		Platform.addLogListener((status, plugin) -> debug(status.toString()));
		IJobManager jobManager = Job.getJobManager();
		jobManager.addJobChangeListener(new IJobChangeListener() {

			@Override
			public void sleeping(IJobChangeEvent event) {
				debug("Job " + event.getJob() + " sleeping...");
			}

			@Override
			public void scheduled(IJobChangeEvent event) {
				debug("Job " + event.getJob() + " scheduled...");
			}

			@Override
			public void running(IJobChangeEvent event) {
				debug("Job " + event.getJob() + " running...");
			}

			@Override
			public void done(IJobChangeEvent event) {
				debug("Job " + event.getJob() + " done...");
			}

			@Override
			public void awake(IJobChangeEvent event) {
				debug("Job " + event.getJob() + " awake...");
			}

			@Override
			public void aboutToRun(IJobChangeEvent event) {
				debug("Job " + event.getJob() + " aboutToRun...");
			}
		});
		printVersion();
		disableAutoBuild();
		setTargetPlatform();
		deleteAllProjects();
		BundleComponent projectComponent = importProject();
		IApiBaseline baseline = createBaseline(baselineBundles, baselineName + " - baseline");
		ResolverError[] resolverErrors = projectComponent.getErrors();
		if (resolverErrors != null && resolverErrors.length > 0) {
			for (ResolverError error : resolverErrors) {
				result.addResolverError(error);
			}
		}
		IApiFilterStore filterStore = getApiFilterStore(projectComponent);
		Properties preferences = getPreferences();
		BaseApiAnalyzer analyzer = new BaseApiAnalyzer();
		try {
			analyzer.setContinueOnResolverError(true);
			analyzer.analyzeComponent(null, filterStore, preferences, baseline, projectComponent, new BuildContext(),
					new NullProgressMonitor());
			IApiProblem[] problems = analyzer.getProblems();
			for (IApiProblem problem : problems) {
				result.addProblem(problem);
				debug(String.valueOf(problem));
			}
		} finally {
			analyzer.dispose();
			ResourcesPlugin.getWorkspace().save(true, new NullProgressMonitor());
		}
		return result;
	}

	private void waitForAnyJobs() {
		IJobManager manager = Job.getJobManager();
		debug("Suspend JobManager...");
		manager.suspend();
		debug("Wait for JobManager become idle...");
		while (!manager.isIdle()) {
			Thread.onSpinWait();
		}
		// See https://github.com/eclipse-platform/eclipse.platform/issues/741
		debug("Cancel remaining jobs...");
		manager.cancel(null);
		debug("JobManager is idle... no new jobs will run!");
	}

	private BundleComponent importProject() throws CoreException, IOException {
		IPath projectPath = IPath.fromOSString(projectDir);
		IPath projectDescriptionFile = projectPath.append(IProjectDescription.DESCRIPTION_FILE_NAME);
		IProjectDescription projectDescription = ResourcesPlugin.getWorkspace()
				.loadProjectDescription(projectDescriptionFile);
		projectDescription.setLocation(projectPath);
		projectDescription.setBuildSpec(new ICommand[0]);
		// FIXME ApiTools wrongly assumes that the location matches the project name
		// see: https://github.com/eclipse-pde/eclipse.pde/issues/789
		// therefore we here must not use projectDescription.getName() but
		// projectPath.lastSegment() ...
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectPath.lastSegment());
		project.create(projectDescription, new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		createOutputFolder(project, projectPath);
		waitForAnyJobs();
		IApiBaseline workspaceBaseline = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
		IApiComponent component = workspaceBaseline.getApiComponent(project);
		if (component instanceof ProjectComponent projectComponent) {
			debug("Project component was found");
			return projectComponent;
		}
		IApiComponent[] components = workspaceBaseline.getApiComponents();
		for (IApiComponent c : components) {
			String location = c.getLocation();
			if (location != null && IPath.fromOSString(location).equals(projectPath)
					&& c instanceof BundleComponent bundle) {
				debug("Fallback to binary bundle component");
				return bundle;
			}
		}
		if (binaryArtifact != null) {
			debug("Fallback to binary artifact");
			// TODO we would like to pass the imported project then see
			// https://github.com/eclipse-pde/eclipse.pde/pull/786
			IApiComponent binaryComponent = ApiModelFactory.newApiComponent(workspaceBaseline, binaryArtifact);
			if (binaryComponent instanceof BundleComponent bundle) {
				workspaceBaseline.addApiComponents(new IApiComponent[] { bundle });
				return bundle;
			}

		}
		throw new RuntimeException("Can't import project");
	}

	private void createOutputFolder(IProject project, IPath projectPath) throws IOException, CoreException {
		// FIXME see bug https://github.com/eclipse-pde/eclipse.pde/issues/791
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject != null) {
			makeOutputFolder(javaProject.getOutputLocation(), projectPath);
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			for (IClasspathEntry entry : classpath) {
				makeOutputFolder(entry.getOutputLocation(), projectPath);
			}
		}
	}

	private void makeOutputFolder(IPath outputLocation, IPath projectPath) throws CoreException, IOException {
		if (outputLocation != null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IFolder folder = workspace.getRoot().getFolder(outputLocation);
			if (!folder.exists()) {
				folder.create(true, true, new NullProgressMonitor());
			}
		}

	}

	private void deleteAllProjects() throws CoreException {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT | IResource.FORCE, new NullProgressMonitor());
		}
	}

	private void disableAutoBuild() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(false);
		workspace.setDescription(desc);
		PDECore.getDefault().getPreferencesManager().setValue(ICoreConstants.DISABLE_API_ANALYSIS_BUILDER, true);
		PDECore.getDefault().getPreferencesManager().setValue(ICoreConstants.RUN_API_ANALYSIS_AS_JOB, false);
	}

	private Properties getPreferences() throws IOException {
		Properties properties = new Properties();
		if (apiPreferences != null) {
			Path path = Path.of(apiPreferences);
			if (Files.isRegularFile(path)) {
				try (InputStream stream = Files.newInputStream(path)) {
					properties.load(stream);
				}
			}
		}
		return properties;
	}

	private void printVersion() {
		Bundle apiToolsBundle = FrameworkUtil.getBundle(ApiModelFactory.class);
		if (apiToolsBundle != null) {
			debug("API Tools version: " + apiToolsBundle.getVersion());
		}
	}

	private IApiBaseline createBaseline(Collection<String> bundles, String name) throws CoreException {
		debug("==== " + name + " ====");
		IApiBaseline baseline = ApiModelFactory.newApiBaseline(name);
		List<IApiComponent> baselineComponents = new ArrayList<IApiComponent>();
		for (String baselineBundle : bundles) {
			IApiComponent component = ApiModelFactory.newApiComponent(baseline, baselineBundle);
			if (component != null) {
				debug(component.getSymbolicName() + " " + component.getVersion() + " -- "
						+ new File(Objects.requireNonNullElse(component.getLocation(), "Unknown")).getName());
				baselineComponents.add(component);
			}
		}
		baseline.addApiComponents(baselineComponents.toArray(IApiComponent[]::new));
		for (IApiComponent component : baseline.getApiComponents()) {
			if (component instanceof SystemLibraryApiComponent systemLibrary) {
				debug("System Component:");
				debug("\tVersion: " + systemLibrary.getVersion());
				debug("\tLocation: " + systemLibrary.getLocation());
				for (String ee : systemLibrary.getExecutionEnvironments()) {
					debug("\tExecution Environment: " + ee);
				}
			}

		}
		return baseline;
	}

	private IApiFilterStore getApiFilterStore(BundleComponent bundle) {
		return new FilterStore(bundle) {
			@Override
			protected synchronized void initializeApiFilters() {
				if (fFilterMap == null) {
					fFilterMap = new HashMap<>(5);
					if (apiFilterFile != null) {
						Path path = Path.of(apiFilterFile);
						if (Files.isRegularFile(path)) {
							try (InputStream stream = Files.newInputStream(path)) {
								readFilterFile(stream);
							} catch (IOException e) {
								debug(e.toString());
							}
						}
					}
				}
			}
		};
	}

	private void debug(String string) {
		if (debug) {
			System.out.println(string);
		}
	}

	private void setTargetPlatform() throws IOException, CoreException, InterruptedException {
		ITargetPlatformService service = TargetPlatformService.getDefault();
		ITargetDefinition target = service.newTarget();
		target.setName("buildpath");
		TargetBundle[] bundles = targetBundles.stream()//
				.map(absoluteFile -> {
					try {
						return new TargetBundle(new File(absoluteFile));
					} catch (CoreException e) {
						debug(e.toString());
						return null;
					}
				}).filter(Objects::nonNull)//
				.toArray(TargetBundle[]::new);
		target.setTargetLocations(new ITargetLocation[] { new BundleListTargetLocation(bundles) });
		service.saveTargetDefinition(target);
		Job job = new LoadTargetDefinitionJob(target);
		job.schedule();
		job.join();
	}

	private static String pathAsString(Path path) {
		if (path != null) {
			return path.toAbsolutePath().toString();
		}
		return null;
	}

}
