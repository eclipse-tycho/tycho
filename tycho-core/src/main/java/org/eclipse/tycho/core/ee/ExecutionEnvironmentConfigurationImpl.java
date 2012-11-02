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
    private final ProfileConfiguration[] configurations = new ProfileConfiguration[2];
    private boolean configurationFreeze = false;

    private ExecutionEnvironment customExecutionEnvironment;

    public void overrideProfileConfiguration(String profileName, String configurationOrigin)
            throws IllegalStateException {
        checkConfigurationMutable();
        if (profileName == null) {
            throw new NullPointerException();
        }

        this.configurations[PRIMARY] = new ProfileConfiguration(profileName, configurationOrigin);
    }

    public void setProfileConfiguration(String profileName, String configurationOrigin) throws IllegalStateException {
        checkConfigurationMutable();
        if (profileName == null) {
            throw new NullPointerException();
        }

        this.configurations[SECONDARY] = new ProfileConfiguration(profileName, configurationOrigin);
    }

    private void checkConfigurationMutable() throws IllegalStateException {
        if (configurationFreeze) {
            throw new IllegalStateException("Cannot change execution environment configuration after it has been used");
        }
    }

    private ProfileConfiguration getEffectiveConfiguration() {
        // disallow further configuration changes
        // TODO debug log entry? configuration origin is effectively no longer shown
        configurationFreeze = true;

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
        String profileName = getProfileName();
        if (profileName == null) {
            return false;
        }

        // TODO 385930 add explicit method for this in ExecutionEnvironmentUtils
        try {
            ExecutionEnvironmentUtils.getExecutionEnvironment(profileName);
            return false;
        } catch (UnknownEnvironmentException e) {
            return true;
        }
    }

    public void setFullSpecificationForCustomProfile(List<SystemCapability> systemCapabilities)
            throws IllegalStateException {
        if (!isCustomProfile()) {
            throw new IllegalStateException(
                    "Cannot set full specification when a standard execution environment is configured");
        }
        // TODO this is not possible while the target platform is still computed multiple times
//        if (this.customExecutionEnvironment != null) {
//            throw new IllegalStateException("Cannot set full specification for a custom profile more than once");
//        }

        this.customExecutionEnvironment = new CustomExecutionEnvironment(getProfileName(), systemCapabilities);
    }

    public ExecutionEnvironment getFullSpecification() throws IllegalStateException {
        if (isCustomProfile()) {
            if (customExecutionEnvironment == null) {
                throw new IllegalStateException("Full specification of custom profile is not (yet) determined");
            }
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
            throw new IllegalArgumentException("Invalid execution environment '" + configuration.profileName
                    + "' specified in " + configuration.origin, e);
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
