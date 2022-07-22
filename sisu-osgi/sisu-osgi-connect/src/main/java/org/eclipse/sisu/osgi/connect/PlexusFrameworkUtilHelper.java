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

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.connect.FrameworkUtilHelper;

/**
 * Implementation of the {@link FrameworkUtilHelper} to connect classes to
 * bundles
 */
public class PlexusFrameworkUtilHelper implements FrameworkUtilHelper {

	static Set<PlexusFrameworkConnectServiceFactory> factories = ConcurrentHashMap.newKeySet();

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		String location = getLocationFromClass(classFromBundle);
		if (location != null) {
			for (PlexusFrameworkConnectServiceFactory factory : factories) {
				PlexusConnectFramework connect = factory.frameworkMap.get(classFromBundle.getClassLoader());
				if (connect != null) {
					connect.debug(" Use framework" + connect.getName());
					for (Bundle bundle : connect.getFramework().getBundleContext().getBundles()) {
						String bundleLocation = bundle.getLocation();
						if (locationsMatch(location, bundleLocation)) {
							connect.info("Return bundle " + bundle.getSymbolicName() + " for location " + location);
							return Optional.of(bundle);
						}
					}
					connect.info("No bundle matched " + location);
				}
			}
		}
		return Optional.empty();
	}

	private String getLocationFromClass(Class<?> classFromBundle) {
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
		return url.toString();
	}

	private static boolean locationsMatch(String classLocation, String bundleLocation) {
		return classLocation.endsWith(bundleLocation);
	}

}
