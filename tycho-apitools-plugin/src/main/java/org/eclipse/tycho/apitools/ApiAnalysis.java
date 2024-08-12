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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.jdt.internal.launching.LaunchingPlugin;
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
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
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

/**
 * Performs the API Analysis inside the embedded OSGi Frameworks
 */
public class ApiAnalysis implements Serializable, Callable<ApiAnalysisResult> {

	private Collection<String> baselineBundles;
	private Collection<String> targetBundles;
	private String baselineName;
	private String apiFilterFile;
	private String projectDir;
	private boolean debug;
	private String apiPreferences;
	private String binaryArtifact;
	private String outputDir;

	ApiAnalysis(Collection<Path> baselineBundles, Collection<Path> dependencyBundles, String baselineName,
			Path apiFilterFile, Path apiPreferences, Path projectDir, boolean debug, Path binaryArtifact,
			Path outputDir) {
		this.targetBundles = dependencyBundles.stream().map(ApiAnalysis::pathAsString).toList();
		this.baselineBundles = baselineBundles.stream().map(ApiAnalysis::pathAsString).toList();
		this.baselineName = baselineName;
		this.apiFilterFile = pathAsString(apiFilterFile);
		this.apiPreferences = pathAsString(apiPreferences);
		this.projectDir = pathAsString(projectDir);
		this.binaryArtifact = pathAsString(binaryArtifact);
		this.outputDir = projectDir.relativize(outputDir).toString();
		this.debug = debug;
	}

	@Override
	public ApiAnalysisResult call() throws Exception {

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
		disableJVMDiscovery();
		setTargetPlatform();
		deleteAllProjects();
		IPath projectPath = IPath.fromOSString(projectDir);
		IProject project = getProject(projectPath);
		ApiAnalysisResult result = new ApiAnalysisResult(getVersion());
		WorkspaceJob job = new WorkspaceJob("Tycho API Analysis") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					BundleComponent projectComponent = getApiComponent(project, projectPath);
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
						analyzer.analyzeComponent(null, filterStore, preferences, baseline, projectComponent,
								new BuildContext(), new NullProgressMonitor());
						IApiProblem[] problems = analyzer.getProblems();
						for (IApiProblem problem : problems) {
							result.addProblem(problem, project);
							debug(String.valueOf(problem));
						}
					} finally {
						analyzer.dispose();
						ResourcesPlugin.getWorkspace().save(true, new NullProgressMonitor());
					}
				} catch (Exception e) {
					return Status.error("Api Analysis failed", e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.schedule();
		job.join();
		IStatus status = job.getResult();
		JRTUtil.reset(); // reclaim space due to loaded multiple JRTUtil should better be fixed to not
							// use that much space
		if (!status.isOK() && status.getException() instanceof Exception error) {
			throw error;
		}
		return result;
	}

	private String getVersion() {
		Bundle apiToolsBundle = FrameworkUtil.getBundle(ApiModelFactory.class);
		if (apiToolsBundle != null) {
			return apiToolsBundle.getVersion().toString();
		}
		return "n/a";
	}

	private void disableJVMDiscovery() {
		IEclipsePreferences instanceNode = InstanceScope.INSTANCE
				.getNode(LaunchingPlugin.getDefault().getBundle().getSymbolicName());
		instanceNode.putBoolean(LaunchingPlugin.PREF_DETECT_VMS_AT_STARTUP, false);
	}

	private BundleComponent getApiComponent(IProject project, IPath projectPath) throws CoreException, IOException {
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

	private IProject getProject(IPath projectPath) throws CoreException, IOException {
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
		return project;
	}

	private void createOutputFolder(IProject project, IPath projectPath) throws IOException, CoreException {
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject != null) {
			Map<String, String> outputJars = computeOutputJars(project, javaProject);
			IFolder outputFolder = project.getFolder(outputDir);
			// FIXME see bug https://github.com/eclipse-pde/eclipse.pde/issues/801
			// it can happen that project output location != maven compiled classes, usually
			// eclipse uses output = bin/ while maven uses target/classes if not
			// specifically configured to be even
			IPath mainOutputLocation = javaProject.getOutputLocation();
			IPath mainRealPath = getRealPath(mainOutputLocation, outputJars, outputFolder);
			makeOutputFolder(mainOutputLocation, mainRealPath);
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			for (IClasspathEntry entry : classpath) {
				IPath entryOutputLocation = entry.getOutputLocation();
				if (entryOutputLocation != null) {
					IPath realEntryPath = getRealPath(entryOutputLocation, outputJars, outputFolder);
					makeOutputFolder(entryOutputLocation, realEntryPath);
				}
			}
		}
	}

	private Map<String, String> computeOutputJars(IProject project, IJavaProject javaProject) throws CoreException {
		Map<String, String> outputJars = new HashMap<String, String>();
		IPluginModelBase base = PluginRegistry.findModel(project);
		if (base != null) {
			IBuildModel model = PluginRegistry.createBuildModel(base);
			if (model != null) {
				IBuild ibuild = model.getBuild();
				IBuildEntry[] entries = ibuild.getBuildEntries();
				for (IBuildEntry entry : entries) {
					String name = entry.getName();
					if (name.startsWith(IBuildEntry.OUTPUT_PREFIX)) {
						String key = name.substring(IBuildEntry.OUTPUT_PREFIX.length());
						for (String token : entry.getTokens()) {
							outputJars.put(normalizeOutputPath(token), key);
						}
					} else if (name.startsWith(IBuildEntry.JAR_PREFIX)) {
						// Actually each source.<jar> should have a corresponding output.<jar> but there
						// are some cases where this is not true... lets cheat and look at the
						// classpath instead...
						String key = name.substring(IBuildEntry.JAR_PREFIX.length());
						IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
						for (String token : entry.getTokens()) {
							IPath srcPath = project.getFolder(token).getFullPath();
							for (IClasspathEntry classpathEntry : rawClasspath) {
								if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
									IPath path = classpathEntry.getPath();
									if (srcPath.equals(path)) {
										IPath outputLocation = classpathEntry.getOutputLocation();
										if (outputLocation == null) {
											outputLocation = javaProject.getOutputLocation();
										}
										IFolder folder = getProjectFolder(outputLocation);
										String tokenOutput = folder.getProjectRelativePath().toString();
										outputJars.putIfAbsent(normalizeOutputPath(tokenOutput), key);
									}
								}
							}
						}
					}
				}
			}
		}
		return outputJars;
	}

	private String normalizeOutputPath(String path) {
		if (path != null && path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		}
		return path;
	}

	private IPath getRealPath(IPath eclipseOutputLocation, Map<String, String> outputJars, IFolder mavenOutputFolder) {
		if (eclipseOutputLocation == null) {
			return null;
		}
		IFolder projectFolder = getProjectFolder(eclipseOutputLocation);
		for (Entry<String, String> entry : outputJars.entrySet()) {
			IFolder jarFolder = projectFolder.getProject().getFolder(entry.getKey());
			if (jarFolder.equals(projectFolder)) {
				String jarOutputPath = entry.getValue();
				if (".".equals(jarOutputPath) || outputJars.size() == 1) {
					// special case of one classpath entry which is not ".", Tycho also use standard
					// maven output dir
					return mavenOutputFolder.getFullPath();
				}
				return mavenOutputFolder.getParent()
						.getFolder(new org.eclipse.core.runtime.Path(jarOutputPath + "-classes")).getFullPath();
			}
		}
		return eclipseOutputLocation;
	}

	private IFolder makeOutputFolder(IPath eclipseOutputLocation, IPath mavenOutputLocation)
			throws CoreException, IOException {
		if (eclipseOutputLocation != null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IFolder folder = workspace.getRoot().getFolder(eclipseOutputLocation);
			if (!folder.exists()) {
				folder.create(true, true, new NullProgressMonitor());
			}
			if (mavenOutputLocation != null && !eclipseOutputLocation.equals(mavenOutputLocation)) {
				copy(getFile(mavenOutputLocation), getFile(eclipseOutputLocation));
			}
			folder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			return folder;
		}
		return null;
	}

	private File getFile(IPath path) {
		if (path == null) {
			return null;
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath location = workspace.getRoot().getFolder(path).getLocation();
		if (location == null) {
			return null;
		}
		return location.toFile();
	}

	private void copy(File from, File to) throws IOException {
		if (from == null || to == null || !from.isDirectory() || !to.isDirectory()) {
			return;
		}
		final Path targetPath = to.toPath();
		Files.walkFileTree(from.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
					throws IOException {
				Files.createDirectories(targetPath.resolve(from.toPath().relativize(dir)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				Files.copy(file, targetPath.resolve(from.toPath().relativize(file)),
						StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private IFolder getProjectFolder(IPath path) {
		if (path != null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			return workspace.getRoot().getFolder(path);
		}
		return null;
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
		PDECore.getDefault().getPreferencesManager().setValue(ICoreConstants.DISABLE_API_ANALYSIS_BUILDER, false);
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
		debug("API Tools version: " + getVersion());
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
