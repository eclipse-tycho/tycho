/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class CompositeArtifactProvider implements IRawArtifactFileProvider {

    private List<IRawArtifactFileProvider> components;

    public CompositeArtifactProvider(IRawArtifactFileProvider... components) {
        this.components = Arrays.asList(components);
    }

    public boolean contains(IArtifactKey key) {
        for (IRawArtifactFileProvider component : components) {
            if (component.contains(key))
                return true;
        }
        return false;
    }

    public boolean contains(IArtifactDescriptor descriptor) {
        for (IRawArtifactFileProvider component : components) {
            if (component.contains(descriptor))
                return true;
        }
        return false;
    }

    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        Set<IArtifactDescriptor> result = new HashSet<IArtifactDescriptor>();
        for (IRawArtifactFileProvider component : components) {
            for (IArtifactDescriptor descriptor : component.getArtifactDescriptors(key)) {
                result.add(descriptor);
            }
        }
        return result.toArray(new IArtifactDescriptor[result.size()]);
    }

    public File getArtifactFile(IArtifactKey key) {
        for (IRawArtifactFileProvider component : components) {
            if (component.contains(key)) {
                return component.getArtifactFile(key);
            }
        }
        return null;
    }

    public File getArtifactFile(IArtifactDescriptor descriptor) {
        for (IRawArtifactFileProvider component : components) {
            if (component.contains(descriptor)) {
                return component.getArtifactFile(descriptor);
            }
        }
        return null;
    }

    public IStatus getArtifact(IArtifactKey key, OutputStream destination, IProgressMonitor monitor) {
        for (IRawArtifactFileProvider component : components) {
            if (component.contains(key)) {
                return component.getArtifact(key, destination, monitor);
            }
        }
        // TODO forward to one of the components for the error message?
        return Status.CANCEL_STATUS;
    }

    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        for (IRawArtifactFileProvider component : components) {
            if (component.contains(descriptor)) {
                return component.getRawArtifact(descriptor, destination, monitor);
            }
        }
        // TODO forward to one of the components for the error message?
        return Status.CANCEL_STATUS;
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor)
            throws ArtifactProviderException {
        return componentsAsQueriable().query(query, monitor);
    }

    // TODO share?
    private IQueryable<IArtifactKey> componentsAsQueriable() {
        int repositoryCount = components.size();
        if (repositoryCount == 1) {
            return components.get(0);
        } else {
            IArtifactProvider[] repositoriesArray = components.toArray(new IArtifactProvider[repositoryCount]);
            return new CompoundQueryable<IArtifactKey>(repositoriesArray);
        }
    }
}
