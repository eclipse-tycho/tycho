/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.io.File;

import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;

public interface PublisherServiceFactory {
    /**
     * Creates a {@link PublisherService} instance that can be used to publish artifacts. The
     * results are stored as metadata and artifacts repository at the given location.
     * <p>
     * Note: Only one <code>PublisherService</code> instance shall be active at a time for each
     * project, so that certain sub-folders of the target folder can be used as storage location of
     * an unsynchronised cache.
     * </p>
     * 
     * @param targetRepository
     *            The location of the output repository; if the output repository exists, new
     *            content will be appended
     * @param contextRepositories
     *            Context metadata repositories that may be consulted by the publishers; note that
     *            artifact repository references in the argument are ignored
     * @param context
     *            Context information about the current project
     * @return A new {@link PublisherService} instance. The caller is responsible to call
     *         <tt>stop</tt> on the instance after use
     * @throws FacadeException
     *             if a checked exception occurs internally
     */
    PublisherService createPublisher(File targetRepository, RepositoryReferences contextRepositories,
            BuildContext context) throws FacadeException;
}
