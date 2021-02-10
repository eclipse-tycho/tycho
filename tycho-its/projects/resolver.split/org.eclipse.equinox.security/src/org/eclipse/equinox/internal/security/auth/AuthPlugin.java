/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.auth;

import java.net.URL;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.security.storage.PasswordProviderSelector;
import org.eclipse.equinox.internal.security.storage.SecurePreferencesMapper;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

// XXX general comment: how this bundle reacts to dynamic events (registry, OSGi) ?

public class AuthPlugin implements BundleActivator {

	/**
	 * The unique identifier constant of this plug-in.
	 */
	public static final String PI_AUTH = "org.eclipse.equinox.security"; //$NON-NLS-1$

	private static AuthPlugin singleton;

	private BundleContext bundleContext;
	private ServiceTracker<?, DebugOptions> debugTracker = null;
	private ServiceTracker<?, Location> configTracker = null;
	private ServiceTracker<?, EnvironmentInfo> environmentTracker = null;
	private volatile ServiceTracker<?, FrameworkLog> logTracker = null;

	public static boolean DEBUG = false;
	public static boolean DEBUG_LOGIN_FRAMEWORK = false;

	/*
	 * Returns the singleton for this Activator. Callers should be aware that
	 * this will return null if the bundle is not active.
	 */
	public static AuthPlugin getDefault() {
		return singleton;
	}

	public AuthPlugin() {
		super();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		singleton = this;

		DEBUG = getBooleanOption(PI_AUTH + "/debug", false); //$NON-NLS-1$
		if (DEBUG)
			DEBUG_LOGIN_FRAMEWORK = getBooleanOption(PI_AUTH + "/debug/loginFramework", false); //$NON-NLS-1$

		// SecurePlatformInternal is started lazily when first SecureContext is created (this reduces 
		// time spend in the bundle activator).
	}

	@Override
	public void stop(BundleContext context) throws Exception {

		PasswordProviderSelector.stop();
		SecurePreferencesMapper.stop();
		SecurePlatformInternal.getInstance().stop();

		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}
		if (configTracker != null) {
			configTracker.close();
			configTracker = null;
		}
		if (environmentTracker != null) {
			environmentTracker.close();
			environmentTracker = null;
		}
		if (logTracker != null) {
			logTracker.close();
			logTracker = null;
		}
		bundleContext = null;
		singleton = null;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void logError(String msg, Throwable e) {
		RuntimeLog.log(new Status(IStatus.ERROR, PI_AUTH, msg, e));
	}

	public void logMessage(String msg) {
		RuntimeLog.log(new Status(IStatus.INFO, PI_AUTH, msg, null));
	}

	public boolean getBooleanOption(String option, boolean defaultValue) {
		if (debugTracker == null) {
			if (bundleContext == null)
				return defaultValue;
			debugTracker = new ServiceTracker<>(bundleContext, DebugOptions.class, null);
			debugTracker.open();
		}
		DebugOptions options = debugTracker.getService();
		if (options == null)
			return defaultValue;
		String value = options.getOption(option);
		if (value == null)
			return defaultValue;
		return value.equalsIgnoreCase("true"); //$NON-NLS-1$
	}

	public URL getConfigURL() {
		Filter filter = null;
		if (configTracker == null) {
			try {
				filter = bundleContext.createFilter(Location.CONFIGURATION_FILTER);
			} catch (InvalidSyntaxException e) {
				// should never happen
			}
			configTracker = new ServiceTracker<>(bundleContext, filter, null);
			configTracker.open();
		}
		Location location = configTracker.getService();
		if (location == null)
			return null;
		return location.getURL();
	}

	public EnvironmentInfo getEnvironmentInfoService() {
		if (environmentTracker == null) {
			if (bundleContext == null)
				return null;
			environmentTracker = new ServiceTracker<>(bundleContext, EnvironmentInfo.class, null);
			environmentTracker.open();
		}
		return environmentTracker.getService();
	}

	/**
	 * At present the logging for bundles positioned below org.eclipse.core.runtime
	 * in the bundle dependency stack is really sub-optimal.
	 * 
	 * In particular, logging with RuntimeLog on shutdown doesn't work as Platform
	 * shuts down (removing listeners from RuntimeLog) before this bundle shuts down.
	 * 
	 * As such, until there is improved logging, the errors that occur on shutdown
	 * should use this method. However, errors occuring during normal operations
	 * should use RuntimeLog as otherwise the Error View is not getting updated.
	 */
	public void frameworkLogError(String msg, int severity, Throwable e) {
		if ((logTracker == null) && (bundleContext != null)) {
			logTracker = new ServiceTracker<>(bundleContext, FrameworkLog.class, null);
			logTracker.open();
		}
		FrameworkLog log = (logTracker == null) ? null : (FrameworkLog) logTracker.getService();
		if (log != null)
			log.log(new FrameworkLogEntry(PI_AUTH, severity, 0, msg, 0, e, null));
		else {
			if (msg != null)
				System.err.println(msg);
			if (e != null)
				e.printStackTrace(System.err);
		}
	}

}
