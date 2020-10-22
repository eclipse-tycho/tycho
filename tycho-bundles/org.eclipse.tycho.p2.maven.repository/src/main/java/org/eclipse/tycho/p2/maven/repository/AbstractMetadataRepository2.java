/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
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
package org.eclipse.tycho.p2.maven.repository;

import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IPool;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;

/**
 * More reasonable base class for Tycho's metadata repositories than
 * {@link AbstractMetadataRepository}.
 */
public abstract class AbstractMetadataRepository2 extends AbstractRepository<IInstallableUnit> implements
        IMetadataRepository {

    /**
     * Creates a metadata repository with the provided parameters (and some default values that are
     * reasonable for Tycho).
     * 
     * @param agent
     *            the provisioning agent that may be used by repository
     * @param name
     *            the name of the repository
     * @param type
     *            the repository type as in
     *            {@link IMetadataRepositoryManager#createRepository(URI, String, String, java.util.Map)}
     * @param location
     *            the physical location of the repository
     */
    protected AbstractMetadataRepository2(IProvisioningAgent agent, String name, String type, File location) {
        super(agent, name, type, "1.0.0", location.toURI(), null, null, null);
    }

    @Override
    public void addReferences(Collection<? extends IRepositoryReference> references) {
        // not supported
    }

    @Override
    public Collection<IRepositoryReference> getReferences() {
        // not supported
        return Collections.emptyList();
    }

    @Override
    public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
        try {
            // TODO do we need better support for batch operations?
            runnable.run(monitor);
        } catch (OperationCanceledException oce) {
            return new Status(IStatus.CANCEL, BUNDLE_ID, oce.getMessage(), oce);
        } catch (Exception e) {
            return new Status(IStatus.ERROR, BUNDLE_ID, e.getMessage(), e);
        }
        return Status.OK_STATUS;
    }

    @Override
    public void compress(IPool<IInstallableUnit> iuPool) {
        // do nothing
    }

}
