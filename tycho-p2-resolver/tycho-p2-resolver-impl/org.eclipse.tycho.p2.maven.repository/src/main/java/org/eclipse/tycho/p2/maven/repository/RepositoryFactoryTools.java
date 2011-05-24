/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;

class RepositoryFactoryTools {
    /**
     * Returns the given {@link URI} as {@link File}, or <code>null</code> if the given URI does not
     * have a "file:" scheme.
     */
    static File asFile(URI location) {
        if ("file".equals(location.getScheme())) {
            return new File(location);
        } else {
            return null;
        }
    }

    static void verifyModifiableNotRequested(int flags, String repositoryType) throws ProvisionException {
        if ((flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) != 0) {
            Status errorStatus = new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_READ_ONLY,
                    "Cannot create writable repositories of type " + repositoryType, null);
            throw new ProvisionException(errorStatus);
        }
    }

    static ProvisionException unsupportedCreation(String repositoryType) {
        Status errorStatus = new Status(IStatus.ERROR, Activator.ID, 0 /*
                                                                        * none of the defined codes
                                                                        * really fit
                                                                        */, "Cannot create repositories of type "
                + repositoryType, null);
        return new ProvisionException(errorStatus);
    }
}
