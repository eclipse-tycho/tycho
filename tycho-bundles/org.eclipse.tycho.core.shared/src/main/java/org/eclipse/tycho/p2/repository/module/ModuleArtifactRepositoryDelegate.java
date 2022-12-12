/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.repository.module;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;

/**
 * Delegate for a {@link ModuleArtifactRepository} which additionally passes a
 * {@link WriteSessionContext} to methods that require context information about the current
 * publishing operation.
 */
@SuppressWarnings({ "deprecation" })
class ModuleArtifactRepositoryDelegate implements IFileArtifactRepository {

    private final ModuleArtifactRepository target;
    private final WriteSessionContext writeSession;

    public ModuleArtifactRepositoryDelegate(ModuleArtifactRepository target, WriteSessionContext writeSession) {
        if (target == null)
            throw new NullPointerException();
        if (writeSession == null)
            throw new NullPointerException();

        this.target = target;
        this.writeSession = writeSession;
    }

    @Override
    public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
        return target.createArtifactDescriptor(key, writeSession);
    }

    // DELEGATES

    @Override
    public void addDescriptor(IArtifactDescriptor descriptor) {
        target.addDescriptor(descriptor);
    }

    @Override
    public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        target.addDescriptor(descriptor, monitor);
    }

    @Override
    public void addDescriptors(IArtifactDescriptor[] descriptors) {
        target.addDescriptors(descriptors);
    }

    @Override
    public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        target.addDescriptors(descriptors, monitor);
    }

    @Override
    public boolean contains(IArtifactDescriptor descriptor) {
        return target.contains(descriptor);
    }

    @Override
    public boolean contains(IArtifactKey key) {
        return target.contains(key);
    }

    @Override
    public IArtifactKey createArtifactKey(String classifier, String id, Version version) {
        return target.createArtifactKey(classifier, id, version);
    }

    @Override
    public IQueryable<IArtifactDescriptor> descriptorQueryable() {
        return target.descriptorQueryable();
    }

    @Override
    public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
        return target.executeBatch(runnable, monitor);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return target.getAdapter(adapter);
    }

    @Override
    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        return target.getArtifact(descriptor, destination, monitor);
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        return target.getArtifactDescriptors(key);
    }

    @Override
    public File getArtifactFile(IArtifactDescriptor descriptor) {
        return target.getArtifactFile(descriptor);
    }

    @Override
    public File getArtifactFile(IArtifactKey key) {
        return target.getArtifactFile(key);
    }

    @Override
    public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
        return target.getArtifacts(requests, monitor);
    }

    @Override
    public String getDescription() {
        return target.getDescription();
    }

    @Override
    public URI getLocation() {
        return target.getLocation();
    }

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        return target.getOutputStream(descriptor);
    }

    @Override
    public Map<String, String> getProperties() {
        return target.getProperties();
    }

    @Override
    public String getProperty(String key) {
        return target.getProperty(key);
    }

    @Override
    public String getProvider() {
        return target.getProvider();
    }

    @Override
    public IProvisioningAgent getProvisioningAgent() {
        return target.getProvisioningAgent();
    }

    @Override
    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        return target.getRawArtifact(descriptor, destination, monitor);
    }

    @Override
    public String getType() {
        return target.getType();
    }

    @Override
    public String getVersion() {
        return target.getVersion();
    }

    @Override
    public boolean isModifiable() {
        return target.isModifiable();
    }

    @Override
    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return target.query(query, monitor);
    }

    @Override
    public void removeAll() {
        target.removeAll();
    }

    @Override
    public void removeAll(IProgressMonitor monitor) {
        target.removeAll(monitor);
    }

    @Override
    public void removeDescriptor(IArtifactDescriptor descriptor) {
        target.removeDescriptor(descriptor);
    }

    @Override
    public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        target.removeDescriptor(descriptor, monitor);
    }

    @Override
    public void removeDescriptor(IArtifactKey key) {
        target.removeDescriptor(key);
    }

    @Override
    public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
        target.removeDescriptor(key, monitor);
    }

    @Override
    public void removeDescriptors(IArtifactDescriptor[] descriptors) {
        target.removeDescriptors(descriptors);
    }

    @Override
    public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        target.removeDescriptors(descriptors, monitor);
    }

    @Override
    public void removeDescriptors(IArtifactKey[] keys) {
        target.removeDescriptors(keys);
    }

    @Override
    public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
        target.removeDescriptors(keys, monitor);
    }

    @Override
    public String setProperty(String key, String value) {
        return target.setProperty(key, value);
    }

    @Override
    public String setProperty(String key, String value, IProgressMonitor monitor) {
        return target.setProperty(key, value, monitor);
    }

}
