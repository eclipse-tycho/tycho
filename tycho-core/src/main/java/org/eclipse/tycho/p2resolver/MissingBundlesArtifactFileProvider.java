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
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.ArtifactSinkException;
import org.eclipse.tycho.IArtifactSink;
import org.eclipse.tycho.IRawArtifactFileProvider;
import org.eclipse.tycho.IRawArtifactSink;

class MissingBundlesArtifactFileProvider implements IRawArtifactFileProvider {

    private Map<IArtifactKey, File> mappedFiles = new HashMap<>();

    @Override
    public File getArtifactFile(IArtifactKey key) {
        return mappedFiles.get(key);
    }

    @Override
    public boolean isFileAlreadyAvailable(IArtifactKey artifactKey) {
        return contains(artifactKey);
    }

    @Override
    public boolean contains(IArtifactKey key) {
        return mappedFiles.containsKey(key);
    }

    @Override
    public IStatus getArtifact(IArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException {
        return Status.CANCEL_STATUS;
    }

    @Override
    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return query.perform(mappedFiles.keySet().iterator());
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        return new IArtifactDescriptor[0];
    }

    @Override
    public boolean contains(IArtifactDescriptor descriptor) {
        return false;
    }

    @Override
    public IStatus getRawArtifact(IRawArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException {
        return Status.CANCEL_STATUS;
    }

    @Override
    public File getArtifactFile(IArtifactDescriptor descriptor) {

        return getArtifactFile(descriptor.getArtifactKey());
    }

    void add(IArtifactKey key, File file) {
        mappedFiles.put(key, file);
    }

}
