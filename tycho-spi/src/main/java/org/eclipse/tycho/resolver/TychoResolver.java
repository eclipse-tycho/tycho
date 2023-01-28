/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype Inc. and others.
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
package org.eclipse.tycho.resolver;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public interface TychoResolver {
    /**
     * Performs basic project setup of a maven project for a given session
     * 
     * @param session
     * @param project
     */
    void setupProject(MavenSession session, MavenProject project);

    /**
     * Performs resolve operation of Tycho dependencies and inject the result into the maven model
     * of the given project
     * 
     * @param session
     * @param project
     */
    void resolveProject(MavenSession session, MavenProject project);

}
