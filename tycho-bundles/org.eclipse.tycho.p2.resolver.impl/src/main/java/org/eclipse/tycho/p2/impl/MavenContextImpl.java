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
package org.eclipse.tycho.p2.impl;

import java.io.File;

import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.internal.MutableMavenContext;

public class MavenContextImpl implements MutableMavenContext {

    private File localRepositoryRoot;
    private MavenLogger mavenLogger;
    private boolean offline;

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public void setLocalRepositoryRoot(File localRepositoryRoot) {
        this.localRepositoryRoot = localRepositoryRoot;
    }

    public void setLogger(MavenLogger mavenLogger) {
        this.mavenLogger = mavenLogger;
    }

    public File getLocalRepositoryRoot() {
        return localRepositoryRoot;
    }

    public MavenLogger getLogger() {
        return mavenLogger;
    }

    public boolean isOffline() {
        return offline;
    }

}
