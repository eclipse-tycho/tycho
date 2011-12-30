/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;

/**
 * Component interface that allows contribution of additional p2 metadata to reactor projects.
 * Implementations of this interface will be invoked as part of reactor project setup logic and
 * contributed metadata will be used to establish project dependencies and reactor build order.
 */
public interface P2MetadataProvider {
    // TODO consider allowing MavenExecutionException
    public void setupProject(MavenSession session, MavenProject project, ReactorProject reactorProject);
}
