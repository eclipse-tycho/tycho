/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.utils;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TargetPlatform;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;

public class TychoProjectUtils {
    private static final String TYCHO_NOT_CONFIGURED = "Tycho build extension not configured for ";

    /**
     * Returns the {@link TargetPlatform} instance associated with the given project.
     * 
     * @param project
     *            a Tycho project
     * @return the target platform for the given project; never <code>null</code>
     * @throws IllegalStateException
     *             if the given project does not have an associated target platform
     */
    public static TargetPlatform getTargetPlatform(MavenProject project) throws IllegalStateException {
        TargetPlatform targetPlatform = (TargetPlatform) project.getContextValue(TychoConstants.CTX_TARGET_PLATFORM);
        if (targetPlatform == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return targetPlatform;
    }

    /**
     * Returns the {@link TargetPlatformConfiguration} instance associated with the given project.
     * 
     * @param project
     *            a Tycho project
     * @return the target platform configuration for the given project; never <code>null</code>
     * @throws IllegalStateException
     *             if the given project does not have an associated target platform configuration
     */
    public static TargetPlatformConfiguration getTargetPlatformConfiguration(MavenProject project)
            throws IllegalStateException {
        TargetPlatformConfiguration targetPlatformConfiguration = (TargetPlatformConfiguration) project
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
        if (targetPlatformConfiguration == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return targetPlatformConfiguration;
    }
}
