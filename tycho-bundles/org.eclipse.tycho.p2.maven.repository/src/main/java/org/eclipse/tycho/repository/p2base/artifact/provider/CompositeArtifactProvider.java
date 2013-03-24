/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static org.eclipse.tycho.repository.util.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

public class CompositeArtifactProvider extends CompositeArtifactProviderBaseImpl implements IRawArtifactFileProvider {

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

    @Override
    protected void getArtifactDescriptorsOfAllSources(IArtifactKey key, Set<IArtifactDescriptor> result) {
        for (IRawArtifactFileProvider component : components) {
            for (IArtifactDescriptor descriptor : component.getArtifactDescriptors(key)) {
                result.add(descriptor);
            }
        }
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

    @Override
    protected void getArtifactFromAnySource(IArtifactSink sink, List<IStatus> statusCollector, IProgressMonitor monitor)
            throws ArtifactSinkException {

        IArtifactKey requestedKey = sink.getArtifactToBeWritten();

        for (IRawArtifactFileProvider component : components) {
            if (component.contains(requestedKey)) {

                if (!sink.canBeginWrite()) {
                    return;
                }
                IStatus transferStatus = component.getArtifact(sink, monitor);

                statusCollector.add(transferStatus);
                if (!isFatal(transferStatus)) {
                    // read was successful -> done
                    return;
                }
            }
        }
    }

    @Override
    protected void getRawArtifactFromAnySource(IRawArtifactSink sink, IProgressMonitor monitor,
            List<IStatus> statusCollector) throws ArtifactSinkException {
        IArtifactDescriptor requestedDescriptor = sink.getArtifactFormatToBeWritten();

        for (IRawArtifactFileProvider component : components) {
            if (component.contains(requestedDescriptor)) {

                if (!sink.canBeginWrite()) {
                    return;
                }
                IStatus transferStatus = component.getRawArtifact(sink, monitor);

                statusCollector.add(transferStatus);
                if (!isFatal(transferStatus)) {
                    // read was successful -> done
                    return;
                }
            }
        }
    }

    @Override
    protected Status getArtifactNotFoundError(String artifact) {
        return new Status(IStatus.ERROR, BUNDLE_ID, ProvisionException.ARTIFACT_NOT_FOUND, "Artifact " + artifact
                + " is not available in the following sources: " + components, null);
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return componentsAsQueriable().query(query, monitor);
    }

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
