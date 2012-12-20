/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.gav;

import java.io.File;

import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

public class LocalRepositoryArtifactLocator implements GAVArtifactLocator {

    private final File localMavenRepositoryRoot;

    public LocalRepositoryArtifactLocator(File localMavenRepositoryRoot) {
        this.localMavenRepositoryRoot = localMavenRepositoryRoot;
    }

    public File getLocalArtifactLocation(GAV gav, String classifier, String extension) {
        return new File(localMavenRepositoryRoot, RepositoryLayoutHelper.getRelativePath(gav, classifier, extension));
    }

}
