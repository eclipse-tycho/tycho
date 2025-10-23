/*******************************************************************************
 * Copyright (c) 2018, 2024 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.compiler.jdt;

import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.tycho.compiler.jdt.copied.LibraryInfo;

/**
 * Determine and cache system library info (Java version, bootclasspath, extension and endorsed
 * directories) for given javaHome directories.
 */
public interface JdkLibraryInfoProvider {

    LibraryInfo getLibraryInfo(String javaHome) throws ArtifactResolutionException;

}
