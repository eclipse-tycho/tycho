/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.util.List;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.shared.BuildFailureException;

public class ExecutionEnvironmentConfigurationImpl implements ExecutionEnvironmentConfiguration {
    // Most likely best to always be the latest known supported EE
    private static final String DEFAULT_EXECUTION_ENVIRONMENT = "JavaSE-9";

    private static final int PRIMARY = 0;
    private static final int SECONDARY = 1;

    private Logger logger;

    /** Configurations, ordered by precedence */
    private final ProfileConfiguration[] configurations = new ProfileConfiguration[2];

    private String effectiveProfileName = null;
    private CustomExecutionEnvironment customExecutionEnvironment;

    private final boolean ignoredByResolver;

    public ExecutionEnvironmentConfigurationImpl(Logger logger, boolean ignoredByResolver) {
        this.logger = logger;
        this.ignoredByResolver = ignoredByResolver;
    }

    @Override
    public void overrideProfileConfiguration(String profileName, String configurationOrigin)
            throws IllegalStateException {
        checkConfigurationMutable();
        if (profileName == null) {
            throw new NullPointerException();
        }

        this.configurations[PRIMARY] = new ProfileConfiguration(profileName, configurationOrigin);
    }

    @Override
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

    @Override
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

    @Override
    public boolean isCustomProfile() {
        String profileName = getProfileName();

        // TODO 385930 add explicit method for this in ExecutionEnvironmentUtils
        try {
            ExecutionEnvironmentUtils.getExecutionEnvironment(profileName);
            return false;
        } catch (UnknownEnvironmentException e) {
            if (ignoredByResolver) {
                throw new BuildFailureException(
                        "When using a custom execution environment profile, resolveWithExecutionEnvironmentConstraints must not be set to false");
            }
            return true;
        }
    }

    @Override
    public void setFullSpecificationForCustomProfile(List<SystemCapability> systemCapabilities)
            throws IllegalStateException {
        if (!isCustomProfile()) {
            throw new IllegalStateException(
                    "Cannot set full specification when a standard execution environment is configured");
        }
        if (this.customExecutionEnvironment != null) {
            throw new IllegalStateException("Cannot set full specification for a custom profile more than once");
        }

        this.customExecutionEnvironment = new CustomExecutionEnvironment(getProfileName(), systemCapabilities);
    }

    @Override
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

    @Override
    public boolean isIgnoredByResolver() {
        return ignoredByResolver;
    }

}
