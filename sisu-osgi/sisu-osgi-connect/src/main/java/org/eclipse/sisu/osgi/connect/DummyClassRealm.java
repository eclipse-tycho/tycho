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
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

final class DummyClassRealm extends ClassRealm {

	private static final String JAR_PREFIX = "jar:";
	private static final String JAR_FILE_PREFIX = JAR_PREFIX + "file:";
	private ClassLoader classLoader;
	private List<URL> urls;

	DummyClassRealm(String id, ClassLoader classLoader) {
		super(new ClassWorld(), id, classLoader);
		this.classLoader = classLoader;
	}

	@Override
	public URL[] getURLs() {
		if (urls == null) {
			urls = new ArrayList<>();
			try {
				Enumeration<URL> resources = classLoader.getResources(JarFile.MANIFEST_NAME);
				while (resources.hasMoreElements()) {
					String location = resources.nextElement().toExternalForm();
					if (location.startsWith(JAR_FILE_PREFIX)) {
						String name = location.substring(JAR_PREFIX.length()).split("!")[0];
						urls.add(new URL(name));
					}
				}
			} catch (IOException e) {
			}
		}
		return urls.toArray(URL[]::new);
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