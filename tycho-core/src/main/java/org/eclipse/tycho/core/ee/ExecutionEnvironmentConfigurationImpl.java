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

import java.util.List;

import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;

public class ExecutionEnvironmentConfigurationImpl implements ExecutionEnvironmentConfiguration {
    private static final int PRIMARY = 0;
    private static final int SECONDARY = 1;

    /** Configurations, ordered by precedence */
    private ProfileConfiguration[] configurations = new ProfileConfiguration[2];
    private ExecutionEnvironment customExecutionEnvironment;

    public void overrideProfileConfiguration(String profileName, String configurationOrigin) {
        // TODO 385930 prevent this if this would invalidate a custom profile
        if (profileName == null) {
            throw new NullPointerException();
        }

        this.configurations[PRIMARY] = new ProfileConfiguration(profileName, configurationOrigin);
    }

    public void setProfileConfiguration(String profileName, String configurationOrigin) {
        // TODO 385930 prevent this if this would invalidate a custom profile
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

    public boolean isCustomProfile() {
        // TODO 385930 add explicit method for this in ExecutionEnvironmentUtils
        try {
            ExecutionEnvironmentUtils.getExecutionEnvironment(getProfileName());
            return false;
        } catch (UnknownEnvironmentException e) {
            return true;
        }
    }

    public void setFullSpecificationForCustomProfile(List<SystemCapability> systemCapabilities) {
        // TODO 385930 check if setting this makes sense
        this.customExecutionEnvironment = new CustomExecutionEnvironment(getProfileName(), systemCapabilities);
    }

    public ExecutionEnvironment getFullSpecification() {
        if (customExecutionEnvironment != null) {
            return customExecutionEnvironment;
        }

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
