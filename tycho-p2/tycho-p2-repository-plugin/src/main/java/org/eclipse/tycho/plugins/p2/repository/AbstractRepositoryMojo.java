/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.maven.AbstractP2Mojo;

public abstract class AbstractRepositoryMojo extends AbstractP2Mojo {

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(property = "buildQualifier", readonly = true)
    private String qualifier;

    protected File getAssemblyRepositoryLocation() {
        return getBuildDirectory().getChild("repository");
    }

}
