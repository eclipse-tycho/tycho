/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
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
package org.eclipse.tycho.core;

import java.net.URI;

import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.p2.tools.FacadeException;

public interface VerifierService {

    public abstract boolean verify(URI metadataRepositoryUri, URI artifactRepositoryUri, BuildDirectory tempDirectory)
            throws FacadeException;

}
