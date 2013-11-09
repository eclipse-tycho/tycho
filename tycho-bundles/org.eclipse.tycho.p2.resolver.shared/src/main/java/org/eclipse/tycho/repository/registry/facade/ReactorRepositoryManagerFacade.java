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
package org.eclipse.tycho.repository.registry.facade;

import org.eclipse.tycho.ReactorProjectIdentities;

public interface ReactorRepositoryManagerFacade {

    /**
     * Returns the project's publishing repository.
     * 
     * @param project
     *            a reference to a project in the reactor.
     */
    PublishingRepositoryFacade getPublishingRepository(ReactorProjectIdentities project);

}
