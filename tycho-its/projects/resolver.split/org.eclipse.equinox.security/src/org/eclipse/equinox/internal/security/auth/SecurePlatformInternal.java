/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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

import java.io.IOException;
import java.net.URL;
import java.security.Security;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.security.auth.ext.loader.ExtCallbackHandlerLoader;
import org.osgi.framework.BundleContext;

// TBD what happens for server-side implementations if configurations are shared across all processes on VM?

public class SecurePlatformInternal {

	private static final String VM_PROPERTY = "equinox.security.vm"; //$NON-NLS-1$
	private static final String SERVER_VM = "server"; //$NON-NLS-1$

	private static final String PROVIDER_URL_BASE = "login.config.url.";//$NON-NLS-1$
	private static final int MAX_PROVIDER_URL_COUNT = 777; // arbitrary upper limit on the number of provider URLs
	private Configuration defaultConfiguration;
	private ExtCallbackHandlerLoader callbackHandlerLoader = new ExtCallbackHandlerLoader();

	private boolean running = false;
	private static final SecurePlatformInternal s_instance = new SecurePlatformInternal();

	private SecurePlatformInternal() {
		// hides default constructor
	}

	public static final SecurePlatformInternal getInstance() {
		return s_instance;
	}

	public CallbackHandler loadCallbackHandler(String configurationName) {
		return callbackHandlerLoader.loadCallbackHandler(configurationName);
	}

	/**
	 * Java docs specify that if multiple config files are passed in, they will be merged into one file.
	 * Hence, aside from implementation details, no priority information is specified by the order
	 * of config files. In this implementation we add customer's config file to the end of the list.
	 * 
	 * This method substitutes default login configuration:
	 * Configuration Inquiries -> ConfigurationFederator ->
	 * 		1) Extension Point supplied config providers;
	 * 		2) default Java config provider ("login.configuration.provider")
	 */
	public void start() {
		if (running)
			return;

		// Kludge for the bug 215828 "JAAS and server-side Eclipse": for the time being configuration 
		// substitution is turned off if running on a server. It is likely possible to work around 
		// configuration substitution using Java 5 methods, but not Java 1.4
		BundleContext context = AuthPlugin.getDefault().getBundleContext();
		String vmType = context.getProperty(VM_PROPERTY);
		if (SERVER_VM.equals(vmType)) {
			defaultConfiguration = null;
			running = true;
			return;
		}
		// end of kludge

		try {
			defaultConfiguration = Configuration.getConfiguration();
		} catch (SecurityException e) {
			// could be caused by missing configuration provider URL;
			// this might be OK if default config provider is ignored
			defaultConfiguration = null;
		}
		Configuration.setConfiguration(new ConfigurationFederator(defaultConfiguration));
		running = true;
	}

	public void stop() {
		if (!running)
			return;
		if (defaultConfiguration != null) {
			Configuration.setConfiguration(defaultConfiguration);
			defaultConfiguration = null;
		}
		running = false;
	}

	public boolean addConfigURL(URL url) {
		if (url == null)
			return false;

		// stop on a first empty URL entry - we will use it to add our new element
		for (int i = 1; i <= MAX_PROVIDER_URL_COUNT; i++) {
			String tag = PROVIDER_URL_BASE + Integer.toString(i);
			String currentURL = Security.getProperty(tag);
			if (currentURL != null && currentURL.length() != 0)
				continue;
			String path;
			try {
				// in case URL is contained in a JARed bundle, this will extract it into a file system
				path = FileLocator.toFileURL(url).toExternalForm();
			} catch (IOException e) {
				path = url.toExternalForm();
			}
			Security.setProperty(tag, path);
			return true;
		}
		return false;
	}
}
