/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.shared.BuildFailureException;

public class ExecutionEnvironmentConfigurationImpl implements ExecutionEnvironmentConfiguration {
    // Most likely best to always be the latest known supported EE
    private static final String DEFAULT_EXECUTION_ENVIRONMENT = "JavaSE-"
            + Integer.toString(Runtime.version().feature());

    private static final int PRIMARY = 0;
    private static final int SECONDARY = 1;

    private Logger logger;

    /** Configurations, ordered by precedence */
    private final ProfileConfiguration[] configurations = new ProfileConfiguration[2];

    private String effectiveProfileName = null;
    private CustomExecutionEnvironment customExecutionEnvironment;

    private final boolean ignoredByResolver;

    private final ToolchainManager toolchainManager;

    private MavenSession session;

    public ExecutionEnvironmentConfigurationImpl(Logger logger, boolean ignoredByResolver,
            ToolchainManager toolchainManager, MavenSession session) {
        this.logger = logger;
        this.ignoredByResolver = ignoredByResolver;
        this.toolchainManager = toolchainManager;
        this.session = session;
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
        if (ignoreExecutionEnvironment()) {
            return false;
        }
        String profileName = getProfileName();
        try {
            ExecutionEnvironmentUtils.getExecutionEnvironment(profileName, toolchainManager, session, logger);
            return false;
        } catch (UnknownEnvironmentException ex) {
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
        if (ignoreExecutionEnvironment()) {
            return NoExecutionEnvironment.INSTANCE;
        }
        if (isCustomProfile()) {
            if (customExecutionEnvironment == null) {
                throw new IllegalStateException(
                        "Full specification of custom profile '" + getProfileName() + "' is not (yet) determined");
            }
            return customExecutionEnvironment;
        }
        return ExecutionEnvironmentUtils.getExecutionEnvironment(getProfileName(), toolchainManager, session, logger);
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

    @Override
    public Collection<ExecutionEnvironment> getAllKnownEEs() {
        return ExecutionEnvironmentUtils.getProfileNames(toolchainManager, session, logger).stream() //
                .map(profileName -> ExecutionEnvironmentUtils.getExecutionEnvironment(profileName, toolchainManager,
                        session, logger)) //
                .collect(Collectors.toList());
    }

    @Override
    public boolean ignoreExecutionEnvironment() {
        return NoExecutionEnvironment.NAME.equals(getProfileName());
    }

}
