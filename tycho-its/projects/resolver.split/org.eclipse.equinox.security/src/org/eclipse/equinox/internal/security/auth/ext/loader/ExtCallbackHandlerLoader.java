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
package org.eclipse.equinox.internal.security.auth.ext.loader;

import javax.security.auth.callback.CallbackHandler;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.osgi.util.NLS;

// XXX rename: CallbackHandlerLoader
public class ExtCallbackHandlerLoader {

	final private static String POINT_HANDLER = AuthPlugin.PI_AUTH + "." + "callbackHandler"; //$NON-NLS-1$ //$NON-NLS-2$
	final private static String ELEM_HANDLER = "callbackHandler"; //$NON-NLS-1$
	final private static String ATTR_HANDLER_CLASS = "class"; //$NON-NLS-1$

	final private static String POINT_MAPPING = AuthPlugin.PI_AUTH + "." + "callbackHandlerMapping"; //$NON-NLS-1$ //$NON-NLS-2$
	final private static String ELEM_MAPPING = "callbackHandlerMapping"; //$NON-NLS-1$
	final private static String ATTR_MAPPING_CONFIGNAME = "configName";//$NON-NLS-1$
	final private static String ATTR_MAPPING_CALLBACKID = "callbackHandlerId"; //$NON-NLS-1$

	public CallbackHandler loadCallbackHandler(String configName) {
		IExtensionRegistry registry = RegistryFactory.getRegistry();

		// First, map config name -> callback handler ID
		IExtensionPoint point = registry.getExtensionPoint(POINT_MAPPING);
		IExtension[] extenders = point.getExtensions();
		String extensionId = null;
		for (IExtension extender : extenders) {
			IConfigurationElement[] confEelements = extender.getConfigurationElements();
			if (confEelements.length != 1)
				continue; // TBD error message?
			extensionId = loadMappingEntry(confEelements[0], configName);
			if (extensionId != null)
				break;
		}
		if (extensionId == null)
			return null;

		// Next, load class specified by the callback handler ID
		IExtensionPoint pointCallbackHandler = registry.getExtensionPoint(POINT_HANDLER);
		IExtension extension = pointCallbackHandler.getExtension(extensionId);
		if (extension == null)
			return null;
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length != 1)
			return null; // TBD error message?
		return loadHandlerClass(elements[0]);
	}

	private String loadMappingEntry(IConfigurationElement element, String configName) {
		if (!expectedElement(element, ELEM_MAPPING))
			return null;
		if (configName.equals(element.getAttribute(ATTR_MAPPING_CONFIGNAME)))
			return element.getAttribute(ATTR_MAPPING_CALLBACKID);
		return null;
	}

	private CallbackHandler loadHandlerClass(IConfigurationElement element) {
		if (!expectedElement(element, ELEM_HANDLER))
			return null;
		try {
			return (CallbackHandler) element.createExecutableExtension(ATTR_HANDLER_CLASS);
		} catch (CoreException e) {
			String message = NLS.bind(SecAuthMessages.instantiationFailed1, element.getAttribute(ATTR_HANDLER_CLASS));
			AuthPlugin.getDefault().logError(message, e);
			return null;
		}
	}

	private boolean expectedElement(IConfigurationElement element, String expectedName) {
		if (expectedName.equals(element.getName()))
			return true;
		String supplier = element.getContributor().getName();
		String message = NLS.bind(SecAuthMessages.unexpectedConfigElement, element.getName(), supplier);
		AuthPlugin.getDefault().logError(message, null);
		return false;
	}
}
