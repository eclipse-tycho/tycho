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
package org.eclipse.tycho.core.utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public class MavenSessionUtils {
    public static MavenProject getMavenProject(MavenSession session, String basedir) {
        return getMavenProject(session, new File(basedir));
    }

    public static MavenProject getMavenProject(MavenSession session, File basedir) {
        for (MavenProject project : session.getProjects()) {
            if (basedir.equals(project.getBasedir())) {
                return project;
            }
        }

        return null; // not a reactor project
    }

    public static Map<File, MavenProject> getBasedirMap(MavenSession session) {
        return getBasedirMap(session.getProjects());
    }

    public static Map<File, MavenProject> getBasedirMap(List<MavenProject> projects) {
        HashMap<File, MavenProject> result = new HashMap<>();

        for (MavenProject project : projects) {
            result.put(project.getBasedir(), project);
        }

        return result;
    }
}
