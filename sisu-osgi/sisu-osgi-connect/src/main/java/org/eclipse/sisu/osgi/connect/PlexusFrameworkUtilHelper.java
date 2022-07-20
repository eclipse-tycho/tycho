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

import java.util.Optional;

import org.osgi.framework.Bundle;
import org.osgi.framework.connect.FrameworkUtilHelper;
import org.osgi.framework.launch.Framework;

/**
 * Implementation of the {@link FrameworkUtilHelper} to connect classes to
 * bundles
 */
public class PlexusFrameworkUtilHelper implements FrameworkUtilHelper {

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		PlexusFrameworkConnectServiceFactory factory = PlexusFrameworkConnectServiceFactory.instance;
		if (factory != null) {
			Framework framework = factory.frameworkMap.get(classFromBundle.getClassLoader());
			if (framework != null) {
				String location = classFromBundle.getProtectionDomain().getCodeSource().getLocation().toString();
				for (Bundle bundle : framework.getBundleContext().getBundles()) {
					String bundleLocation = bundle.getLocation();
					if (locationsMatch(location, bundleLocation)) {
						return Optional.of(bundle);
					}
				}
			}
		}
		return Optional.empty();
	}

	private static boolean locationsMatch(String classLocation, String bundleLocation) {
		return classLocation.endsWith(bundleLocation);
	}

}
