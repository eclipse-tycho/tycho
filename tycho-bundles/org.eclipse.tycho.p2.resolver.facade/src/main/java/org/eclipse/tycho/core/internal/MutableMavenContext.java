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
package org.eclipse.tycho.core.internal;

import java.io.File;

import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;

/**
 * Not intended to be used by clients.
 */
public interface MutableMavenContext extends MavenContext {

    public void setLocalRepositoryRoot(File root);

    public void setOffline(boolean offline);

    public void setLogger(MavenLogger logger);

}
