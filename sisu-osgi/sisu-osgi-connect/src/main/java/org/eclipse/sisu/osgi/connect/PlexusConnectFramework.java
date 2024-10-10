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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.connect.FrameworkUtilHelper;
import org.osgi.framework.launch.Framework;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * 
 * This class captures some state and allows to identify a connect frame work
 * created by plexus
 *
 */
class PlexusConnectFramework //
		implements Logger, EmbeddedEquinox, EquinoxServiceFactory, FrameworkUtilHelper, FrameworkListener, LogListener,
		BundleActivator {

	private final Framework framework;
	private final Logger logger;
	private final String uuid = UUID.randomUUID().toString();
	private final Map<Class<?>, ServiceTracker<?, ?>> trackerMap = new ConcurrentHashMap<>();
	private final ClassRealm realm;
	final PlexusFrameworkConnectServiceFactory factory;
	final boolean foreign;
	private ServiceTracker<LogReaderService, LogReaderService> logReaderServiceTracker;
	private String storagePath;

	PlexusConnectFramework(Framework framework, Logger logger, PlexusFrameworkConnectServiceFactory factory,
			ClassRealm realm, boolean foreign, String storagePath) {
		this.framework = framework;
		this.logger = logger;
		this.factory = factory;
		this.realm = realm;
		this.foreign = foreign;
		this.storagePath = storagePath;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public String getUuid() {
		return uuid;
	}

	public Framework getFramework() {
		return framework;
	}


	// Logger

	@Override
	public String getName() {
		return String.format("%s (realm = %s, factory = %s)", uuid, realm.getId(), factory);
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@Override
	public void trace(String s) {
		logger.trace(s);
	}

	@Override
	public void trace(String s, Object o) {
		logger.trace(s, o);
	}

	@Override
	public void trace(String s, Object o, Object o1) {
		logger.trace(s, o, o1);
	}

	@Override
	public void trace(String s, Object... objects) {
		logger.trace(s, objects);
	}

	@Override
	public void trace(String s, Throwable throwable) {
		logger.trace(s, throwable);
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return logger.isTraceEnabled(marker);
	}

	@Override
	public void trace(Marker marker, String s) {
		logger.trace(marker, s);
	}

	@Override
	public void trace(Marker marker, String s, Object o) {
		logger.trace(marker, s, o);
	}

	@Override
	public void trace(Marker marker, String s, Object o, Object o1) {
		logger.trace(marker, s, o, o1);
	}

	@Override
	public void trace(Marker marker, String s, Object... objects) {
		logger.trace(marker, s, objects);
	}

	@Override
	public void trace(Marker marker, String s, Throwable throwable) {
		logger.trace(marker, s, throwable);
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public void debug(String s) {
		logger.debug(s);
	}

	@Override
	public void debug(String s, Object o) {
		logger.debug(s, o);
	}

	@Override
	public void debug(String s, Object o, Object o1) {
		logger.debug(s, o, o1);
	}

	@Override
	public void debug(String s, Object... objects) {
		logger.debug(s, objects);
	}

	@Override
	public void debug(String s, Throwable throwable) {
		logger.debug(s, throwable);
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return logger.isDebugEnabled(marker);
	}

	@Override
	public void debug(Marker marker, String s) {
		logger.debug(marker, s);
	}

	@Override
	public void debug(Marker marker, String s, Object o) {
		logger.debug(marker, s, o);
	}

	@Override
	public void debug(Marker marker, String s, Object o, Object o1) {
		logger.debug(marker, s, o, o1);
	}

	@Override
	public void debug(Marker marker, String s, Object... objects) {
		logger.debug(marker, s, objects);
	}

	@Override
	public void debug(Marker marker, String s, Throwable throwable) {
		logger.debug(marker, s, throwable);
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public void info(String s) {
		logger.info(s);
	}

	@Override
	public void info(String s, Object o) {
		logger.info(s, o);
	}

	@Override
	public void info(String s, Object o, Object o1) {
		logger.info(s, o, o1);
	}

	@Override
	public void info(String s, Object... objects) {
		logger.info(s, objects);
	}

	@Override
	public void info(String s, Throwable throwable) {
		logger.info(s, throwable);
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return logger.isInfoEnabled(marker);
	}

	@Override
	public void info(Marker marker, String s) {
		logger.info(marker, s);
	}

	@Override
	public void info(Marker marker, String s, Object o) {
		logger.info(marker, s, o);
	}

	@Override
	public void info(Marker marker, String s, Object o, Object o1) {
		logger.info(marker, s, o, o1);
	}

	@Override
	public void info(Marker marker, String s, Object... objects) {
		logger.info(marker, s, objects);
	}

	@Override
	public void info(Marker marker, String s, Throwable throwable) {
		logger.info(marker, s, throwable);
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public void warn(String s) {
		logger.warn(s);
	}

	@Override
	public void warn(String s, Object o) {
		logger.warn(s, o);
	}

	@Override
	public void warn(String s, Object... objects) {
		logger.warn(s, objects);
	}

	@Override
	public void warn(String s, Object o, Object o1) {
		logger.warn(s, o, o1);
	}

	@Override
	public void warn(String s, Throwable throwable) {
		logger.warn(s, throwable);
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return logger.isWarnEnabled(marker);
	}

	@Override
	public void warn(Marker marker, String s) {
		logger.warn(marker, s);
	}

	@Override
	public void warn(Marker marker, String s, Object o) {
		logger.warn(marker, s, o);
	}

	@Override
	public void warn(Marker marker, String s, Object o, Object o1) {
		logger.warn(marker, s, o, o1);
	}

	@Override
	public void warn(Marker marker, String s, Object... objects) {
		logger.warn(marker, s, objects);
	}

	@Override
	public void warn(Marker marker, String s, Throwable throwable) {
		logger.warn(marker, s, throwable);
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public void error(String s) {
		logger.error(s);
	}

	@Override
	public void error(String s, Object o) {
		logger.error(s, o);
	}

	@Override
	public void error(String s, Object o, Object o1) {
		logger.error(s, o, o1);
	}

	@Override
	public void error(String s, Object... objects) {
		logger.error(s, objects);
	}

	@Override
	public void error(String s, Throwable throwable) {
		logger.error(s, throwable);
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return logger.isErrorEnabled(marker);
	}

	@Override
	public void error(Marker marker, String s) {
		logger.error(marker, s);
	}

	@Override
	public void error(Marker marker, String s, Object o) {
		logger.error(marker, s, o);
	}

	@Override
	public void error(Marker marker, String s, Object o, Object o1) {
		logger.error(marker, s, o, o1);
	}

	@Override
	public void error(Marker marker, String s, Object... objects) {
		logger.error(marker, s, objects);
	}

	@Override
	public void error(Marker marker, String s, Throwable throwable) {
		logger.error(marker, s, throwable);
	}


	// End Logger

	@Override
	public EquinoxServiceFactory getServiceFactory() {
		return this;
	}

	@Override
	public <T> void registerService(Class<T> clazz, T service) {
		registerService(clazz, service, Map.of());
	}

	@Override
	public <T> void registerService(Class<T> clazz, T service, Map<String, ?> properties) {
		framework.getBundleContext().registerService(clazz, service, FrameworkUtil.asDictionary(properties));
	}

	@Override
	public <T> T getService(Class<T> clazz) {
		return getService(clazz, null);
	}

	@Override
	public <T> T getService(Class<T> clazz, String filter) {
		try {
			ServiceTracker<?, ?> serviceTracker = trackerMap.computeIfAbsent(clazz, cls -> {
				ServiceTracker<?, ?> tracker = new ServiceTracker<>(framework.getBundleContext(), cls, null);
				// Sometimes Equinox thinks that classes are not compatible even if they
				// are...?!? So we track all services here in case of a DummyClassRealm
				tracker.open(realm instanceof DummyClassRealm);
				return tracker;
			});
			if (filter == null) {
				Object service = serviceTracker.getService();
				return clazz.cast(service);
			}
			Filter f = framework.getBundleContext().createFilter(filter);
			for (var entry : serviceTracker.getTracked().entrySet()) {
				if (f.match(entry.getKey())) {
					return clazz.cast(entry.getValue());
				}
			}
			return null;
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		URI location = getLocationFromClass(classFromBundle);
		if (location != null) {
			BundleContext bundleContext = getFramework().getBundleContext();
			if (bundleContext == null) {
				// already shut down
				return Optional.empty();
			}
			debug("Searching bundle for class " + classFromBundle + " and location " + location);
			Bundle[] bundles = bundleContext.getBundles();
			for (Bundle bundle : bundles) {
				String bundleLocation = bundle.getLocation();
				if (locationsMatch(location, bundleLocation)) {
					debug("Return bundle " + bundle.getSymbolicName() + " for location " + location);
					return Optional.of(bundle);
				}
			}
			if (classFromBundle.getClassLoader() == BundleContext.class.getClassLoader()) {
				// TODO should this really happen? This is not unique!
				return Optional.of(bundleContext.getBundle(0));
			}
			debug("No bundle matched for " + location);
		}
		return Optional.empty();
	}

	private String format(String message) {
		return String.format("[%s][%s] %s", getUuid(), realm.getId(), message);
	}

	static URI getLocationFromClass(Class<?> classFromBundle) {
		ProtectionDomain domain = classFromBundle.getProtectionDomain();
		if (domain == null) {
			return null;
		}
		CodeSource codeSource = domain.getCodeSource();
		if (codeSource == null) {
			return null;
		}
		URL url = codeSource.getLocation();
		if (url == null) {
			return null;
		}
		try {
			return url.toURI().normalize();
		} catch (URISyntaxException e) {
			return null;
		}
	}

	static boolean locationsMatch(URI location, String bundleLocation) {
		if (bundleLocation == null) {
			return false;
		}
		return location.equals(new File(bundleLocation).toURI().normalize());
	}

	@Override
	public String toString() {
		return format(getClass().getSimpleName());
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		if (logger.isDebugEnabled()) {
			if (event.getType() == FrameworkEvent.ERROR) {
				error(event.getBundle().getSymbolicName(), event.getThrowable());
			}
			if (event.getType() == FrameworkEvent.WARNING) {
				warn(event.getBundle().getSymbolicName(), event.getThrowable());
			}
			if (event.getType() == FrameworkEvent.INFO) {
				info(event.getBundle().getSymbolicName(), event.getThrowable());
			}
		}
	}

	@Override
	public void logged(LogEntry entry) {
		if (isOnlyDebug(entry) && !logger.isDebugEnabled()) {
			return;
		}
		switch (entry.getLogLevel()) {
		case AUDIT:
		case ERROR:
			error(entry.getMessage(), entry.getException());
			break;
		case WARN:
			warn(entry.getMessage(), entry.getException());
			break;
		case INFO:
			info(entry.getMessage(), entry.getException());
			break;
		case TRACE:
		case DEBUG:
			debug(entry.getMessage(), entry.getException());
			break;
		}
	}

	private static boolean isOnlyDebug(LogEntry entry) {
		return entry.getException() instanceof BundleException;
	}

	@Override
	public void start(BundleContext context) {
		context.addFrameworkListener(this);
		logReaderServiceTracker = new ServiceTracker<>(context, LogReaderService.class, new ServiceTrackerCustomizer<>() {

			@Override
			public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
				LogReaderService service = context.getService(reference);
				if (service != null) {
					service.addLogListener(PlexusConnectFramework.this);
				}
				return service;
			}

			@Override
			public void modifiedService(ServiceReference<LogReaderService> reference, LogReaderService service) {
			}

			@Override
			public void removedService(ServiceReference<LogReaderService> reference, LogReaderService service) {
				service.removeLogListener(PlexusConnectFramework.this);
				context.ungetService(reference);
			}
		});
		logReaderServiceTracker.open();
	}

	@Override
	public void stop(BundleContext context) {
		context.removeFrameworkListener(this);
		logReaderServiceTracker.close();
		trackerMap.values().forEach(ServiceTracker::close);
		trackerMap.clear();
	}
}
