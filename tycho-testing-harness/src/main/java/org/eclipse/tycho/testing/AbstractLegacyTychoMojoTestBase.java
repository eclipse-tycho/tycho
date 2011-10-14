/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testing;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.resolver.LegacyLifecycleSupport;

public class AbstractLegacyTychoMojoTestBase extends AbstractTychoMojoTestCase {

    @Override
    protected List<MavenProject> getSortedProjects(File basedir, Properties userProperties, File platform)
            throws Exception {
        List<MavenProject> sortedProjects = super.getSortedProjects(basedir, userProperties, platform);

        // execute things which used to be done in afterProjectsRead
        MavenSession session = newMavenSession(sortedProjects.get(0), sortedProjects);
        LegacyLifecycleSupport legacyLifecycle = lookup(LegacyLifecycleSupport.class);
        legacyLifecycle.afterProjectsRead(session);

        return sortedProjects;
    }

}
