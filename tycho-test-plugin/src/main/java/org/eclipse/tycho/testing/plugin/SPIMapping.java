/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    
 */
package org.eclipse.tycho.testing.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;

final class SPIMapping {

	private final Class<?> serviceClass;
	private final Bundle bundle;
	private final Set<String> classes;
	private final URL url;

	SPIMapping(Class<?> serviceClass, Bundle bundle, URL url) {
		this.serviceClass = serviceClass;
		this.bundle = bundle;
		this.url = url;
		this.classes = readClasses(url);
	}

	private Set<String> readClasses(URL entry) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(entry.openStream()))) {
			return reader.lines().collect(Collectors.toSet());
		} catch (IOException e) {
			return Set.of();
		}
	}

	boolean isCompatible(Bundle other) {
		try {
			return other.loadClass(serviceClass.getName()) == serviceClass;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	URL getUrl() {
		return url;
	}

	boolean hasService(String implementation) {
		return classes != null && classes.contains(implementation);
	}

	Class<?> loadImplementation(String name) throws ClassNotFoundException {
		return bundle.loadClass(name);
	}

}