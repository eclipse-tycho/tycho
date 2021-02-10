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
package org.eclipse.equinox.internal.security.auth;

import java.util.Hashtable;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.osgi.util.NLS;

public class ConfigurationFederator extends Configuration {

	// TODO this usage needs to be changed. We should retrieve federatedConfigs
	// from the "ConfigurationFactory" every time we are asked; the "ConfigurationFactory"
	// shoudl keep a cache that corresponds to what's in the registry and update it on registry 
	// events
	private Configuration[] federatedConfigs = null;

	private Hashtable<String, AppConfigurationEntry[]> configCache = new Hashtable<>(5);
	private Hashtable<String, String> configToProviderMap = new Hashtable<>(5);

	final private Configuration defaultConfiguration;

	public ConfigurationFederator(Configuration defaultConfiguration) {
		this.defaultConfiguration = defaultConfiguration;
	}

	@Override
	public synchronized AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		AppConfigurationEntry[] returnValue = configCache.get(name);
		if (returnValue != null)
			return returnValue;

		// Note: adding default config provider last; extension-point based configs are queried first
		Configuration[] configs = getFederatedConfigs();
		Configuration[] allConfigs = configs;
		if (defaultConfiguration != null) {
			allConfigs = new Configuration[configs.length + 1];
			System.arraycopy(configs, 0, allConfigs, 0, configs.length);
			allConfigs[configs.length] = defaultConfiguration;
		}
		for (Configuration c : allConfigs) {
			boolean found = false;
			AppConfigurationEntry[] config = c.getAppConfigurationEntry(name);
			if (config == null)
				continue;
			String cachedProviderName = configToProviderMap.get(name);
			if (cachedProviderName != null && !cachedProviderName.equals(c.getClass().getName())) {
				String message = NLS.bind(SecAuthMessages.duplicateJaasConfig1, name, cachedProviderName);
				AuthPlugin.getDefault().logError(message, null);
			} else {
				if (found) {
					String message = NLS.bind(SecAuthMessages.duplicateJaasConfig2, name, cachedProviderName);
					AuthPlugin.getDefault().logError(message, null);
				} else if ((config != null) && (config.length != 0)) {
					returnValue = config;
					configToProviderMap.put(name, c.getClass().getName());
					configCache.put(name, returnValue);
					found = true;
				}
			}
		}

		if (returnValue == null || returnValue.length == 0) {
			String message = NLS.bind(SecAuthMessages.nonExistantJaasConfig, name);
			AuthPlugin.getDefault().logError(message, null);
		}
		return returnValue;
	}

	@Override
	public synchronized void refresh() {
		for (Configuration federatedConfig : federatedConfigs) {
			federatedConfig.refresh();
		}
		if (defaultConfiguration != null)
			defaultConfiguration.refresh();

		configCache.clear();
		configToProviderMap.clear();
	}

	private Configuration[] getFederatedConfigs() {
		if (federatedConfigs == null)
			federatedConfigs = ConfigurationFactory.getInstance().getConfigurations();
		return federatedConfigs;
	}
}
