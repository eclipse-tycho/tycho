/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.auth.ext.loader;

import java.util.Map;
import javax.security.auth.spi.LoginModule;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.auth.module.ExtensionLoginModule;
import org.eclipse.osgi.util.NLS;

/**
 * Expected usage pattern: this method is called infrequently (a few times per life cycle;
 * most likely once). As such, no internal caches are maintained and it simply goes
 * to the registry and retrieves information when asked.
 */
public class ExtLoginModuleLoader {

	final private static String POINT_MODULE = AuthPlugin.PI_AUTH + "." + "loginModule"; //$NON-NLS-1$ //$NON-NLS-2$
	final private static String ELEM_MODULE = "loginModule"; //$NON-NLS-1$
	final private static String ATTR_MODULE_CLASS = "class"; //$NON-NLS-1$

	public static LoginModule load(Map<String, ?> options) {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint(POINT_MODULE);
		IExtension[] extensions = point.getExtensions();

		String targetPoint = (String) options.get(ExtensionLoginModule.OPTION_MODULE_POINT);

		LoginModule loginModule = null;
		for (IExtension extension : extensions) {
			String sourcePoint = extension.getUniqueIdentifier();
			if (sourcePoint == null) // technically, IDs on extensions are optional
				continue;
			if (sourcePoint.equals(targetPoint)) {
				IConfigurationElement[] elements = extension.getConfigurationElements();
				int elementCount = elements.length;
				if (elementCount == 1)
					loginModule = readEntry(elements[0]);
				else {
					String message = NLS.bind(SecAuthMessages.invalidLoginmoduleCount, Integer.toString(elementCount));
					AuthPlugin.getDefault().logError(message, null);
				}
			}
		}
		return loginModule;
	}

	private static LoginModule readEntry(IConfigurationElement element) {
		// XXX make this check an utility
		if (!ELEM_MODULE.equals(element.getName())) {
			String supplier = element.getContributor().getName();
			String message = NLS.bind(SecAuthMessages.unexpectedConfigElement, element.getName(), supplier);
			AuthPlugin.getDefault().logError(message, null);
			return null;
		}

		// XXX make creation of executable extension and its error handling an utility
		try {
			LoginModule module = (LoginModule) element.createExecutableExtension(ATTR_MODULE_CLASS);
			return module;
			// future expandability: description is stored in the attribute "description" 
		} catch (CoreException e) {
			String supplier = element.getContributor().getName();
			String message = NLS.bind(SecAuthMessages.instantiationFailed, element.getAttribute(ATTR_MODULE_CLASS), supplier);
			AuthPlugin.getDefault().logError(message, e);
			return null;
		}
	}
}
