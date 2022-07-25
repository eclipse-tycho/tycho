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
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.connect.FrameworkUtilHelper;

/**
 * Implementation of the {@link FrameworkUtilHelper} to connect classes to
 * bundles
 */
public class PlexusFrameworkUtilHelper implements FrameworkUtilHelper {

	private static Set<FrameworkUtilHelper> helpers = ConcurrentHashMap.newKeySet();

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		ClassLoader loader = classFromBundle.getClassLoader();
		if (loader != null) {
			for (FrameworkUtilHelper helper : helpers) {
				Optional<Bundle> bundle = helper.getBundle(classFromBundle);
				if (bundle.isPresent()) {
					return bundle;
				}
			}
		}
		return Optional.empty();
	}

	public static void registerHelper(FrameworkUtilHelper helper) {
		for (FrameworkUtilHelper spi : ServiceLoader.load(FrameworkUtilHelper.class,
				FrameworkUtilHelper.class.getClassLoader())) {
			Class<? extends FrameworkUtilHelper> spiHelperClass = spi.getClass();
			Class<PlexusFrameworkUtilHelper> thisClass = PlexusFrameworkUtilHelper.class;
			if (spiHelperClass.getName().equals(thisClass.getName())) {
				if (spiHelperClass == thisClass) {
					// register our instance here...
					helpers.add(helper);
				} else {
					invokeForeignMethod(spiHelperClass, "registerHelper", helper);
				}
				break;
			}
		}
	}

	public static void unregisterHelper(FrameworkUtilHelper helper) {
		for (FrameworkUtilHelper spi : ServiceLoader.load(FrameworkUtilHelper.class,
				FrameworkUtilHelper.class.getClassLoader())) {
			Class<? extends FrameworkUtilHelper> spiHelperClass = spi.getClass();
			Class<PlexusFrameworkUtilHelper> thisClass = PlexusFrameworkUtilHelper.class;
			if (spiHelperClass.getName().equals(thisClass.getName())) {
				if (spiHelperClass == thisClass) {
					// register our instance here...
					helpers.add(helper);
				} else {
					invokeForeignMethod(spiHelperClass, "unregisterHelper", helper);
				}
				break;
			}
		}
	}

	private static void invokeForeignMethod(Class<?> clazz, String methodName, Object parameter) {
		try {
			Method method = clazz.getMethod(methodName, FrameworkUtilHelper.class);
			method.invoke(null, parameter);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
		}
	}

}
