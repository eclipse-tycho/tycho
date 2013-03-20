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

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;

public class ExecutionEnvironmentConfigurationImpl implements ExecutionEnvironmentConfiguration {
    private static final String DEFAULT_EXECUTION_ENVIRONMENT = "JavaSE-1.6";

    private static final int PRIMARY = 0;
    private static final int SECONDARY = 1;

    private Logger logger;

    /** Configurations, ordered by precedence */
    private final ProfileConfiguration[] configurations = new ProfileConfiguration[2];

    private String effectiveProfileName = null;
    private CustomExecutionEnvironment customExecutionEnvironment;

    public ExecutionEnvironmentConfigurationImpl(Logger logger) {
        this.logger = logger;
    }

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
        if (effectiveProfileName != null) {
            throw new IllegalStateException("Cannot change execution environment configuration after it has been used");
        }
    }

    public String getProfileName() {
        if (effectiveProfileName == null) {
            // this also disallows further configuration changes
            effectiveProfileName = computeEffectiveProfileName();
        }
        return effectiveProfileName;
    }

    private String computeEffectiveProfileName() {
        for (ProfileConfiguration entry : configurations) {
            if (entry != null) {
                logger.debug("Using execution environment '" + entry.profileName + "' configured in " + entry.origin);
                return entry.profileName;
            }
        }

        logger.debug("Using default execution environment '" + DEFAULT_EXECUTION_ENVIRONMENT + "'");
        return DEFAULT_EXECUTION_ENVIRONMENT;
    }

    public boolean isCustomProfile() {
        String profileName = getProfileName();

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

        return ExecutionEnvironmentUtils.getExecutionEnvironment(getProfileName());
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
