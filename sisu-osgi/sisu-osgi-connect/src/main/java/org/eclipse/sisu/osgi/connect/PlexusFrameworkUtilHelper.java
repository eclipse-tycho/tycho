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

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Optional;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.connect.FrameworkUtilHelper;

/**
 * Implementation of the {@link FrameworkUtilHelper} to connect classes to
 * bundles
 */
public class PlexusFrameworkUtilHelper implements FrameworkUtilHelper {

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		ClassLoader loader = classFromBundle.getClassLoader();
		if (loader instanceof ClassRealm) {
			@SuppressWarnings("resource")
			ClassRealm realm = (ClassRealm) loader;
			Class<?> thisHelper = PlexusFrameworkUtilHelper.class;
			Class<?> realmHelper = realm.loadClassFromSelf(thisHelper.getName());
			if (realmHelper != null && thisHelper != realmHelper) {
				try {
					Object object = realmHelper.getConstructor().newInstance();
					if (object instanceof FrameworkUtilHelper) {
						FrameworkUtilHelper helper = (FrameworkUtilHelper) object;
						Optional<Bundle> bundle = helper.getBundle(classFromBundle);
						return bundle;
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				}
			}
		}
		String location = getLocationFromClass(classFromBundle);
		if (location != null) {
			ClassLoader classLoader = classFromBundle.getClassLoader();
			PlexusConnectFramework connect = PlexusFrameworkConnectServiceFactory.frameworkMap.get(classLoader);
			if (connect != null) {
				connect.debug(" Use framework" + connect.getName());
				BundleContext bundleContext = connect.getFramework().getBundleContext();
				Bundle[] bundles = bundleContext.getBundles();
				for (Bundle bundle : bundles) {
					String bundleLocation = bundle.getLocation();
					if (locationsMatch(location, bundleLocation)) {
						connect.debug("Return bundle " + bundle.getSymbolicName() + " for location " + location);
						return Optional.of(bundle);
					}
				}
				if (classLoader == BundleContext.class.getClassLoader()) {
					return Optional.of(bundleContext.getBundle(0));
				}
				connect.debug("No bundle matched " + location);
			}
		}
		return Optional.empty();
	}

	static String getLocationFromClass(Class<?> classFromBundle) {
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

	static boolean locationsMatch(String classLocation, String bundleLocation) {
		return classLocation.endsWith(bundleLocation);
	}

}
