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
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
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
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
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

//	@Requirement(role = EquinoxLifecycleListener.class)
//	private Map<String, EquinoxLifecycleListener> lifecycleListeners;

	@Requirement
	private PlexusContainer plexusContainer;

	private final String name;

	public PlexusFrameworkConnectServiceFactory() {
		name = getName(getClass().getClassLoader());
	}

	protected String getName(ClassLoader classLoader) {
		if (classLoader instanceof ClassRealm) {
			@SuppressWarnings("resource")
			ClassRealm classRealm = (ClassRealm) classLoader;
			return classRealm.getId();
		} else {
			return classLoader.toString();
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
			ClassRealm realm = getRealm(classloader);
			if (log.isDebugEnabled()) {
				printRealm(realm, 0, new HashSet<ClassRealm>());
			}
			Collection<ClassRealm> realms = collectRealms(realm, new LinkedHashSet<ClassRealm>());
			log.debug("Create framework for " + this + " with classloader " + classloader);
			Logger fwLogger = new PlexusConnectFramework(null, log, this, realm);
			Map<String, String> p = readProperties(classloader, fwLogger);
			p.put("osgi.framework.useSystemProperties", "false");
			p.put("osgi.parentClassloader", "fwk");
			p.put(Constants.FRAMEWORK_STORAGE,
					System.getProperty("java.io.tmpdir") + File.separator + "plexus.osgi." + UUID.randomUUID());
			p.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "6");
			ServiceLoader<ConnectFrameworkFactory> sl = ServiceLoader.load(ConnectFrameworkFactory.class, classloader);
			ConnectFrameworkFactory factory = sl.iterator().next();
			PlexusModuleConnector connector = new PlexusModuleConnector(factory);
			Framework osgiFramework = factory.newFramework(p, connector);
			PlexusConnectFramework connectFramework = new PlexusConnectFramework(osgiFramework, log, this, realm);
			osgiFramework.init(new FrameworkListener() {

				@Override
				public void frameworkEvent(FrameworkEvent event) {
					connectFramework.info(event.toString());
				}
			});
			frameworkMap.put(classloader, connectFramework);
			osgiFramework.getBundleContext().addFrameworkListener(new FrameworkListener() {

				@Override
				public void frameworkEvent(FrameworkEvent event) {
					if (event.getType() == FrameworkEvent.ERROR) {
						log.error(event.getBundle().getSymbolicName(), event.getThrowable());
					}
				}
			});
			for (ClassRealm r : realms) {
				connector.installRealm(r, osgiFramework.getBundleContext(), connectFramework);
			}
			osgiFramework.start();

			try {
				Collection<EquinoxLifecycleListener> values = plexusContainer.lookupMap(EquinoxLifecycleListener.class)
						.values();
				for (EquinoxLifecycleListener listener : values) {
					connectFramework.debug("Calling " + listener + "...");
					listener.afterFrameworkStarted(connectFramework);
				}
			} catch (ComponentLookupException e) {
			}
			if (log.isDebugEnabled()) {
			printFrameworkState(osgiFramework, connectFramework);
			}
			return connectFramework;
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	private void printRealm(ClassRealm realm, int indent, Set<ClassRealm> printed) {
		if (printed.add(realm)) {
			ClassRealm parentRealm = realm.getParentRealm();
			if (parentRealm != null) {
				printRealm(parentRealm, indent, printed);
				indent += 2;
			}
			for (int j = 0; j < indent; j++) {
				System.out.print(' ');
			}
			System.out.println("> " + realm.getId() + " (parent = " + realm.getParentRealm() + ")");
			Enumeration<URL> resources = realm.loadResourcesFromSelf(PlexusModuleConnector.MAVEN_EXTENSION_DESCRIPTOR);
			while (resources != null && resources.hasMoreElements()) {
				URL url = resources.nextElement();
				for (int j = 0; j < indent; j++) {
					System.out.print(' ');
				}
				System.out.println("  " + url);
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

	protected ClassRealm getRealm(ClassLoader classloader) throws BundleException {
		if (classloader instanceof ClassRealm) {
			return (ClassRealm) classloader;
		}
		throw new BundleException("Not called from a ClassRealm");
	}

	private static Map<String, String> readProperties(ClassLoader classloader, Logger logger) {
		Map<String, String> frameworkProperties = new HashMap<String, String>();
		Enumeration<URL> resources;
		try {
			resources = classloader.getResources("META-INF/sisu-connect.properties");
		} catch (IOException e1) {
			return frameworkProperties;
		}
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			logger.debug("Reading properties from " + url);
			Properties properties = new Properties();
			try (InputStream stream = url.openStream()) {
				properties.load(stream);
			} catch (IOException e) {
				logger.warn("Can't read properties from url " + url);
			}
			for (String property : properties.stringPropertyNames()) {
				String existing = frameworkProperties.get(property);
				if (existing == null) {
					frameworkProperties.put(property, properties.getProperty(property));
				} else {
					frameworkProperties.put(property, existing + "," + properties.getProperty(property));
				}
			}
		}
		return frameworkProperties;
	}

	private static void printFrameworkState(Framework framework, Logger log) {
		Bundle[] bundles = framework.getBundleContext().getBundles();
		log.info("============ Framework Bundles ==================");
		Comparator<Bundle> bySymbolicName = Comparator.comparing(Bundle::getSymbolicName,
				String.CASE_INSENSITIVE_ORDER);
		Comparator<Bundle> byState = Comparator.comparingInt(Bundle::getState);
		Arrays.stream(bundles).sorted(byState.thenComparing(bySymbolicName)).forEachOrdered(bundle -> {
			log.info(toBundleState(bundle.getState()) + " | " + bundle.getSymbolicName() + " (" + bundle.getVersion()
					+ ")");
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
							for (int i = 0; i < dto.unsatisfiedReferences.length; i++) {
								UnsatisfiedReferenceDTO ref = dto.unsatisfiedReferences[i];
								log.info("\t" + ref.name + " is missing");
							}
						});
			} else {
				log.info("No service component runtime installed (or started) in this framework!");
			}
		} finally {
			st.close();
		}
		log.info("============ Registered Services ==================");
		Arrays.stream(bundles).map(bundle -> bundle.getRegisteredServices()).filter(Objects::nonNull)
				.flatMap(Arrays::stream).forEach(reference -> {
			Object service = reference.getProperty(Constants.OBJECTCLASS);
			if (service instanceof Object[]) {
				Object[] objects = (Object[]) service;
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

	@Override
	public String toString() {
		return name;
	}
}
