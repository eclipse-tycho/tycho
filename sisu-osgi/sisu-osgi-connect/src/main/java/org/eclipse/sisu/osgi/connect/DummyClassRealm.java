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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.logging.Logger;

final class DummyClassRealm extends ClassRealm {

	private static final String JAR_PREFIX = "jar:";
	private static final String JAR_FILE_PREFIX = JAR_PREFIX + "file:";
	private ClassLoader classLoader;
	private List<URL> urls;
	private Logger logger;

	DummyClassRealm(String id, ClassLoader classLoader, Logger logger) {
		super(new ClassWorld(), id, classLoader);
		this.classLoader = classLoader;
		this.logger = logger;
	}

	@Override
	public URL[] getURLs() {
		if (urls == null) {
			InputStream stream = classLoader.getResourceAsStream("META-INF/sisu/realm.filter");
			List<String> filters;
			if (stream != null) {
				try {
					filters = IOUtils.readLines(stream, StandardCharsets.UTF_8);
					stream.close();
				} catch (IOException e) {
					filters = null;
				}
			} else {
				filters = null;
			}
			urls = new ArrayList<>();
			try {
				Enumeration<URL> resources = classLoader.getResources(JarFile.MANIFEST_NAME);
				while (resources.hasMoreElements()) {
					String location = resources.nextElement().toExternalForm();
					if (isValidJar(location, filters)) {
						String name = location.substring(JAR_PREFIX.length()).split("!")[0];
						logger.debug("Adding URL " + name + " to DummyRealm...");
						urls.add(new URL(name));
					} else {
						logger.debug("Location " + location + " was filtered from DummyRealm...");
					}
				}
			} catch (IOException e) {
			}
		}
		return urls.toArray(URL[]::new);
	}

	private boolean isValidJar(String location, List<String> filters) {
		if (location.startsWith(JAR_FILE_PREFIX)) {
			if (filters != null) {
				String normalizedpath = location.replace('\\', '/').toLowerCase();
				for (String filter : filters) {
					if (filter.isBlank() || filter.startsWith("#")) {
						continue;
					}
					if (normalizedpath.contains(filter)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public Class<?> loadClassFromSelf(String name) {
		try {
			return classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	@Override
	public URL loadResourceFromSelf(String name) {
		return classLoader.getResource(name);
	}

	@Override
	public Enumeration<URL> loadResourcesFromSelf(String name) {
		try {
			return classLoader.getResources(name);
		} catch (IOException e) {
			return null;
		}
	}
}