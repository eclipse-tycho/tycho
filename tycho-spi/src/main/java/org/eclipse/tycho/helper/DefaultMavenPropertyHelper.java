/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.helper;

import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;

@Named
public class DefaultMavenPropertyHelper implements MavenPropertyHelper {

    @Inject
    LegacySupport legacySupport;

    /**
     * Returns a global (user) property of the given key, the search order is
     * <ol>
     * <li>Maven session user properties</li>
     * <li>Active profile(s) property</li>
     * <li>Maven session system properties</li>
     * <li>Java System Properties</li>
     * </ol>
     * 
     * @param key
     *            the key to search
     * @return the value according to the described search order or <code>null</code> if nothing can
     *         be found.
     */
    public String getGlobalProperty(String key) {
        return getGlobalProperty(key, null);
    }

    /**
     * Returns a global (user) property of the given key, the search order is
     * <ol>
     * <li>Maven session user properties</li>
     * <li>Active profile(s) property</li>
     * <li>Maven session system properties</li>
     * <li>Java System Properties</li>
     * <li>default value</li>
     * </ol>
     * 
     * @param key
     *            the key to search
     * @param defaultValue
     *            the default value to use as a last resort
     * @return the value according to the described search order
     */
    public String getGlobalProperty(String key, String defaultValue) {
        MavenSession mavenSession = legacySupport.getSession();
        if (mavenSession != null) {
            // Check user properties first ...
            Properties userProperties = mavenSession.getUserProperties();
            String userProperty = userProperties.getProperty(key);
            if (userProperty != null) {
                return userProperty;
            }
            // check if there are any active profile properties ...
            Settings settings = mavenSession.getSettings();
            List<Profile> profiles = settings.getProfiles();
            List<String> activeProfiles = settings.getActiveProfiles();
            for (Profile profile : profiles) {
                if (activeProfiles.contains(profile.getId())) {
                    String profileProperty = profile.getProperties().getProperty(key);
                    if (profileProperty != null) {
                        return profileProperty;
                    }
                }
            }
            // now maven system properties
            Properties systemProperties = mavenSession.getSystemProperties();
            String systemProperty = systemProperties.getProperty(key);
            if (systemProperty != null) {
                return systemProperty;
            }
        }
        // java system properties last
        return System.getProperty(key, defaultValue);
    }

    public boolean getGlobalBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getGlobalProperty(key, Boolean.toString(defaultValue)));
    }

    public int getGlobalIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(getGlobalProperty(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
