/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.utils;

import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

public class TychoProjectUtils {

    public static final String TYCHO_ENV_OSGI_WS = "tycho.env.osgi.ws";
    public static final String TYCHO_ENV_OSGI_OS = "tycho.env.osgi.os";
    public static final String TYCHO_ENV_OSGI_ARCH = "tycho.env.osgi.arch";

    /**
     * Computes the merged properties from maven session and the project
     * 
     * @param mavenProject
     * @param mavenSession
     * @return a (possibly cached) value, do not modify the object!
     */
    public static Properties getMergedProperties(MavenProject mavenProject, MavenSession mavenSession) {
        if (mavenSession == null) {
            //only temporary ...
            return computeMergedProperties(mavenProject, null);
        }
        return DefaultReactorProject.adapt(mavenProject, mavenSession).computeContextValue(
                ReactorProject.CTX_MERGED_PROPERTIES, () -> computeMergedProperties(mavenProject, mavenSession));
    }

    private static Properties computeMergedProperties(MavenProject mavenProject, MavenSession mavenSession) {
        Properties properties = new Properties();
        properties.putAll(mavenProject.getProperties());
        if (mavenSession != null) {
            properties.putAll(mavenSession.getSystemProperties()); // session wins
            properties.putAll(mavenSession.getUserProperties());
        }
        setTychoEnvironmentProperties(properties, mavenProject);
        return properties;
    }

    public static void setTychoEnvironmentProperties(Properties properties, MavenProject project) {
        String arch = PlatformPropertiesUtils.getArch(properties);
        String os = PlatformPropertiesUtils.getOS(properties);
        String ws = PlatformPropertiesUtils.getWS(properties);
        project.getProperties().put(TYCHO_ENV_OSGI_WS, ws);
        project.getProperties().put(TYCHO_ENV_OSGI_OS, os);
        project.getProperties().put(TYCHO_ENV_OSGI_ARCH, arch);
    }
}
