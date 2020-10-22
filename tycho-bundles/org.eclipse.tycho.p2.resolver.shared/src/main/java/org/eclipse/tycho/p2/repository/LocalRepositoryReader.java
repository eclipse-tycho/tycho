/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.repository;

import java.io.File;

public class LocalRepositoryReader implements RepositoryReader {

    private final File localMavenRepositoryRoot;

    public LocalRepositoryReader(File localMavenRepositoryRoot) {
        this.localMavenRepositoryRoot = localMavenRepositoryRoot;
    }

    @Override
    public File getLocalArtifactLocation(GAV gav, String classifier, String extension) {
        return new File(localMavenRepositoryRoot, RepositoryLayoutHelper.getRelativePath(gav, classifier, extension));
    }

}
