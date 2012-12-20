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
package org.eclipse.tycho.repository.general;

import org.eclipse.equinox.p2.core.ProvisionException;

public class ArtifactProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ArtifactProviderException(ProvisionException e) {
        super(e.getMessage(), e);
    }

}
