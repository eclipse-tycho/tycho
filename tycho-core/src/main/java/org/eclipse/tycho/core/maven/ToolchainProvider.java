/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.util.Collections;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = ToolchainProvider.class)
public class ToolchainProvider {

    @Requirement
    ToolchainManagerPrivate toolChainManager;

    public static enum JDKUsage {
        SYSTEM, BREE;
    }

    /**
     * Finds a matching {@link DefaultJavaToolChain} in the maven toolchains for a given maven
     * session and toolchain id. Returns the toolchain or null if no toolchain could be found.
     * 
     * @param session
     *            The maven session
     * @param toolchainId
     *            The id of the toolchain
     * @return the toolchain that matches or null if no toolchain could be found
     * @throws MojoExecutionException
     *             if the toolchains are misconfigured
     */
    public DefaultJavaToolChain findMatchingJavaToolChain(final MavenSession session, final String toolchainId)
            throws MojoExecutionException {
        try {
            final Map<String, String> requirements = Collections.singletonMap("id", toolchainId);
            for (ToolchainPrivate javaToolChain : toolChainManager.getToolchainsForType("jdk", session)) {
                if (javaToolChain.matchesRequirements(requirements)) {
                    if (javaToolChain instanceof DefaultJavaToolChain) {
                        return ((DefaultJavaToolChain) javaToolChain);
                    }
                }
            }
            return null;

        } catch (MisconfiguredToolchainException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
