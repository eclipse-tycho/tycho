/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.tycho.p2.impl.Activator;

/**
 * workaround for https://bugs.eclipse.org/357357
 */
@SuppressWarnings("restriction")
class OfflineTransport extends Transport {

    private static final Status OFFLINE_STATUS = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "offline");

    @Override
    public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {
        throw new IllegalStateException("no download in offline mode");
    }

    /*
     * This is the only method expected to be called in offline mode.
     * 
     * @see AbstractRepositoryManager#loadIndexFile(URI location, IProgressMonitor monitor)
     */
    @Override
    public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
        return OFFLINE_STATUS;
    }

    @Override
    public InputStream stream(URI toDownload, IProgressMonitor monitor) throws FileNotFoundException, CoreException,
            AuthenticationFailedException {
        throw new IllegalStateException("no download in offline mode");
    }

    @Override
    public long getLastModified(URI toDownload, IProgressMonitor monitor) throws CoreException, FileNotFoundException,
            AuthenticationFailedException {
        throw new IllegalStateException("no download in offline mode");
    }

}
