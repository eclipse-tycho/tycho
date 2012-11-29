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
package org.eclipse.tycho.resolver;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public interface LegacyLifecycleSupport {

    void afterProjectsRead(MavenSession session);

    void setupProjects(MavenSession session);

    void resolveProject(MavenSession session, MavenProject project);

}
