/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 * workarounds for Bug 357357, Bug 571195
 */
@SuppressWarnings("restriction")
class OfflineTransport extends Transport {

    @Override
    public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {
        return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, createMessage(toDownload));
    }

    /*
     * This is the only method expected to be called in offline mode.
     * 
     * @see AbstractRepositoryManager#loadIndexFile(URI location, IProgressMonitor monitor)
     */
    @Override
    public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
        return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, createMessage(toDownload));
    }

    @Override
    public InputStream stream(URI toDownload, IProgressMonitor monitor)
            throws FileNotFoundException, CoreException, AuthenticationFailedException {
        throw new FileNotFoundException(createMessage(toDownload));
    }

    @Override
    public long getLastModified(URI toDownload, IProgressMonitor monitor)
            throws CoreException, FileNotFoundException, AuthenticationFailedException {
        return 0;
    }

    private static String createMessage(URI toDownload) {
        return String.format("maven is currently in offline mode, requested URI: %s", toDownload);
    }
}
