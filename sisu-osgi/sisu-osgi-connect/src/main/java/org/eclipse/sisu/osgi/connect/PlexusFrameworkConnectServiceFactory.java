/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.sisu.osgi.connect;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The {@link PlexusFrameworkConnectServiceFactory} provides a
 * {@link EquinoxServiceFactory} using the <a href=
 * "http://docs.osgi.org/specification/osgi.core/8.0.0/framework.connect.html#framework.connect">Connect
 * Specification</a> that allows to connect the plexus-world with the maven
 * world.
 */
@Component(role = EquinoxServiceFactory.class, hint = "connect")
public class PlexusFrameworkConnectServiceFactory implements Initializable, Disposable, EquinoxServiceFactory {

	private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	@Requirement
	private Logger log;

	final Map<ClassLoader, PlexusConnectFramework> frameworkMap = new HashMap<>();

	@Requirement(role = EquinoxLifecycleListener.class)
	private Map<String, EquinoxLifecycleListener> lifecycleListeners;

	private final String name;

	public PlexusFrameworkConnectServiceFactory() {
		ClassLoader classLoader = getClass().getClassLoader();
		if (classLoader instanceof ClassRealm) {
			@SuppressWarnings("resource")
			ClassRealm classRealm = (ClassRealm) classLoader;
			name = classRealm.getId();
		} else {
			name = classLoader.toString();
		}
	}

	/**
	 * 
	 * 
	 * @param classloader the classloader to use for discovering bundles to add to
	 *                    the framework
	 * @return get (or creates) the Framework that is made of the given classloader
	 * @throws BundleException if creation of the framework failed
	 */
	synchronized PlexusConnectFramework getFramework(ClassLoader classloader) throws BundleException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classloader);
			PlexusConnectFramework framework = frameworkMap.get(classloader);
			if (framework != null) {
				return framework;
			}
			List<ClassRealm> realms = collectRealms(getRealm(classloader));
			log.debug("Create framework for " + this + " with classloader " + classloader);
			Map<String, Boolean> bundleStartMap = readBundles(classloader, new PlexusConnectFramework(null, log, this));
			Map<String, String> p = new HashMap<>();
			p.put("osgi.framework.useSystemProperties", "false");
			p.put("osgi.parentClassloader", "fwk");
			p.put(Constants.FRAMEWORK_STORAGE,
					System.getProperty("java.io.tmpdir") + File.separator + "plexus.osgi." + UUID.randomUUID());
			p.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "6");
			ServiceLoader<ConnectFrameworkFactory> sl = ServiceLoader.load(ConnectFrameworkFactory.class, classloader);
			ConnectFrameworkFactory factory = sl.iterator().next();
			PlexusModuleConnector connector = new PlexusModuleConnector(classloader, factory);
			Framework osgiFramework = factory.newFramework(p, connector);
			PlexusConnectFramework connectFramework = new PlexusConnectFramework(osgiFramework, log, this);
			osgiFramework.init(new FrameworkListener() {

				@Override
				public void frameworkEvent(FrameworkEvent event) {
					connectFramework.info(event.toString());
				}
			});
			frameworkMap.put(classloader, connectFramework);
			for (ClassRealm realm : realms) {
				connector.scanRealm(realm, osgiFramework.getBundleContext(), connectFramework);
			}
			osgiFramework.start();
			Map<String, List<Bundle>> bundles = Arrays.stream(osgiFramework.getBundleContext().getBundles())
					.collect(Collectors.groupingBy(Bundle::getSymbolicName));
			for (Entry<String, Boolean> entry : bundleStartMap.entrySet()) {
				List<Bundle> list = bundles.get(entry.getKey());
				if (list == null) {
					connectFramework.warn("Bundle " + entry.getKey() + " was not found in the framework!");
				} else if (entry.getValue()) {
					for (Bundle bundle : list) {
						try {
							bundle.start();
						} catch (BundleException e) {
							if (log.isDebugEnabled()) {
								connectFramework.warn("Can't start bundle " + bundle.getSymbolicName() + " "
										+ bundle.getVersion() + ": ", e);
							} else {
								connectFramework.warn("Can't start bundle " + bundle.getSymbolicName() + " "
										+ bundle.getVersion() + ": " + e);
							}
						}
					}
				}
			}
			for (EquinoxLifecycleListener listener : lifecycleListeners.values()) {
				listener.afterFrameworkStarted(connectFramework);
			}
			if (log.isDebugEnabled()) {
				printFrameworkState(osgiFramework, connectFramework);
			}
			return connectFramework;
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	private static List<ClassRealm> collectRealms(ClassRealm realm) {
		ArrayList<ClassRealm> result = new ArrayList<ClassRealm>(realm.getImportRealms());
		result.add(realm);
		return result;
	}

	protected ClassRealm getRealm(ClassLoader classloader) throws BundleException {
		if (classloader instanceof ClassRealm) {
			return (ClassRealm) classloader;
		}
		throw new BundleException("Not called from a ClassRealm");
	}

	private static Map<String, Boolean> readBundles(ClassLoader classloader, Logger logger) {
		Enumeration<URL> resources;
		try {
			resources = classloader.getResources("META-INF/sisu-connect.bundles");
		} catch (IOException e1) {
			return Map.of();
		}
		LinkedHashMap<String, Boolean> map = new LinkedHashMap<>();
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			logger.debug("Reading Bundles from " + url);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
				reader.lines().forEachOrdered(line -> {
					if (line.startsWith("#") || line.isBlank()) {
						return;
					}
					String[] split = line.split(",", 2);
					boolean start;
					if (split.length == 2) {
						start = Boolean.parseBoolean(split[1]);
					} else {
						start = false;
					}
					map.put(split[0], start);
				});
			} catch (IOException e) {
				logger.warn("Can't read bundle infos from url " + url);
			}
		}
		return map;
	}

	private static void printFrameworkState(Framework framework, Logger log) {
		Bundle[] bundles = framework.getBundleContext().getBundles();
		log.info("============ Framework Bundles ==================");
		Comparator<Bundle> bySymbolicName = Comparator.comparing(Bundle::getSymbolicName,
				String.CASE_INSENSITIVE_ORDER);
		Comparator<Bundle> byState = Comparator.comparingInt(Bundle::getState);
		Arrays.stream(bundles).sorted(byState.thenComparing(bySymbolicName)).forEachOrdered(bundle -> {
			log.info(toBundleState(bundle.getState()) + " | " + bundle.getSymbolicName());
		});
		ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> st = new ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime>(
				framework.getBundleContext(), ServiceComponentRuntime.class, null);
		st.open();
		try {
			ServiceComponentRuntime componentRuntime = st.getService();
			if (componentRuntime != null) {
				log.info("============ Framework Components ==================");
				Collection<ComponentDescriptionDTO> descriptionDTOs = componentRuntime.getComponentDescriptionDTOs();
				Comparator<ComponentConfigurationDTO> byComponentName = Comparator
						.comparing(dto -> dto.description.name, String.CASE_INSENSITIVE_ORDER);
				Comparator<ComponentConfigurationDTO> byComponentState = Comparator.comparingInt(dto -> dto.state);
				descriptionDTOs.stream().flatMap(dto -> componentRuntime.getComponentConfigurationDTOs(dto).stream())
						.sorted(byComponentState.thenComparing(byComponentName)).forEachOrdered(dto -> {
							if (dto.state == ComponentConfigurationDTO.FAILED_ACTIVATION) {
								log.info(toComponentState(dto.state) + " | " + dto.description.name + " | "
										+ dto.failure);
							} else {
								log.info(toComponentState(dto.state) + " | " + dto.description.name);
							}
						});
			} else {
				log.info("No service component runtime installed (or started) in this framework!");
			}
		} finally {
			st.close();
		}
	}

	private static String toComponentState(int state) {
		switch (state) {
		case ComponentConfigurationDTO.ACTIVE:
			return "ACTIVE     ";
		case ComponentConfigurationDTO.FAILED_ACTIVATION:
			return "FAILED     ";
		case ComponentConfigurationDTO.SATISFIED:
			return "SATISFIED  ";
		case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
		case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
			return "UNSATISFIED";
		default:
			return String.valueOf(state);
		}
	}

	private static String toBundleState(int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE   ";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED ";
		case Bundle.STARTING:
			return "STARTING ";
		case Bundle.STOPPING:
			return "STOPPING ";
		default:
			return String.valueOf(state);
		}
	}

	@Override
	public void dispose() {
		frameworkMap.values().forEach(connect -> {
			Framework fw = connect.getFramework();
			String storage = fw.getBundleContext().getProperty(Constants.FRAMEWORK_STORAGE);
			try {
				fw.stop();
			} catch (BundleException e) {
			}
			try {
				fw.waitForStop(TimeUnit.SECONDS.toMillis(10));
			} catch (InterruptedException e) {
			}
			if (storage != null) {
				FileUtils.deleteQuietly(new File(storage));
			}
		});
		frameworkMap.clear();
		PlexusFrameworkUtilHelper.factories.remove(this);
	}

	@Override
	public void initialize() throws InitializationException {

		log.debug("Init instance " + this + " [" + getClass().getClassLoader() + "]");
		PlexusFrameworkUtilHelper.factories.add(this);
	}

	@Override
	public <T> T getService(Class<T> clazz) {
		return locateClass(clazz, null, WALKER.getCallerClass());
	}

	@Override
	public <T> T getService(Class<T> clazz, String filter) {
		return locateClass(clazz, filter, WALKER.getCallerClass());
	}

	private <T> T locateClass(Class<T> clazz, String filter, Class<?> callerClass) {
		if (clazz == null || callerClass == null) {
			return null;
		}
		try {
			return getFramework(callerClass.getClassLoader()).getService(clazz, filter);
		} catch (BundleException e) {
			throw new RuntimeException("can't acquire the framework!", e);
		}
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}
}
