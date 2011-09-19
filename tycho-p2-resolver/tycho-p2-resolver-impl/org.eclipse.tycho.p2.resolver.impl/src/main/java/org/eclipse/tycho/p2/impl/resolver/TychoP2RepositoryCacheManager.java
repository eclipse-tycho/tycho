/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.p2.core.ProvisionException;

/**
 * @author igor
 */
@SuppressWarnings("restriction")
public class TychoP2RepositoryCacheManager extends CacheManager {
    public static final String CACHE_RELPATH = ".cache/tycho/p2-repository-metadata";

    private boolean offline;

    private File localRepositoryLocation;

    public TychoP2RepositoryCacheManager() {
        super(null);
    }

    @Override
    public File createCache(URI repositoryLocation, String prefix, IProgressMonitor monitor) throws IOException,
            ProvisionException {
        File cacheFile = getCache(repositoryLocation, prefix);
        if (offline) {
            if (cacheFile != null) {
                return cacheFile;
            }

            throw new ProvisionException("Repository system is offline and no local cache available for "
                    + repositoryLocation.toString());
        } else {
            return super.createCache(repositoryLocation, prefix, monitor);
        }
    }

    @Override
    protected File getCacheDirectory() {
        return new File(localRepositoryLocation, CACHE_RELPATH);
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public void setLocalRepositoryLocation(File localRepositoryLocation) {
        this.localRepositoryLocation = localRepositoryLocation;
    }
}
