/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public class DebugUtils {
    public static boolean isDebugEnabled(MavenSession session, MavenProject project) {
        String config = session.getUserProperties().getProperty("tycho.debug.resolver");
        return config != null && (config.trim().equals(project.getArtifactId()) || "true".equals(config.trim()));
    }
}
