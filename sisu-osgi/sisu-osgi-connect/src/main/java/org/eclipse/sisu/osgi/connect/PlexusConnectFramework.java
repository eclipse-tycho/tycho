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
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.connect.FrameworkUtilHelper;
import org.osgi.framework.launch.Framework;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

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
	private ServiceTracker<LogReaderService, LogReaderService> serviceTracker;

	PlexusConnectFramework(Framework framework, Logger logger, PlexusFrameworkConnectServiceFactory factory,
			ClassRealm realm, boolean foreign) {
		this.framework = framework;
		this.logger = logger;
		this.factory = factory;
		this.realm = realm;
		this.foreign = foreign;
	}

	public String getUuid() {
		return uuid;
	}

	public Framework getFramework() {
		return framework;
	}

	@Override
	public void debug(String message) {
		debug(message, null);
	}

	@Override
	public void debug(String message, Throwable throwable) {
		logger.debug(format(message), throwable);
	}

	private String format(String message) {
		return String.format("[%s][%s] %s", getUuid(), realm.getId(), message);
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public void info(String message) {
		info(message, null);
	}

	@Override
	public void info(String message, Throwable throwable) {
		logger.info(format(message), throwable);
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public void warn(String message) {
		warn(message, null);
	}

	@Override
	public void warn(String message, Throwable throwable) {
		logger.warn(format(message), throwable);
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public void error(String message) {
		error(message, null);
	}

	@Override
	public void error(String message, Throwable throwable) {
		logger.error(format(message), throwable);
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public void fatalError(String message) {
		logger.fatalError(format(message));
	}

	@Override
	public void fatalError(String message, Throwable throwable) {
		logger.fatalError(format(message), throwable);
	}

	@Override
	public boolean isFatalErrorEnabled() {
		return logger.isFatalErrorEnabled();
	}

	@Override
	public int getThreshold() {
		return logger.getThreshold();
	}

	@Override
	public void setThreshold(int threshold) {
		logger.setThreshold(threshold);
	}

	@Override
	public Logger getChildLogger(String name) {
		return logger.getChildLogger(format(name));
	}

	@Override
	public String getName() {
		return String.format("%s (realm = %s, factory = %s)", uuid, realm.getId(), factory);
	}

	@Override
	public EquinoxServiceFactory getServiceFactory() {
		return this;
	}

	@Override
	public <T> void registerService(Class<T> clazz, T service) {
		registerService(clazz, service, null);
	}

	@Override
	public <T> void registerService(Class<T> clazz, T service, Dictionary<String, ?> properties) {
		framework.getBundleContext().registerService(clazz, service, properties);
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
				tracker.open();
				return tracker;
			});
			if (filter == null) {
				return clazz.cast(serviceTracker.getService());
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
			debug("Searching bundle for class " + classFromBundle + " and location " + location);
			BundleContext bundleContext = getFramework().getBundleContext();
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

	@Override
	public void logged(LogEntry entry) {
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

	@Override
	public void start(BundleContext context) {
		context.addFrameworkListener(this);
		serviceTracker = new ServiceTracker<>(context, LogReaderService.class, new ServiceTrackerCustomizer<>() {

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
		serviceTracker.open();
	}

	@Override
	public void stop(BundleContext context) {
		context.removeFrameworkListener(this);
		serviceTracker.close();
	}
}
