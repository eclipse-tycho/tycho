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
package org.eclipse.tycho.packaging;

import java.util.jar.Manifest;

import org.apache.maven.project.MavenProject;

/**
 * A manifest processor can process manifest entries before they are packaged
 */
public interface ManifestProcessor {

    void processManifest(MavenProject mavenProject, Manifest manifest);
}
