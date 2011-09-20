/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.impl.repo;

import java.io.File;

import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class LocalRepositoryP2IndicesImpl implements LocalRepositoryP2Indices {

    private boolean initialized = false;
    private MavenContext mavenContext;
    private TychoRepositoryIndex artifactsIndex;
    private TychoRepositoryIndex metadataIndex;

    public LocalRepositoryP2IndicesImpl() {
    }

    private void checkInitialized() {
        if (initialized) {
            return;
        }
        File localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        this.artifactsIndex = FileBasedTychoRepositoryIndex.createArtifactsIndex(localRepositoryRoot);
        this.metadataIndex = FileBasedTychoRepositoryIndex.createMetadataIndex(localRepositoryRoot);
        initialized = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.tycho.p2.repository.LocalP2Repository#getArtifactsIndex()
     */
    public TychoRepositoryIndex getArtifactsIndex() {
        checkInitialized();
        return artifactsIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.tycho.p2.repository.LocalP2Repository#getMetadataIndex()
     */
    public TychoRepositoryIndex getMetadataIndex() {
        checkInitialized();
        return metadataIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.tycho.p2.repository.LocalP2Repository#getLocation()
     */
    public File getBasedir() {
        checkInitialized();
        return mavenContext.getLocalRepositoryRoot();
    }

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

}
