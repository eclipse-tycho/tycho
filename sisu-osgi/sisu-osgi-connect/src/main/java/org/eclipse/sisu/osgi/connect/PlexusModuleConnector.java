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
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.codehaus.plexus.logging.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;

/**
 * The PlexusModuleConnector scans a linear classpath for bundles and install
 * them as {@link ConnectContent} into the given {@link BundleContext}
 */
final class PlexusModuleConnector implements ModuleConnector {

	private static final String JAR_FILE_PREFIX = "jar:file:";

	private ClassLoader classloader;

	private Map<String, PlexusConnectContent> modulesMap = new HashMap<>();

	private File storage;

	public PlexusModuleConnector(ClassLoader classloader) {
		this.classloader = classloader;
	}

	public void installBundles(BundleContext bundleContext, Predicate<String> filter, Logger logger) {
		Enumeration<URL> resources;
		try {
			resources = classloader.getResources(JarFile.MANIFEST_NAME);
		} catch (IOException e) {
			logger.error("Can't load resources for classloader " + classloader);
			return;
		}
		Map<String, String> installed = new HashMap<String, String>();
		while (resources.hasMoreElements()) {
			String location = resources.nextElement().toExternalForm();
			logger.debug("Scan " + location + " for bundle data...");
			if (location.startsWith(JAR_FILE_PREFIX)) {
				String name = location.substring(JAR_FILE_PREFIX.length()).split("!")[0];
				try {
					JarFile jarFile = new JarFile(name);
					try {
						Manifest manifest = jarFile.getManifest();
						Attributes mainAttributes = manifest.getMainAttributes();
						if (mainAttributes == null) {
							jarFile.close();
							continue;
						}
						String bundleSymbolicName = getBsn(mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
						if (bundleSymbolicName == null || !filter.test(bundleSymbolicName)) {
							if (bundleSymbolicName != null) {
								logger.debug("Ignore bundle " + bundleSymbolicName
										+ " as it is not included in the bundle list.");
							}
							jarFile.close();
							continue;
						}
						String existingLocation = installed.putIfAbsent(bundleSymbolicName, name);
						if (existingLocation != null) {
							logger.warn("Skip duplicate bundle " + bundleSymbolicName + "\r\n\texisting location: "
									+ existingLocation + "\r\n\tskipped location: " + name);
							jarFile.close();
							continue;
						}
						logger.debug("Discovered bundle " + bundleSymbolicName + " @ " + name);
						modulesMap.put(name, new PlexusConnectContent(jarFile, classloader));
						try {
							bundleContext.installBundle(name);
						} catch (BundleException e) {
							logger.warn("Can't install bundle at " + name, e);
							jarFile.close();
							modulesMap.remove(name);
						}
					} catch (IOException e) {
						jarFile.close();
					}
				} catch (IOException e) {
					logger.warn("Can't open jar at " + name, e);
				}
			}
		}
	}

	private String getBsn(String value) {
		if (value != null) {
			return value.split(";")[0].trim();
		}
		return null;
	}

	@Override
	public Optional<ConnectModule> connect(String location) throws BundleException {
		return Optional.ofNullable(modulesMap.get(location));
	}

	@Override
	public void initialize(File storage, Map<String, String> configuration) {
		this.storage = storage;
	}

	@Override
	public Optional<BundleActivator> newBundleActivator() {
		return Optional.empty();
	}

	public File getStorage() {
		return storage;
	}
}