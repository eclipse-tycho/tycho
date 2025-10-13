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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The classloader wraps the OSGi provided one but gives access for the JUnit
 * runer to any SPI declared services.
 */
class SPIBundleClassLoader extends ClassLoader {

	private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	private static final String META_INF_SERVICES = "META-INF/services/";
	private Bundle bundle;
	private Consumer<String> logger;
	private List<Bundle> bundles;
	private Map<String, List<SPIMapping>> mappings = new ConcurrentHashMap<>();

	public SPIBundleClassLoader(Bundle bundle, List<Bundle> bundles, Consumer<String> logger) {
		this.bundle = bundle;
		this.bundles = bundles;
		this.logger = logger;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			return bundle.loadClass(name);
		} catch (ClassNotFoundException e) {
			Bundle caller = WALKER.walk(stream -> stream.map(sf -> FrameworkUtil.getBundle(sf.getDeclaringClass()))
					.filter(Objects::nonNull).findFirst().orElse(null));
			if (isValidCaller(caller)) {
				Optional<SPIMapping> spi = mappings.values().stream().flatMap(Collection::stream)
						.filter(mapping -> mapping.hasService(name)).filter(mapping -> mapping.isCompatible(caller))
						.findFirst();
				if (spi.isPresent()) {
					logger.accept("Loading SPI implementation " + name + " from mapping");
					return spi.get().loadImplementation(name);
				}
			}
			logger.accept("Can't load class " + name);
			throw e;
		}
	}

	@Override
	protected URL findResource(String name) {
		URL resource = bundle.getResource(name);
		if (resource != null) {
			return resource;
		}
		logger.accept("Can't find resource: " + name);
		return null;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		List<URL> result = new ArrayList<>();
		Enumeration<URL> resources = bundle.getResources(name);
		if (resources != null) {
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				result.add(url);
			}
		}
		if (name.startsWith(META_INF_SERVICES)) {
			Bundle caller = WALKER.walk(stream -> stream.map(sf -> FrameworkUtil.getBundle(sf.getDeclaringClass()))
					.filter(Objects::nonNull).findFirst().orElse(null));
			if (isValidCaller(caller)) {
				List<SPIMapping> spis = mappings.computeIfAbsent(name, spi -> {
					String serviceName = name.substring(META_INF_SERVICES.length());
					logger.accept("searching for SPI services " + serviceName + " ...");
					List<SPIMapping> list = new ArrayList<>();
					for (Bundle other : bundles) {
						if (other == bundle) {
							continue;
						}
						URL entry = other.getEntry(name);
						if (entry != null) {
							try {
								list.add(new SPIMapping(other.loadClass(serviceName), other, entry));
							} catch (ClassNotFoundException e) {
								// should not happen
							}
						}
					}
					return list;
				});
				for (SPIMapping mapping : spis) {
					if (mapping.isCompatible(caller)) {
						result.add(mapping.getUrl());
					}
				}
			}
		}
		return Collections.enumeration(result);
	}

	private boolean isValidCaller(Bundle caller) {
		return caller != null && caller.getSymbolicName().startsWith("junit-platform-");
	}

	@Override
	public String toString() {
		return "SPIBundleClassLoader for bundle " + bundle;
	}

}
