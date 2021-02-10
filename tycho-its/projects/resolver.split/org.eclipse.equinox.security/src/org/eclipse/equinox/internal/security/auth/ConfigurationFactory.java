/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.auth;

import java.util.ArrayList;
import javax.security.auth.login.Configuration;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.osgi.util.NLS;

// TODO is this class expected to read extension registry only once?
// consider caching/dynamic registry. This should implement registry listener
// and react to registry events.

// TODO this class creates ConfigurationProvider's from the registry information
// consider moving it into loader package and renaming to reflect this. (This is not
// a factory in the general meaning of the pattern.)
public class ConfigurationFactory {

	final private static String ELEM_PROVIDER = "loginConfigurationProvider";//$NON-NLS-1$
	final private static String ATTR_PROVIDER_CLASS = "class";//$NON-NLS-1$
	final private static String POINT_PROVIDER = "org.eclipse.equinox.security.loginConfigurationProvider"; //$NON-NLS-1$

	private static ConfigurationFactory s_instance = new ConfigurationFactory();

	static ConfigurationFactory getInstance() {
		return s_instance;
	}

	public Configuration[] getConfigurations() {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint(POINT_PROVIDER);
		IExtension[] extensions = point.getExtensions();

		ArrayList<Configuration> returnValue = new ArrayList<>(extensions.length);
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				Configuration provider = readProvider(element);
				if (provider != null)
					returnValue.add(provider);
			}
		}
		return returnValue.toArray(new Configuration[] {});
	}

	private Configuration readProvider(IConfigurationElement element) {
		if (!ELEM_PROVIDER.equals(element.getName())) {
			reportError(SecAuthMessages.unexpectedConfigElement, element.getName(), element, null);
			return null;
		}
		try {
			return (Configuration) element.createExecutableExtension(ATTR_PROVIDER_CLASS);
		} catch (CoreException e) {
			reportError(SecAuthMessages.instantiationFailed, element.getAttribute(ATTR_PROVIDER_CLASS), element, e);
			return null;
		}
	}

	private void reportError(String template, String arg, IConfigurationElement element, Throwable e) {
		String supplier = element.getContributor().getName();
		String message = NLS.bind(template, arg, supplier);
		AuthPlugin.getDefault().logError(message, e);
	}
}
