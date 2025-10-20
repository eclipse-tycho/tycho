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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The {@link PlexusFrameworkConnectServiceFactory} provides a
 * {@link EquinoxServiceFactory} using the <a href=
 * "http://docs.osgi.org/specification/osgi.core/8.0.0/framework.connect.html#framework.connect">Connect
 * Specification</a> that allows to connect the plexus-world with the maven
 * world.
 */
@Named("connect")
@Singleton
public class PlexusFrameworkConnectServiceFactory implements Initializable, Disposable, EquinoxServiceFactory {

	private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	@Inject
	private Logger log;

	private static final Map<ClassRealm, PlexusConnectFramework> frameworkMap = new HashMap<>();

	private static final Map<ClassLoader, ClassRealm> loaderMap = new HashMap<>();

	@Inject
	private Map<String, EquinoxLifecycleListener> lifecycleListeners;

	private final String name;

	public PlexusFrameworkConnectServiceFactory() {
		name = getName(getClass().getClassLoader());
	}

	protected String getName(ClassLoader classLoader) {
		return classLoader instanceof ClassRealm classRealm ? classRealm.getId() : classLoader.toString();
	}

	/**
	 * 
	 * 
	 * @param classloader the classloader to use for discovering bundles to add to
	 *                    the framework
	 * @return get (or creates) the Framework that is made of the given classloader
	 * @throws BundleException if creation of the framework failed
	 */
	synchronized PlexusConnectFramework getFramework(ClassRealm realm) throws BundleException {
		PlexusConnectFramework cachedFramework = frameworkMap.get(realm);
		if (cachedFramework != null) {
			return cachedFramework;
		}
		Framework foreignFramework = getForeignFramework(realm);
		if (foreignFramework != null) {
			PlexusConnectFramework connectFwk = new PlexusConnectFramework(foreignFramework, log, this, realm, true,
					null);
			frameworkMap.put(realm, connectFwk);
			return connectFwk;
		}
		if (log.isDebugEnabled()) {
			printRealm(realm, 0, new HashSet<>());
		}
		Collection<ClassRealm> realms = collectRealms(realm, new LinkedHashSet<>());

		log.debug("Create framework for " + this + " with Realm " + realm);
		String storagePath = createStoragePath();
		Logger fwLogger = new PlexusConnectFramework(null, log, this, realm, false, storagePath);
		Map<String, String> p = readProperties(realm, fwLogger);
		p.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
				"javax.security.auth.x500;version=\"1.3.0\", org.slf4j;version=\"1.7.37\"");
		p.put("osgi.framework.useSystemProperties", "false");
		p.put("osgi.parentClassloader", "fwk");
		p.put(Constants.FRAMEWORK_STORAGE, storagePath + File.separator + "storage");
		p.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "6");
		p.put("osgi.instance.area", storagePath + File.separator + "instance");

		var loader = ServiceLoader.load(ConnectFrameworkFactory.class, getClass().getClassLoader());
		ConnectFrameworkFactory factory = loader.findFirst()
				.orElseThrow(() -> new NoSuchElementException("No ConnectFrameworkFactory found"));

		PlexusModuleConnector connector = new PlexusModuleConnector(factory);
		Framework osgiFramework = factory.newFramework(p, connector);
		PlexusConnectFramework connectFramework = new PlexusConnectFramework(osgiFramework, log, this, realm, false,
				storagePath);
		PlexusFrameworkUtilHelper.registerHelper(connectFramework);
		osgiFramework.init(connectFramework);
		frameworkMap.put(realm, connectFramework);

		connectFramework.start(osgiFramework.getBundleContext());
		for (ClassRealm r : realms) {
			connector.installRealm(r, osgiFramework.getBundleContext(), connectFramework);
		}
		FrameworkWiring wiring = osgiFramework.adapt(FrameworkWiring.class);
		wiring.resolveBundles(Collections.emptyList());
		osgiFramework.start();

		for (EquinoxLifecycleListener listener : lifecycleListeners.values()) {
			connectFramework.debug("Calling " + listener);
			try {
				listener.afterFrameworkStarted(connectFramework);
			} catch (RuntimeException e) {
				log.warn("Internal error in EquinoxLifecycleListener " + listener, e);
			}
		}
		if (log.isDebugEnabled()) {
			printFrameworkState(osgiFramework, connectFramework);
		}
		return connectFramework;

	}

	private String createStoragePath() {
		try {
			Path createTempDirectory = Files.createTempDirectory("plexus.osgi.");
			return createTempDirectory.toFile().getAbsolutePath();
		} catch (IOException e) {
			return System.getProperty("java.io.tmpdir") + File.separator + "plexus.osgi." + UUID.randomUUID();
		}
	}

	private void printRealm(ClassRealm realm, int indent, Set<ClassRealm> printed) {
		if (printed.add(realm)) {
			ClassRealm parentRealm = realm.getParentRealm();
			if (parentRealm != null) {
				printRealm(parentRealm, indent, printed);
				indent += 2;
			}
			String indentation = StringUtils.repeat(" ", indent);
			System.out.println(indentation + "> " + realm.getId() + " (parent = " + realm.getParentRealm() + ")");
			Enumeration<URL> resources = realm.loadResourcesFromSelf(PlexusModuleConnector.MAVEN_EXTENSION_DESCRIPTOR);
			if (resources != null) {
				resources.asIterator().forEachRemaining(url -> System.out.println(indentation + "  " + url));
			}
			realm.display();
			for (ClassRealm imports : realm.getImportRealms()) {
				printRealm(imports, indent + 2, printed);
			}
		}
	}

	private static Collection<ClassRealm> collectRealms(ClassRealm realm, Collection<ClassRealm> realms) {
		if (realms.add(realm)) {
			ClassRealm parentRealm = realm.getParentRealm();
			if (parentRealm != null) {
				realms.add(parentRealm);
			}
			for (ClassRealm imported : realm.getImportRealms()) {
				collectRealms(imported, realms);
			}
		}
		return realms;
	}

	protected ClassRealm getRealm(ClassLoader classloader) {
		return classloader instanceof ClassRealm classRealm ? classRealm : //
				loaderMap.computeIfAbsent(classloader,
						cl -> new DummyClassRealm("Not called from a ClassRealm", cl, log));
	}

	private static Map<String, String> readProperties(ClassLoader classloader, Logger logger) {
		Map<String, String> frameworkProperties = new HashMap<>();
		Enumeration<URL> resources;
		try {
			resources = classloader.getResources("META-INF/sisu/connect.properties");
		} catch (IOException e1) {
			return frameworkProperties;
		}
		resources.asIterator().forEachRemaining(url -> {
			logger.debug("Reading properties from " + url);
			Properties properties = new Properties();
			try (InputStream stream = url.openStream()) {
				properties.load(stream);
			} catch (IOException e) {
				logger.warn("Cannot read properties from URL " + url);
			}
			for (String property : properties.stringPropertyNames()) {
				String value = properties.getProperty(property);
				frameworkProperties.merge(property, value, (v1, v2) -> v1 + "," + v2);
			}
		});
		return frameworkProperties;
	}

	private static void printFrameworkState(Framework framework, Logger log) {
		Bundle[] bundles = printBundles(framework, log);
		printComponents(framework, log);
		printExtensions(framework, log);
		printServices(log, bundles);
	}

	private static void printExtensions(Framework framework, Logger log) {
		log.info("============ Extension Registry ==================");
		ServiceTracker<IExtensionRegistry, IExtensionRegistry> st = new ServiceTracker<>(framework.getBundleContext(),
				IExtensionRegistry.class, null);
		st.open(true);
		try {
			IExtensionRegistry registry = st.getService();
			if (registry == null) {
				log.info("No IExtensionRegistry installed (or started) in this framework");
				return;
			}
			IExtensionPoint[] extensionPoints = registry.getExtensionPoints();
			for (IExtensionPoint point : extensionPoints) {
				log.info(point.getUniqueIdentifier() + " [contributed by " + point.getContributor() + "]");
				for (IExtension extention : point.getExtensions()) {
					log.info("\t" + extention.getUniqueIdentifier() + " [from " + extention.getContributor().getName()
							+ "]");
					for (IConfigurationElement element : extention.getConfigurationElements()) {
						printConfigElement(element, 2, log);
					}
				}
			}
		} finally {
			st.close();
		}
	}



	private static void printConfigElement(IConfigurationElement element, int level, Logger log) {
		StringBuilder sb = new StringBuilder();
		sb.append("\t".repeat(level));
		sb.append(element.getName());
		for (String attr : element.getAttributeNames()) {
			sb.append(' ');
			sb.append(attr);
			sb.append('=');
			sb.append(element.getAttribute(attr));
		}
		String value = element.getValue();
		if (value != null) {
			sb.append(" @value = ");
			sb.append(value);
		}
		log.info(sb.toString());
		for (IConfigurationElement child : element.getChildren()) {
			printConfigElement(child, level + 1, log);
		}
	}

	private static void printComponents(Framework framework, Logger log) {
		ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> st = new ServiceTracker<>(
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
							for (int i = 0; i < dto.unsatisfiedReferences.length; i++) {
								UnsatisfiedReferenceDTO ref = dto.unsatisfiedReferences[i];
								log.info("\t" + ref.name + " is missing");
							}
						});
			} else {
				log.info("No service component runtime installed (or started) in this framework");
			}
		} finally {
			st.close();
		}
	}

	private static void printServices(Logger log, Bundle[] bundles) {
		log.info("============ Registered Services ==================");
		Arrays.stream(bundles).map(Bundle::getRegisteredServices).filter(Objects::nonNull).flatMap(Arrays::stream)
				.forEach(reference -> {
					Object service = reference.getProperty(Constants.OBJECTCLASS);
					if (service instanceof Object[] objects) {
						if (objects.length == 1) {
							service = objects[0];
						} else {
							service = Arrays.toString(objects);
						}
					}
					log.info(service + " registered by " + reference.getBundle().getSymbolicName() + " | "
							+ reference.getProperties());
				});
	}

	private static Bundle[] printBundles(Framework framework, Logger log) {
		Bundle[] bundles = framework.getBundleContext().getBundles();
		log.info("============ Framework Bundles ==================");
		Comparator<Bundle> bySymbolicName = Comparator.comparing(Bundle::getSymbolicName,
				String.CASE_INSENSITIVE_ORDER);
		Comparator<Bundle> byState = Comparator.comparingInt(Bundle::getState);
		Arrays.stream(bundles).sorted(byState.thenComparing(bySymbolicName)).forEachOrdered(bundle -> {
			String state = toBundleState(bundle.getState());
			log.info(state + " | " + bundle.getSymbolicName() + " (" + bundle.getVersion() + ") at "
					+ bundle.getLocation());
		});
		return bundles;
	}

	private static String toComponentState(int state) {
		return switch (state) {
		case ComponentConfigurationDTO.ACTIVE -> "ACTIVE     ";
		case ComponentConfigurationDTO.FAILED_ACTIVATION -> "FAILED     ";
		case ComponentConfigurationDTO.SATISFIED -> "SATISFIED  ";
		case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION, ComponentConfigurationDTO.UNSATISFIED_REFERENCE ->
			"UNSATISFIED";
		default -> String.valueOf(state);
		};
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

	@Override
	public void dispose() {
		frameworkMap.values().removeIf(connect -> {
			if (connect.factory != this) {
				return false;
			}
			String storagePath = connect.getStoragePath();
			try {
				if (!connect.foreign) {
					Framework fw = connect.getFramework();
					BundleContext systemContext = fw.getBundleContext();
					try {
						connect.stop(systemContext);
						try {
							fw.stop();
						} catch (BundleException e) {
						}
						try {
							fw.waitForStop(TimeUnit.SECONDS.toMillis(10));
						} catch (InterruptedException e) {
						}
					} finally {
						PlexusFrameworkUtilHelper.unregisterHelper(connect);
					}
				}
			} catch (Throwable t) {
				// see for example https://github.com/eclipse-equinox/equinox/issues/169
				System.err.println("Error on disposing framework: " + t);
			} finally {
				if (!connect.foreign && storagePath != null) {
					// try delete always whatever happens to not leave garbage around!
					FileUtils.deleteQuietly(new File(storagePath));
				}
			}
			return true;
		});
	}

	@Override
	public void initialize() throws InitializationException {
		log.debug("Init instance " + this + " [" + getClass().getClassLoader() + "]");
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
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			if (clazz == null || callerClass == null) {
				return null;
			}
			try {
				ClassRealm realm = getRealm(callerClass.getClassLoader());
				return getFramework(realm).getService(clazz, filter);
			} catch (BundleException e) {
				throw new RuntimeException("can't acquire the framework!", e);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public String toString() {
		return name;
	}

	public static Framework getForeignFramework(ClassRealm realm) {
		Class<?> thisClass = PlexusFrameworkConnectServiceFactory.class;
		Class<?> foreignFactoryClass = realm.loadClassFromSelf(thisClass.getName());
		if (foreignFactoryClass != null && foreignFactoryClass != thisClass) {
			try {
				Method method = foreignFactoryClass.getMethod("getOsgiFramework", ClassRealm.class);
				return (Framework) method.invoke(null, realm);
			} catch (ReflectiveOperationException e) {
			}
		}
		return null;
	}

	// DO NOT DELETE THIS METHOD! It is referenced by reflection in
	// getForeignFramework!
	public static Framework getOsgiFramework(ClassRealm realm) {
		PlexusConnectFramework cachedFramework = frameworkMap.get(realm);
		if (cachedFramework != null) {
			return cachedFramework.getFramework();
		}
		return null;
	}
}
