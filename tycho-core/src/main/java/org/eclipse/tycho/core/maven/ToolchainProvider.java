/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronics GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.java.JavaToolchainImpl;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;

@Named
@SessionScoped
public class ToolchainProvider {

    private static final String RUNNING_PROFILE_NAME = "JavaSE-" + Runtime.version().feature();

    static final String TYPE_JDK = "jdk";

    @Inject
    ToolchainManagerPrivate toolChainManager;

    @Inject
    ToolchainManager toolchainManager;

    @Inject
    LegacySupport legacySupport;

    @Inject
    Logger logger;

    private Map<ToolchainKey, Optional<OSGiJavaToolchain>> toolchainMap = new ConcurrentHashMap<>();

    private MavenSession mavenSession;

    public static enum JDKUsage {
        SYSTEM, BREE;
    }

    @Inject
    public ToolchainProvider(MavenSession mavenSession) {
        this.mavenSession = mavenSession;
    }

    public Optional<OSGiJavaToolchain> getToolchain(String profileName) {
        return getToolchain(JDKUsage.BREE, profileName).or(() -> getSystemToolchain());
    }

    public Optional<OSGiJavaToolchain> getToolchain(JDKUsage usage, String profileName) {
        if (usage == JDKUsage.SYSTEM) {
            return getSystemToolchain();
        }
        return toolchainMap.computeIfAbsent(new ToolchainKey(usage, profileName), key -> {
            Toolchain toolchain = ExecutionEnvironmentUtils.getToolchainFor(profileName,
                    TargetEnvironment.getRunningEnvironment(), toolchainManager, getMavenSession(), logger);
            if (toolchain != null) {
                return Optional.of(new OSGiJavaToolchain(toolchain));
            }
            //Special case the requested profile is the one of the running JVM, but not defined in the toolchains, we can handle this like a JAVA_HOME defined one!
            if (RUNNING_PROFILE_NAME.equals(profileName)) {
                String javaHomeProperty = System.getProperty("java.home");
                if (javaHomeProperty != null) {
                    JavaHomeToolchain javaHomeToolchain = new JavaHomeToolchain(javaHomeProperty);
                    if (javaHomeToolchain.findTool("java") != null) {
                        return Optional.of(new OSGiJavaToolchain(javaHomeToolchain));
                    }
                }
            }
            return Optional.empty();
        });
    }

    private Optional<OSGiJavaToolchain> getSystemToolchain() {
        Toolchain contextToolchain = toolchainManager.getToolchainFromBuildContext(TYPE_JDK, getMavenSession());
        if (contextToolchain != null) {
            //this is the one configured by the maven toolchain plugin...
            return Optional.of(new OSGiJavaToolchain(contextToolchain));
        }
        //Fallback to running system ...
        Optional<OSGiJavaToolchain> profileToolchain = getToolchain(JDKUsage.BREE, RUNNING_PROFILE_NAME);
        if (profileToolchain.isPresent()) {
            return profileToolchain;
        }
        //Fallback to java home ...
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            JavaHomeToolchain javaHomeToolchain = new JavaHomeToolchain(javaHome);
            if (javaHomeToolchain.findTool("java") != null) {
                return Optional.of(new OSGiJavaToolchain(javaHomeToolchain));
            }
        }
        return Optional.empty();
    }

    private MavenSession getMavenSession() {
        MavenSession session = legacySupport.getSession();
        if (session != null) {
            return session;
        }
        return mavenSession;
    }

    /**
     * Finds a matching {@link JavaToolchainImpl} in the maven toolchains for a given maven session
     * and toolchain id. Returns the toolchain or null if no toolchain could be found.
     * 
     * @param session
     *            The maven session
     * @param toolchainId
     *            The id of the toolchain
     * @return the toolchain that matches or null if no toolchain could be found
     * @throws MojoExecutionException
     *             if the toolchains are misconfigured
     */
    public JavaToolchainImpl findMatchingJavaToolChain(final MavenSession session, final String toolchainId)
            throws MojoExecutionException {
        try {
            final Map<String, String> requirements = Collections.singletonMap("id", toolchainId);
            for (ToolchainPrivate javaToolChain : toolChainManager.getToolchainsForType("jdk", session)) {
                if (javaToolChain.matchesRequirements(requirements)
                        && javaToolChain instanceof JavaToolchainImpl javaToolchain) {
                    return javaToolchain;
                }
            }
            return null;

        } catch (MisconfiguredToolchainException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static final record ToolchainKey(JDKUsage usage, String profileName) {

    }

}
