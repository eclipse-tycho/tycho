/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.facade;

import java.io.File;

/**
 * Makes maven information which is constant for the whole maven session available as a declarative
 * service to the embedded OSGi runtime.
 */
public interface MavenContext {

    public File getLocalRepositoryRoot();

    public MavenLogger getLogger();

    public boolean isOffline();

}
