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
 *    
 */
package org.eclipse.tycho.testing.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.StreamSupport;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Execute tests using the JUnit Platform inside an OSGi Framework using the
 * <a href=
 * "https://docs.junit.org/current/user-guide/#running-tests-console-launcher">tests-console-launcher</a>
 */
@Mojo(name = "junit-platform", threadSafe = true, defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresProject = true, requiresDependencyCollection = ResolutionScope.TEST)
public class JUnitPlatformMojo extends AbstractMojo {

	private static final String JUNIT_TOOLPROVIDER_NAME = "junit";

	@Component
	TychoProjectManager projectManager;

	@Component
	MavenProject mavenProject;

	@Parameter(property = "select-class")
	private List<String> selectClass;

	@Parameter(property = "select-method")
	private List<String> selectMethod;

	@Parameter(property = "select-package")
	private List<String> selectPackage;

	@Parameter(property = "scan-classpath", defaultValue = "true")
	private boolean scanClasspath;

	@Parameter(property = "disable-banner", defaultValue = "true")
	private boolean disableBanner;

	@Parameter(property = "reports-dir", defaultValue = "${project.build.directory}/testReports")
	private File reportsDir;

	@Parameter(property = "include-classname")
	private String includeClassname;

	@Parameter(property = "exclude-classname")
	private String excludeClassname;

	@Parameter(property = "include-package")
	private List<String> includePackage;

	@Parameter(property = "exclude-package")
	private List<String> excludePackage;

	@Parameter(property = "include-engine")
	private List<String> includeEngine;

	@Parameter(property = "exclude-engine")
	private List<String> excludeEngine;

	@Parameter(property = "include-tag")
	private List<String> includeTag;

	@Parameter(property = "exclude-tag")
	private List<String> excludeTag;

	@Parameter(property = "fail-if-no-tests", defaultValue = "false")
	private boolean failIfNoTests;

	@Parameter(property = "details")
	private String details;

	@Parameter(property = "details-theme")
	private String detailsTheme;

	@Parameter(property = "single-color", defaultValue = "false")
	private boolean singleColor;

	@Parameter
	private Map<String, String> config;

	@Parameter
	private Map<String, String> frameworkProperties = new LinkedHashMap<>();

	/**
	 * The initial start level used for bundles and the OSGi framework.
	 */
	@Parameter(defaultValue = "4")
	private int applicationStartLevel;

	/**
	 * The start level used for bundles providing an <code>osgi.extender</code>
	 * capability.
	 */
	@Parameter(defaultValue = "1")
	private int extenderStartLevel;

	/**
	 * If bundles providing an <code>osgi.extender</code> capability should be
	 * started by default
	 */
	@Parameter(defaultValue = "true")
	private boolean startExtender;

	/**
	 * Value in seconds how long to wait until the framework reaches the final
	 * startlevel
	 */
	@Parameter(defaultValue = "30")
	private long startupTimout;

	@Parameter
	private boolean printBundles;

	@Parameter(property = "junit-platform.skip")
	private boolean skip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = getLog();
		if (skip) {
			log.info("Execution is skipped!");
			return;
		} else {
			log.info("Reports will be written to: " + reportsDir);
		}
		Collection<Path> projectDependencies;
		try {
			projectDependencies = projectManager.getProjectDependencies(mavenProject);
		} catch (Exception e) {
			throw new MojoExecutionException("Can't determine project dependencies!");
		}
		Set<Artifact> artifacts = mavenProject.getArtifacts();
		for (Artifact artifact : artifacts) {
			if (Artifact.SCOPE_TEST.equals(artifact.getScope()) && artifact.getArtifactHandler().isAddedToClasspath()) {
				log.debug("Adding test scoped artifact: " + artifact);
				projectDependencies.add(artifact.getFile().toPath());
			}
		}
		log.debug("Project Dependencies:");
		for (Path path : projectDependencies) {
			log.debug(path.getFileName().toString());
		}
		Map<String, String> frameworkProperties = getFrameworkProperties(
				Path.of(mavenProject.getBuild().getDirectory()).resolve("work"));
		frameworkProperties.putAll(this.frameworkProperties);
		frameworkProperties.put("eclipse.ignoreApp", "true");
		frameworkProperties.put("osgi.noShutdown", "true");
		ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class,
				getClass().getClassLoader());
		FrameworkFactory factory = loader.findFirst()
				.orElseThrow(() -> new MojoExecutionException("No ConnectFrameworkFactory found"));
		Framework framework = factory.newFramework(frameworkProperties);
		try {
			framework.init();
		} catch (BundleException e) {
			throw new MojoExecutionException("Initialize the framework failed!", e);
		}
		try {
			try {
				framework.start();
			} catch (BundleException e) {
				throw new MojoExecutionException("Start the framework failed!", e);
			}
			FrameworkWiring wiring = framework.adapt(FrameworkWiring.class);
			FrameworkStartLevel startLevel = framework.adapt(FrameworkStartLevel.class);
			startLevel.setInitialBundleStartLevel(applicationStartLevel);
			BundleContext systemBundleContext = framework.getBundleContext();
			log.debug("Install Bundles ...");
			for (Path path : projectDependencies) {
				if (path.getFileName().toString().startsWith("org.eclipse.osgi")) {
					continue;
				}
				try {
					Bundle bundle = systemBundleContext.installBundle(path.toString(), Files.newInputStream(path));
					log.debug("Installed " + bundle);
				} catch (BundleException | IOException e) {
					log.debug(path.getFileName() + ": " + e);
				}
			}
			log.debug("Resolve bundles...");
			wiring.resolveBundles(null);
			Bundle[] bundles = systemBundleContext.getBundles();
			setStartLevels(bundles);
			CountDownLatch latch = new CountDownLatch(1);
			startLevel.setStartLevel(applicationStartLevel, new FrameworkListener() {

				@Override
				public void frameworkEvent(FrameworkEvent event) {
					latch.countDown();
				}
			});
			try {
				if (!latch.await(startupTimout, TimeUnit.SECONDS)) {
					new MojoExecutionException("Framework did not reached the required startlevel in time!");
				}
			} catch (InterruptedException e) {
				return;
			}
			printBundleInfo(bundles, printBundles ? log::info : log::debug);
			Bundle testBundle = findTestProbe(bundles)
					.orElseThrow(() -> new MojoFailureException("Testprobe not found in Framework"));
			ToolProvider junit = findToolProvider(testBundle, bundles).orElseThrow(() -> new MojoFailureException(
					"No compatible tool provider for junit make sure to have defined a matching junit-platform-console in your pom!"));
			if (isFragment(testBundle)) {
				testBundle = getHost(testBundle).orElseThrow(
						() -> new MojoFailureException("Testprobe is a fragment but not attached to any host bundle"));
			}
			try {
				testBundle.start();
			} catch (BundleException e) {
				throw new MojoExecutionException("Testprobe " + testBundle + " can not be started!", e);
			}
			executeTestWithTestProbe(testBundle, junit, bundles);
		} finally {
			try {
				framework.stop();
			} catch (BundleException e) {
				// we can't really do anything or care much here...
			}
		}
	}

	private Optional<Bundle> findTestProbe(Bundle[] bundles) {
		File file = mavenProject.getArtifact().getFile();
		if (file != null) {
			for (Bundle bundle : bundles) {
				if (new File(bundle.getLocation()).equals(file)) {
					return Optional.of(bundle);
				}
			}
		}
		return Optional.empty();
	}

	private void printBundleInfo(Bundle[] bundles, Consumer<? super CharSequence> consumer) {
		consumer.accept("============ Bundles ==================");
		Comparator<Bundle> bySymbolicName = Comparator.comparing(Bundle::getSymbolicName,
				String.CASE_INSENSITIVE_ORDER);
		Comparator<Bundle> byState = Comparator.comparingInt(Bundle::getState);
		Arrays.stream(bundles).sorted(byState.thenComparing(bySymbolicName)).forEachOrdered(bundle -> {
			String state = toBundleState(bundle.getState());
			consumer.accept(state + " | " + bundle.getSymbolicName() + " (" + bundle.getVersion() + ") at "
					+ bundle.getLocation());
		});

	}

	private static String toBundleState(int state) {
		return switch (state) {
		case Bundle.ACTIVE -> "ACTIVE   ";
		case Bundle.INSTALLED -> "INSTALLED";
		case Bundle.RESOLVED -> "RESOLVED ";
		case Bundle.STARTING -> "STARTING ";
		case Bundle.STOPPING -> "STOPPING ";
		default -> String.valueOf(state);
		};
	}

	private void setStartLevels(Bundle[] bundles) {
		for (Bundle bundle : bundles) {
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			if (wiring != null) {
				List<BundleWire> providedWires = wiring.getProvidedWires("osgi.extender");
				if (providedWires.isEmpty()) {
					continue;
				}
				getLog().debug("Found extender bundle " + bundle);
				BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);
				startLevel.setStartLevel(extenderStartLevel);
				if (startExtender) {
					try {
						bundle.start();
					} catch (BundleException e) {
						getLog().debug("Extender bundle " + bundle + " can not be started!", e);
					}
				}
			}
		}
	}

	private Optional<ToolProvider> findToolProvider(Bundle testprobe, Bundle[] bundles) {
		for (Bundle bundle : bundles) {
			if (bundle == testprobe) {
				continue;
			}
			try {
				URL resource = bundle.getResource("META-INF/services/java.util.spi.ToolProvider");
				if (resource != null) {
					getLog().debug("Checking " + bundle + " for toolprovider...");
					BundleWiring wiring = bundle.adapt(BundleWiring.class);
					if (wiring == null) {
						return Optional.empty();
					}
					ServiceLoader<ToolProvider> sl = ServiceLoader.load(ToolProvider.class, wiring.getClassLoader());
					Optional<ToolProvider> provider = StreamSupport.stream(sl.spliterator(), false)
							.filter(p -> p.name().equals(JUNIT_TOOLPROVIDER_NAME)).findFirst();
					if (provider.isPresent()) {
						getLog().debug("--> found one: " + provider.get());
						return provider;
					}
				}
			} catch (Exception e) {
				// can't use it then!
			}
		}
		return Optional.empty();
	}

	private void executeTestWithTestProbe(Bundle bundle, ToolProvider junit, Bundle[] bundles)
			throws MojoFailureException, MojoExecutionException {
		Thread thread = Thread.currentThread();
		ClassLoader loader = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(new SPIBundleClassLoader(bundle, bundles, getLog()::debug));
			List<String> arguments = new ArrayList<>();
			arguments.add("execute");
			if (disableBanner) {
				arguments.add("--disable-banner");
			}
			if (selectClass != null) {
				for (String clz : selectClass) {
					arguments.add("--select-class");
					arguments.add(clz);
				}
			}
			if (selectMethod != null) {
				for (String method : selectMethod) {
					arguments.add("--select-method");
					arguments.add(method);
				}
			}
			if (selectPackage != null) {
				for (String pkg : selectPackage) {
					arguments.add("--select-package");
					arguments.add(pkg);
				}
			}
			if (scanClasspath) {
				arguments.add("--scan-classpath");
				arguments.add(mavenProject.getArtifact().getFile().getAbsolutePath());
			}
			if (includeClassname != null) {
				arguments.add("--include-classname");
				arguments.add(includeClassname);
			}
			if (excludeClassname != null) {
				arguments.add("--exclude-classname");
				arguments.add(excludeClassname);
			}
			if (includePackage != null) {
				for (String pkg : includePackage) {
					arguments.add("--include-package");
					arguments.add(pkg);
				}
			}
			if (excludePackage != null) {
				for (String pkg : excludePackage) {
					arguments.add("--exclude-package");
					arguments.add(pkg);
				}
			}
			if (includeEngine != null) {
				for (String engine : includeEngine) {
					arguments.add("--include-engine");
					arguments.add(engine);
				}
			}
			if (excludeEngine != null) {
				for (String engine : excludeEngine) {
					arguments.add("--exclude-engine");
					arguments.add(engine);
				}
			}
			if (includeTag != null) {
				for (String tag : includeTag) {
					arguments.add("--include-tag");
					arguments.add(tag);
				}
			}
			if (excludeTag != null) {
				for (String tag : excludeTag) {
					arguments.add("--exclude-tag");
					arguments.add(tag);
				}
			}
			if (config != null) {
				for (Entry<String, String> entry : config.entrySet()) {
					arguments.add("--config");
					arguments.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
				}
			}
			if (details != null) {
				arguments.add("--details");
				arguments.add(details);
			}
			if (detailsTheme != null) {
				arguments.add("--details-theme");
				arguments.add(detailsTheme);
			}
			if (singleColor) {
				arguments.add("--single-color");
			}
			arguments.add("--reports-dir");
			arguments.add(reportsDir.getAbsolutePath());
			if (failIfNoTests) {
				arguments.add("--fail-if-no-tests");
			}
			int exitCode = junit.run(System.out, System.err, arguments.toArray(String[]::new));
			if (exitCode == 0) {
				return;
			}
			if (exitCode == 2) {
				throw new MojoFailureException("No tests found!");
			}
			if (exitCode == 1) {
				throw new MojoFailureException("There are test failures");
			}
			throw new MojoExecutionException("Tool execution failed with exit code " + exitCode);
		} finally {
			thread.setContextClassLoader(loader);
		}
	}

	private Map<String, String> getFrameworkProperties(Path workDir) {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("osgi.configuration.area", workDir.resolve("configuration").toAbsolutePath().toString());
		map.put("osgi.instance.area", workDir.resolve("data").toAbsolutePath().toString());
		map.put("osgi.compatibility.bootdelegation", "true");
		map.put("osgi.classloader.copy.natives", "true");
		map.put("osgi.framework.useSystemProperties", "false");
		map.put("osgi.clean", "true");
		if (frameworkProperties != null) {
			map.putAll(frameworkProperties);
		}
		return map;
	}

	private static boolean isFragment(Bundle bundle) {
		BundleRevisions bundleRevisions = bundle.adapt(BundleRevisions.class);
		List<BundleRevision> revisions = bundleRevisions.getRevisions();
		if (revisions.isEmpty()) {
			return false;
		}
		return (revisions.get(0).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	private static Optional<Bundle> getHost(Bundle bundle) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null) {
			return Optional.empty();
		}
		List<BundleWire> hostWires = wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE);
		if (hostWires == null) {
			return Optional.empty();
		}
		return hostWires.stream().map(wire -> wire.getProvider().getBundle()).filter(Objects::nonNull).findFirst();
	}

}
