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
package org.eclipse.tycho.repository.module;

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
@SuppressWarnings({ "rawtypes", "deprecation" })
class ModuleArtifactRepositoryDelegate implements IFileArtifactRepository {

    private final ModuleArtifactRepository target;
    private final WriteSessionContext writeSession;

    public ModuleArtifactRepositoryDelegate(ModuleArtifactRepository target, WriteSessionContext writeSession) {
        this.target = target;
        this.writeSession = writeSession;
    }

    public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
        return target.createArtifactDescriptor(key, writeSession);
    }

    // DELEGATES

    public void addDescriptor(IArtifactDescriptor descriptor) {
        target.addDescriptor(descriptor);
    }

    public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        target.addDescriptor(descriptor, monitor);
    }

    public void addDescriptors(IArtifactDescriptor[] descriptors) {
        target.addDescriptors(descriptors);
    }

    public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        target.addDescriptors(descriptors, monitor);
    }

    public boolean contains(IArtifactDescriptor descriptor) {
        return target.contains(descriptor);
    }

    public boolean contains(IArtifactKey key) {
        return target.contains(key);
    }

    public IArtifactKey createArtifactKey(String classifier, String id, Version version) {
        return target.createArtifactKey(classifier, id, version);
    }

    public IQueryable<IArtifactDescriptor> descriptorQueryable() {
        return target.descriptorQueryable();
    }

    public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
        return target.executeBatch(runnable, monitor);
    }

    public Object getAdapter(Class adapter) {
        return target.getAdapter(adapter);
    }

    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        return target.getArtifact(descriptor, destination, monitor);
    }

    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        return target.getArtifactDescriptors(key);
    }

    public File getArtifactFile(IArtifactDescriptor descriptor) {
        return target.getArtifactFile(descriptor);
    }

    public File getArtifactFile(IArtifactKey key) {
        return target.getArtifactFile(key);
    }

    public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
        return target.getArtifacts(requests, monitor);
    }

    public String getDescription() {
        return target.getDescription();
    }

    public URI getLocation() {
        return target.getLocation();
    }

    public String getName() {
        return target.getName();
    }

    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        return target.getOutputStream(descriptor);
    }

    public Map<String, String> getProperties() {
        return target.getProperties();
    }

    public String getProperty(String key) {
        return target.getProperty(key);
    }

    public String getProvider() {
        return target.getProvider();
    }

    public IProvisioningAgent getProvisioningAgent() {
        return target.getProvisioningAgent();
    }

    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        return target.getRawArtifact(descriptor, destination, monitor);
    }

    public String getType() {
        return target.getType();
    }

    public String getVersion() {
        return target.getVersion();
    }

    public boolean isModifiable() {
        return target.isModifiable();
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return target.query(query, monitor);
    }

    public void removeAll() {
        target.removeAll();
    }

    public void removeAll(IProgressMonitor monitor) {
        target.removeAll(monitor);
    }

    public void removeDescriptor(IArtifactDescriptor descriptor) {
        target.removeDescriptor(descriptor);
    }

    public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        target.removeDescriptor(descriptor, monitor);
    }

    public void removeDescriptor(IArtifactKey key) {
        target.removeDescriptor(key);
    }

    public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
        target.removeDescriptor(key, monitor);
    }

    public void removeDescriptors(IArtifactDescriptor[] descriptors) {
        target.removeDescriptors(descriptors);
    }

    public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        target.removeDescriptors(descriptors, monitor);
    }

    public void removeDescriptors(IArtifactKey[] keys) {
        target.removeDescriptors(keys);
    }

    public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
        target.removeDescriptors(keys, monitor);
    }

    public String setProperty(String key, String value) {
        return target.setProperty(key, value);
    }

    public String setProperty(String key, String value, IProgressMonitor monitor) {
        return target.setProperty(key, value, monitor);
    }

}
