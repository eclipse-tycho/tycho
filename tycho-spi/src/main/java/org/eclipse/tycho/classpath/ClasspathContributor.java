/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.classpath;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ClasspathEntry;

/**
 * A {@link ClasspathContributor} can contribute additional items to the compile classpath of a
 * project
 *
 */
public interface ClasspathContributor {

    List<ClasspathEntry> getAdditionalClasspathEntries(MavenProject project, String scope);
}
