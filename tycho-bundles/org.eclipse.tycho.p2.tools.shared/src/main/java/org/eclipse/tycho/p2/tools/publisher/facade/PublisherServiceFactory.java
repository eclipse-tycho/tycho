/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher.facade;

import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;

public interface PublisherServiceFactory {
    /**
     * Creates a {@link PublisherService} instance that can be used to publish artifacts. The
     * results are stored in the build output p2 repository of the given module (identified through
     * the <tt>BuildContext</tt> parameter).
     * 
     * @param contextRepositories
     *            Context metadata repositories that may be consulted by the publishers; note that
     *            artifact repository references in the argument are ignored
     * @param buildContext
     *            Build information of the current project, e.g. the build output directory.
     * @return A new {@link PublisherService} instance.
     * @throws FacadeException
     *             if a checked exception occurs internally
     */
    PublisherService createPublisher(RepositoryReferences contextRepositories, BuildContext buildContext)
            throws FacadeException;
}
