/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.storage;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.internal.security.storage.friends.IStorageConstants;
import org.eclipse.equinox.internal.security.storage.provider.IValidatingPasswordProvider;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

//XXX add validation on module IDs - AZaz09 and dots, absolutely no tabs 
// XXX reserved name DEFAULT_PASSWORD_ID

/**
 * Finds appropriate password provider module to use.
 */
public class PasswordProviderSelector implements IRegistryEventListener {

	final private static String EXTENSION_POINT = "org.eclipse.equinox.security.secureStorage"; //$NON-NLS-1$
	final private static String STORAGE_MODULE = "provider";//$NON-NLS-1$
	final private static String MODULE_PRIORITY = "priority";//$NON-NLS-1$
	final private static String MODULE_DESCRIPTION = "description";//$NON-NLS-1$
	final private static String CLASS_NAME = "class";//$NON-NLS-1$
	final private static String HINTS_NAME = "hint";//$NON-NLS-1$
	final private static String HINT_VALUE = "value";//$NON-NLS-1$

	private Map<String, PasswordProviderModuleExt> modules = new HashMap<>(5); // cache of modules found

	public class ExtStorageModule {
		public String moduleID;
		public IConfigurationElement element;
		public int priority;
		public String name;
		public String description;
		public List<String> hints;

		public ExtStorageModule(String id, IConfigurationElement element, int priority, String name, String description, List<String> hints) {
			super();
			this.element = element;
			this.moduleID = id;
			this.priority = priority;
			this.name = name;
			this.description = description;
			this.hints = hints;
		}
	}

	static private PasswordProviderSelector instance = null;

	static public PasswordProviderSelector getInstance() {
		if (instance == null) {
			instance = new PasswordProviderSelector();
			IExtensionRegistry registry = RegistryFactory.getRegistry();
			registry.addListener(instance, EXTENSION_POINT);
		}
		return instance;
	}

	static public void stop() {
		if (instance != null) {
			IExtensionRegistry registry = RegistryFactory.getRegistry();
			registry.removeListener(instance);
			instance = null;
		}
	}

	private PasswordProviderSelector() {
		// hides default constructor; use getInstance()
	}

	public List<ExtStorageModule> findAvailableModules(String expectedID) {

		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint(EXTENSION_POINT);
		IExtension[] extensions = point.getExtensions();

		ArrayList<ExtStorageModule> allAvailableModules = new ArrayList<>(extensions.length);

		for (IExtension extension : extensions) {
			String moduleID = extension.getUniqueIdentifier();
			if (moduleID == null) // IDs on those extensions are mandatory; if not specified, ignore the extension
				continue;
			moduleID = moduleID.toLowerCase();
			if (expectedID != null && !expectedID.equals(moduleID))
				continue;
			IConfigurationElement[] elements = extension.getConfigurationElements();
			if (elements.length == 0)
				continue;
			IConfigurationElement element = elements[0]; // only one module is allowed per extension
			if (!STORAGE_MODULE.equals(element.getName())) {
				reportError(SecAuthMessages.unexpectedConfigElement, element.getName(), element, null);
				continue;
			}
			String attribute = element.getAttribute(MODULE_PRIORITY);
			int priority = -1;
			if (attribute != null) {
				priority = Integer.parseInt(attribute);
				if (priority < 0)
					priority = 0;
				if (priority > 10)
					priority = 10;
			}
			String name = extension.getLabel();
			String description = element.getAttribute(MODULE_DESCRIPTION);
			List<String> suppliedHints = null;
			IConfigurationElement[] hints = element.getChildren(HINTS_NAME);
			if (hints.length != 0) {
				suppliedHints = new ArrayList<>(hints.length);
				for (IConfigurationElement h : hints) {
					String hint = h.getAttribute(HINT_VALUE);
					if (hint != null)
						suppliedHints.add(hint);
				}
			}
			Object clazz;
			try {
				clazz = element.createExecutableExtension(CLASS_NAME);
				// Bug 537833 - on some systems, the password provider does not work (e.g. Linux with KDE desktop) so these
				// providers will request validation
				if (clazz instanceof IValidatingPasswordProvider && !((IValidatingPasswordProvider) clazz).isValid())
					continue;
			} catch (CoreException e) {
				continue;
			}
			allAvailableModules.add(new ExtStorageModule(moduleID, element, priority, name, description, suppliedHints));
		}

		Collections.sort(allAvailableModules, (o1, o2) -> {
			int p1 = o1.priority;
			int p2 = o2.priority;
			return p2 - p1;
		});

		return allAvailableModules;
	}

	public PasswordProviderModuleExt findStorageModule(String expectedID) throws StorageException {
		if (expectedID != null)
			expectedID = expectedID.toLowerCase(); // ID is case-insensitive
		synchronized (modules) {
			if (modules.containsKey(expectedID))
				return modules.get(expectedID);
		}

		List<ExtStorageModule> allAvailableModules = findAvailableModules(expectedID);
		HashSet<String> disabledModules = getDisabledModules();

		for (ExtStorageModule module : allAvailableModules) {

			if (expectedID == null && disabledModules != null && disabledModules.contains(module.moduleID))
				continue;

			Object clazz;
			try {
				clazz = module.element.createExecutableExtension(CLASS_NAME);
			} catch (CoreException e) {
				reportError(SecAuthMessages.instantiationFailed, module.element.getAttribute(CLASS_NAME), module.element, e);
				continue;
			}
			if (!(clazz instanceof PasswordProvider))
				continue;

			PasswordProviderModuleExt result = new PasswordProviderModuleExt((PasswordProvider) clazz, module.moduleID);

			// cache the result
			synchronized (modules) {
				if (expectedID == null)
					modules.put(null, result);
				modules.put(module.moduleID, result);
			}

			return result;
		}

		// the secure storage module was not found - error in app's configuration
		String msg;
		if (expectedID == null)
			msg = SecAuthMessages.noSecureStorageModules;
		else
			msg = NLS.bind(SecAuthMessages.noSecureStorageModule, expectedID);
		throw new StorageException(StorageException.NO_SECURE_MODULE, msg);
	}

	private void reportError(String template, String arg, IConfigurationElement element, Throwable e) {
		String supplier = element.getContributor().getName();
		String message = NLS.bind(template, arg, supplier);
		AuthPlugin.getDefault().logError(message, e);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// Synch local cache with the registry 
	@Override
	public void added(IExtension[] extensions) {
		clearCaches();
	}

	@Override
	public void added(IExtensionPoint[] extensionPoints) {
		clearCaches();
	}

	@Override
	public void removed(IExtension[] extensions) {
		clearCaches();
	}

	@Override
	public void removed(IExtensionPoint[] extensionPoints) {
		clearCaches();
	}

	/**
	 * Clear whole cache as priorities might have changed after new modules were added.
	 */
	public void clearCaches() {
		synchronized (modules) {
			modules.clear();
			// If module was removed, clear its entry from the password cache.
			// The code below clears all entries for simplicity, in future this
			// can be made more limiting if a scenario exists where module
			// removal/addition is a frequent event.
			SecurePreferencesMapper.clearPasswordCache();
		}
	}

	public boolean isLoggedIn() {
		synchronized (modules) {
			return (modules.size() != 0);
		}
	}

	protected HashSet<String> getDisabledModules() {
		IScopeContext[] scopes = {ConfigurationScope.INSTANCE, DefaultScope.INSTANCE};
		String defaultPreferenceValue = ""; //$NON-NLS-1$
		IPreferencesService preferencesService = getPreferencesService();
		String tmp = preferencesService.getString(AuthPlugin.PI_AUTH, IStorageConstants.DISABLED_PROVIDERS_KEY, defaultPreferenceValue, scopes);
		if (tmp == null || tmp.length() == 0)
			return null;
		HashSet<String> disabledModules = new HashSet<>();
		String[] disabledProviders = tmp.split(","); //$NON-NLS-1$
		for (String disabledProvider : disabledProviders) {
			disabledModules.add(disabledProvider);
		}
		return disabledModules;
	}

	private IPreferencesService getPreferencesService() {
		BundleContext context = AuthPlugin.getDefault().getBundleContext();
		ServiceReference<IPreferencesService> reference = context.getServiceReference(IPreferencesService.class);
		if (reference == null) {
			throw new IllegalStateException("Failed to find service: " + IPreferencesService.class); //$NON-NLS-1$
		}
		try {
			return context.getService(reference);
		} finally {
			context.ungetService(reference);
		}
	}
}
