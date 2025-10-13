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

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.spi.ToolProvider;
import java.util.stream.StreamSupport;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Encapsulates the code that actually runs inside the OSGi framework.
 */
public class JUnitPlatformRunner implements Serializable, Function<BundleContext, JUnitPlatformRunnerResult> {

	private static final String JUNIT_TOOLPROVIDER_NAME = "junit";

	private String[] bundleLocations;
	private int applicationStartLevel;
	private int extenderStartLevel;
	private boolean startExtender;
	private long startupTimout;
	private boolean printBundles;
	private String artifactLocation;
	private String[] runnerArguments;

	public JUnitPlatformRunner(Collection<Path> bundlesToInstall, Path projectArtifact, int applicationStartLevel,
			int extenderStartLevel, boolean startExtender, long startupTimout, boolean printBundles,
			List<String> runnerArguments) {
		this.runnerArguments = runnerArguments.toArray(String[]::new);
		this.artifactLocation = projectArtifact.toAbsolutePath().toString();
		this.applicationStartLevel = applicationStartLevel;
		this.extenderStartLevel = extenderStartLevel;
		this.startExtender = startExtender;
		this.startupTimout = startupTimout;
		this.printBundles = printBundles;
		bundleLocations = bundlesToInstall.stream()
				.filter(p -> !p.getFileName().toString().startsWith("org.eclipse.osgi"))
				.map(p -> p.toAbsolutePath().toString()).toArray(String[]::new);
	}

	@Override
	public JUnitPlatformRunnerResult apply(BundleContext bundleContext) {
		Bundle framework = bundleContext.getBundle(0);
		FrameworkWiring wiring = framework.adapt(FrameworkWiring.class);
		FrameworkStartLevel startLevel = framework.adapt(FrameworkStartLevel.class);
		startLevel.setInitialBundleStartLevel(applicationStartLevel);
		JUnitPlatformRunnerResult result = new JUnitPlatformRunnerResult();
		result.debug("Install Bundles ...");
		List<Bundle> bundles = new ArrayList<>();
		for (String loc : bundleLocations) {
			Path path = Path.of(loc);
			try {
				Bundle bundle = bundleContext.installBundle(path.toString(), Files.newInputStream(path));
				result.debug("Installed " + bundle);
				bundles.add(bundle);
			} catch (BundleException | IOException e) {
				result.debug(path.getFileName() + ": " + e);
			}
		}
		result.debug("Resolve bundles...");
		wiring.resolveBundles(bundles);
		setStartLevels(bundles, result);
		CountDownLatch latch = new CountDownLatch(1);
		startLevel.setStartLevel(applicationStartLevel, new FrameworkListener() {

			@Override
			public void frameworkEvent(FrameworkEvent event) {
				latch.countDown();
			}
		});
		try {
			if (!latch.await(startupTimout, TimeUnit.SECONDS)) {
				return result.failure("Framework did not reached the required startlevel in time!");
			}
		} catch (InterruptedException e) {
			return result.setException(e);
		}
		printBundleInfo(bundles, printBundles ? result::info : result::debug);
		Optional<Bundle> testProbe = findTestProbe(bundles);
		if (testProbe.isEmpty()) {
			return result.failure("Testprobe not found in Framework");
		}
		Bundle testBundle = testProbe.get();
		Optional<ToolProvider> toolProvider = findToolProvider(testBundle, bundles, result);
		if (toolProvider.isEmpty()) {
			return result.failure(
					"No compatible tool provider for junit make sure to have defined a matching junit-platform-console in your pom!");
		}
		if (isFragment(testBundle)) {
			Optional<Bundle> host = getHost(testBundle);
			if (host.isEmpty()) {
				return result.failure(						"Testprobe is a fragment but not attached to any host bundle");	
			}
			testBundle = host.get();
		}
		try {
			testBundle.start();
		} catch (BundleException e) {
			return result.setException(
					new JUnitPlatformFailureException("Testprobe " + testBundle + " can not be started!", e));
		}
		Thread thread = Thread.currentThread();
		ClassLoader loader = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(new SPIBundleClassLoader(testBundle, bundles, result::debug));
			return result.setExitCode(toolProvider.get().run(System.out, System.err, runnerArguments));
		} finally {
			thread.setContextClassLoader(loader);
		}
	}


	private void setStartLevels(List<Bundle> bundles, JUnitPlatformRunnerResult result) {
		for (Bundle bundle : bundles) {
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			if (wiring != null) {
				List<BundleWire> providedWires = wiring.getProvidedWires("osgi.extender");
				if (providedWires.isEmpty()) {
					continue;
				}
				result.debug("Found extender bundle " + bundle);
				BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);
				startLevel.setStartLevel(extenderStartLevel);
				if (startExtender) {
					try {
						bundle.start();
					} catch (BundleException e) {
						result.debug("Extender bundle " + bundle + " can not be started: " + e);
					}
				}
			}
		}
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

	private Optional<Bundle> findTestProbe(List<Bundle> bundles) {
		for (Bundle bundle : bundles) {
			if (artifactLocation.equals(bundle.getLocation())) {
				return Optional.of(bundle);
			}
		}
		return Optional.empty();
	}

	private void printBundleInfo(List<Bundle> bundles, Consumer<String> consumer) {
		consumer.accept("============ Bundles ==================");
		Comparator<Bundle> bySymbolicName = Comparator.comparing(Bundle::getSymbolicName,
				String.CASE_INSENSITIVE_ORDER);
		Comparator<Bundle> byState = Comparator.comparingInt(Bundle::getState);
		bundles.stream().sorted(byState.thenComparing(bySymbolicName)).forEachOrdered(bundle -> {
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

	private Optional<ToolProvider> findToolProvider(Bundle testprobe, List<Bundle> bundles,
			JUnitPlatformRunnerResult result) {
		for (Bundle bundle : bundles) {
			if (bundle == testprobe) {
				continue;
			}
			try {
				URL resource = bundle.getResource("META-INF/services/java.util.spi.ToolProvider");
				if (resource != null) {
					result.debug("Checking " + bundle + " for toolprovider...");
					BundleWiring wiring = bundle.adapt(BundleWiring.class);
					if (wiring == null) {
						return Optional.empty();
					}
					ServiceLoader<ToolProvider> sl = ServiceLoader.load(ToolProvider.class, wiring.getClassLoader());
					Optional<ToolProvider> provider = StreamSupport.stream(sl.spliterator(), false)
							.filter(p -> p.name().equals(JUNIT_TOOLPROVIDER_NAME)).findFirst();
					if (provider.isPresent()) {
						result.debug("--> found one: " + provider.get());
						return provider;
					}
				}
			} catch (Exception e) {
				result.debug("Can't check " + bundle + ": " + e);
			}
		}
		return Optional.empty();
	}

}
