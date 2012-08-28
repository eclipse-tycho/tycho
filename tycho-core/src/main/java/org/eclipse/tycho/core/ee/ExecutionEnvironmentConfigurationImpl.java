/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;

public class ExecutionEnvironmentConfigurationImpl implements ExecutionEnvironmentConfiguration {
    private static final int PRIMARY = 0;
    private static final int SECONDARY = 1;

    /** Configurations, ordered by precedence */
    private ProfileConfiguration[] configurations = new ProfileConfiguration[2];

    public void overrideProfileConfiguration(String profileName, String configurationOrigin) {
        if (profileName == null) {
            throw new NullPointerException();
        }

        this.configurations[PRIMARY] = new ProfileConfiguration(profileName, configurationOrigin);
    }

    public void setProfileConfiguration(String profileName, String configurationOrigin) {
        if (profileName == null) {
            throw new NullPointerException();
        }

        this.configurations[SECONDARY] = new ProfileConfiguration(profileName, configurationOrigin);
    }

    private ProfileConfiguration getEffectiveConfiguration() {
        for (ProfileConfiguration entry : configurations) {
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    public String getProfileName() {
        ProfileConfiguration configuration = getEffectiveConfiguration();
        if (configuration == null) {
            // TODO 387796 return explicit global default instead of null
            return null;
        }
        return configuration.profileName;
    }

    public ExecutionEnvironment getFullSpecification() {
        ProfileConfiguration configuration = getEffectiveConfiguration();
        if (configuration == null) {
            // TODO 387796 return explicit global default instead of null
            return null;
        }
        try {
            return ExecutionEnvironmentUtils.getExecutionEnvironment(configuration.profileName);
        } catch (UnknownEnvironmentException e) {
            throw new IllegalArgumentException("Invalid execution environment \"" + configuration.profileName
                    + "\" specified in " + configuration.origin, e);
        }
    }

    private static class ProfileConfiguration {

        final String profileName;
        final String origin;

        ProfileConfiguration(String profileName, String origin) {
            this.profileName = profileName;
            this.origin = origin;
        }

    }
}
