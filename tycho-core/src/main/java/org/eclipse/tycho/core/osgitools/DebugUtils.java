/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
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
