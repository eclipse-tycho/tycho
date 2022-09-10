/*******************************************************************************
 * Copyright (c) 2022, 2023 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.apitools;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;
import org.eclipse.pde.api.tools.internal.AnyValue;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.model.ApiBaseline;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.model.SystemLibraryApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.extras.pde.ListDependenciesMojo;
import org.eclipse.tycho.helper.PluginRealmHelper;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

/**
 * Performs a PDE-API Tools analysis of this project.
 */
@Mojo(name = "analyse", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true) // , requiresDependencyCollection =
																					// ResolutionScope.COMPILE_PLUS_RUNTIME,
																					// requiresOnline = true
// TODO: or in a later phase?
public class ApiAnalysisMojo extends AbstractMojo {

	// TODO: check SystemLibraryComponent content
	// TODO: check how PDE-APItools uses the baseline repo in the UI. Does it
	// consider java exported packages?
	// Generally compare it in more detail with the UI version.

	// TODO: consider api-filters

	@Component(hint = "connect")
	private EquinoxServiceFactory serviceFactory;

	@Parameter(property = "project", readonly = true)
	private MavenProject project;

	@Component(role = TychoProject.class)
	private Map<String, TychoProject> projectTypes;

	@Component
	private PluginRealmHelper pluginRealmHelper;

	@Parameter(property = "session", readonly = true)
	private MavenSession session;

	@Component
	private TychoProjectManager tychoProjectManager;

	@Component
	P2ResolverFactory resolverFactory;

	@Parameter(required = true, property = "tycho.apitools.analyse.baseline")
	private List<URL> baselines; // FIXME: make required? or use String to allow file-paths? Just like
									// CompareWithBaselineMojo?

	@Parameter(defaultValue = "eclipse-plugin")
	private Set<String> supportedPackagingTypes;

	@Parameter(defaultValue = "false", property = "tycho.apitools.analyse.skip")
	private boolean skip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skipped");
			return;
		}
		if (supportedPackagingTypes.contains(project.getPackaging())) {

			File baselineFile2 = null;
			try {
				baselineFile2 = getBaselineFile();
			} catch (URISyntaxException e) {
				throw new MojoExecutionException("Failed to fetch baseline file", e);
			}
			System.out.println("Baseline file: " + baselineFile2);

			synchronized (ApiAnalysisMojo.class) { // TODO: ensure thread-safety

				File projectFile = project.getArtifact().getFile();
				Objects.requireNonNull(projectFile,
						"Project artifact (not yet) build. Consider to bind this goal to a phase after 'package'");
				Set<File> projectDependencies = collectProjectDependencyPaths();

				IApiProblem[] problems;
				try {
					problems = analyse(projectFile, projectDependencies, baselineFile2);
				} catch (Exception e) {
					throw new MojoExecutionException("Failed to analyse API", e);
				}
				for (IApiProblem problem : problems) {
					System.out.println(problem);
					// TODO: without preferences all problems are warnings
					if (problem.getSeverity() == ApiPlugin.ERROR) {
						// TODO: collect problems and make it simple to parse it like in latest API-app
						// improvements
						throw new MojoFailureException("API-problems:" + problem.getMessage());
					}
				}
			}
		}
	}

	@SuppressWarnings("removal")
	private Set<File> collectProjectDependencyPaths() throws MojoExecutionException {
		// TODO: move called method here when that plug-in is removed
		return ListDependenciesMojo.collectProjectDependencyPaths(project, projectTypes, pluginRealmHelper, session);
	} // FIXME: the osgi-system-bundle has to be added too!
		// C:\Users\Hannes\.m2\repository\p2\osgi\bundle\org.eclipse.osgi\3.18.100.v20220817-1601\org.eclipse.osgi-3.18.100.v20220817-1601.jar

	@Requirement
	private PlexusContainer container;

	private File getBaselineFile() throws URISyntaxException {
		// TODO: unify into a an abstractBaseline-mojo? Use that also for baseline
		// comparison and p2-metadata validation.

		// TODO: is this something for TargetPlatformService? Add baseline repos and a
		// key

		ReactorProject reactorProject = DefaultReactorProject.adapt(project);

		Set<IInstallableUnit> dependencyMetadata = reactorProject.getDependencyMetadata(DependencyMetadataType.SEED);
		if (dependencyMetadata == null || dependencyMetadata.isEmpty()) {
			getLog().debug("Skipping API-analysis, no p2 artifacts created in build.");
			return null;
		}

		// TODO: why not use the injected one?
		TargetEnvironment runningEnvironment = TargetEnvironment.getRunningEnvironment();
//		P2ResolverFactory resolverFactory = serviceFactory.getService(P2ResolverFactory.class);
		P2Resolver resolver = resolverFactory.createResolver(List.of(runningEnvironment));

		TargetPlatformConfigurationStub baselineTPStub = new TargetPlatformConfigurationStub();
		baselineTPStub.setIgnoreLocalArtifacts(true);
		baselineTPStub.setEnvironments(List.of(runningEnvironment));
		for (URL baseline : this.baselines) {
			baselineTPStub.addP2Repository(baseline.toURI().normalize());
		}

		ExecutionEnvironmentConfiguration eeConfig = tychoProjectManager.getExecutionEnvironmentConfiguration(project);
		TargetPlatform baselineTP = resolverFactory.getTargetPlatformFactory().createTargetPlatform(baselineTPStub,
				eeConfig, null);

		return dependencyMetadata.stream().filter(u -> !isSourceBundle(u)).flatMap(u -> { // TODO: more to filter?
			P2ResolutionResult result = resolver.resolveInstallableUnit(baselineTP, u.getId(), "0.0.0");
			return result.getArtifacts().stream();
		}).map(a -> a.getLocation(true)).findFirst().orElse(null);
	}

	private boolean isSourceBundle(IInstallableUnit unit) {
		return unit.getProperty("maven-type").equals("java-source");
	}

	public IApiProblem[] analyse(File reactorJar, Set<File> reactorDependencies, File baselineJar)
			throws CoreException {

		// TODO: See
		// org.eclipse.pde.api.tools.model.tests.TestSuiteHelper.createTestingBaseline();

		IApiBaseline reactorBaseline = createBaseline("reactor");
		IApiComponent reactorComponent = createAPIComponent(reactorBaseline, reactorJar);
		List<IApiComponent> components = new ArrayList<>();
		components.add(reactorComponent);
		for (File dependencyPath : reactorDependencies) {
			components.add(createAPIComponent(reactorBaseline, dependencyPath));
		}
		reactorBaseline.addApiComponents(components.toArray(IApiComponent[]::new));

		IApiBaseline baseline = createBaseline("baseline");
		IApiComponent baselineComponent = createAPIComponent(baseline, baselineJar);
		baseline.addApiComponents(new IApiComponent[] { baselineComponent });

		IApiAnalyzer analyzer = new BaseApiAnalyzer();
		analyzer.analyzeComponent(null, null, null, baseline, reactorComponent, null, null);
		return analyzer.getProblems();
	}

	private IApiComponent createAPIComponent(IApiBaseline current, File bundleFile) throws CoreException {
		new BundleComponent(current, bundleFile.getAbsolutePath(), 0);
		// TODO: this is not thread-safe!
		return ApiModelFactory.newApiComponent(current, bundleFile.getAbsolutePath());
	}

	private static AnyValue ANY_VALUE = new AnyValue("*"); //$NON-NLS-1$

	private static IApiBaseline createBaseline(String name) throws CoreException {
		// TODO: use the configured JDK. How to get that?
		String javaHome = System.getProperty("java.home");
		String javaSpecVersion = System.getProperty("java.specification.version"); // e.g. "17"

		// TODO: create a method to APItools to create a baseline with a given
		// EEDescription
		ExecutionEnvironmentDescription eeDescription = createEEDescription(javaHome, javaSpecVersion);

		return new ApiBaseline(name + "-Tycho-APIBaseline") {
			@Override
			protected void resolveSystemLibrary(HashSet<String> ees) {
				try {
					String[] systemPackages = querySystemPackages(eeDescription);
					String executionEnvironments = calculateVMExecutionEnvs(new Version(javaSpecVersion));
					getState().setPlatformProperties(FrameworkUtil.asDictionary(Map.of(//
							Constants.FRAMEWORK_SYSTEMPACKAGES, systemPackages, //
							Constants.FRAMEWORK_EXECUTIONENVIRONMENT, executionEnvironments, //
							"osgi.os", ANY_VALUE, //
							"osgi.arch", ANY_VALUE, //
							"osgi.ws", ANY_VALUE, //
							"osgi.nl", ANY_VALUE)));

					var sysLib = new SystemLibraryApiComponent(this, eeDescription, systemPackages);
					addComponent(sysLib);
					addToSystemLibraryComponentList(this, sysLib);
				} catch (CoreException e) {
					throw new IllegalStateException("Failed to initialize API-baseline", e);
				}
			}

		};
	}

	private static ExecutionEnvironmentDescription createEEDescription(String javaHome, String javaSpecVersion)
			throws CoreException {
		try {
			Path eeFile = Files.createTempFile("apiBaseline", ".ee");
			try {
				Files.write(eeFile, List.of(//
						ExecutionEnvironmentDescription.JAVA_HOME + "=" + javaHome, //
						ExecutionEnvironmentDescription.CLASS_LIB_LEVEL + "=JavaSE-" + javaSpecVersion, //
						ExecutionEnvironmentDescription.BOOT_CLASS_PATH + "=" + getJavaRuntimeJar(javaHome), //
						ExecutionEnvironmentDescription.LANGUAGE_LEVEL + "=" + javaSpecVersion //
				));
				return new ExecutionEnvironmentDescription(eeFile.toFile());
			} finally {
				Files.delete(eeFile);
			}
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to write EEDescription file", e));
		}
	}

	// Heavily based on
	// org.eclipse.pde.internal.core.TargetPlatformHelper.querySystemPackages()

	private static String[] querySystemPackages(ExecutionEnvironmentDescription eeDescription) throws CoreException {
		Collection<String> packages = new TreeSet<>();
		String javaHome = eeDescription.getProperty(ExecutionEnvironmentDescription.JAVA_HOME);
		String release = eeDescription.getProperty(ExecutionEnvironmentDescription.LANGUAGE_LEVEL);
		String file = getJavaRuntimeJar(javaHome).toString();
		var jrt = org.eclipse.jdt.internal.core.builder.ClasspathLocation.forJrtSystem(file, null, null, release);
		for (String moduleName : jrt.getModuleNames(null)) {
			var module = jrt.getModule(moduleName);
			if (module != null) {
				for (var packageExport : module.exports()) {
					if (!packageExport.isQualified()) {
						packages.add(new String(packageExport.name()));
					}
				}
			}
		}
		return packages.toArray(String[]::new); // $NON-NLS-1$
	}

	private static Path getJavaRuntimeJar(String javaHome) {
		return Path.of(javaHome).resolve("lib").resolve(org.eclipse.jdt.internal.compiler.util.JRTUtil.JRT_FS_JAR);
	}

	// Copied from org.eclipse.jdt.internal.launching.environments
	// ExecutionEnvironment
	private static final String JAVASE = "JavaSE"; //$NON-NLS-1$

	private static String calculateVMExecutionEnvs(Version javaVersion) {
		StringBuilder result = new StringBuilder(
				"OSGi/Minimum-1.0, OSGi/Minimum-1.1, OSGi/Minimum-1.2, JavaSE/compact1-1.8, JavaSE/compact2-1.8, JavaSE/compact3-1.8, JRE-1.1, J2SE-1.2, J2SE-1.3, J2SE-1.4, J2SE-1.5, JavaSE-1.6, JavaSE-1.7, JavaSE-1.8"); //$NON-NLS-1$
		Version v = new Version(9, 0, 0);
		while (v.compareTo(javaVersion) <= 0) {
			result.append(',').append(' ').append(JAVASE).append('-').append(v.getMajor());
			if (v.getMinor() > 0) {
				result.append('.').append(v.getMinor());
			}
			if (v.getMajor() == javaVersion.getMajor()) {
				v = new Version(v.getMajor(), v.getMinor() + 1, 0);
			} else {
				v = new Version(v.getMajor() + 1, 0, 0);
			}
		}
		return result.toString();
	}

	private static void addToSystemLibraryComponentList(ApiBaseline baseline, SystemLibraryApiComponent systemLibrary) {
		try {
			Field systemLibraryList = ApiBaseline.class.getDeclaredField("fSystemLibraryComponentList");
			systemLibraryList.trySetAccessible();
			@SuppressWarnings("unchecked")
			List<IApiComponent> list = (List<IApiComponent>) systemLibraryList.get(baseline);
			list.add(systemLibrary);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to add SystemLibraryComponent", e);
		}
	}

}
