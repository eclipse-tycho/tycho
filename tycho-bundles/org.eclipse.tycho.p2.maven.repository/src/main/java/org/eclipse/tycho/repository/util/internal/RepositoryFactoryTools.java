/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.util.internal;

import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;

public class RepositoryFactoryTools {
    /**
     * Returns the given {@link URI} as {@link File}, or <code>null</code> if the given URI does not
     * have a "file:" scheme.
     */
    public static File asFile(URI location) {
        if ("file".equals(location.getScheme())) {
            return new File(location);
        } else {
            return null;
        }
    }

    public static void verifyModifiableNotRequested(int flags, String repositoryType) throws ProvisionException {
        if ((flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) != 0) {
            Status errorStatus = new Status(IStatus.ERROR, BUNDLE_ID, ProvisionException.REPOSITORY_READ_ONLY,
                    "Cannot create writable repositories of type " + repositoryType, null);
            throw new ProvisionException(errorStatus);
        }
    }

    public static ProvisionException unsupportedCreation(String repositoryType) {
        String message = "Cannot create repositories of type " + repositoryType;
        return new ProvisionException(newUnsupportedCreationStatus(message));
    }

    public static ProvisionException unsupportedCreation(Class<?> factoryClass) {
        String message = "The factory " + factoryClass.getName() + " cannot create repositories";
        return new ProvisionException(newUnsupportedCreationStatus(message));
    }

    private static Status newUnsupportedCreationStatus(String message) {
        // none of the codes defined in ProvisionException really fit
        int errorCode = 0;
        return new Status(IStatus.ERROR, BUNDLE_ID, errorCode, message, null);
    }

    public static ProvisionException invalidCreationLocation(String repositoryType, URI location) {
        String message = "Cannot create repositories of type " + repositoryType + " at location " + location;
        int errorCode = ProvisionException.REPOSITORY_INVALID_LOCATION;
        return new ProvisionException(new Status(IStatus.ERROR, BUNDLE_ID, errorCode, message, null));
    }
}
