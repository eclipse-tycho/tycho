/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;

import org.eclipse.tycho.core.maven.AbstractP2Mojo;

public abstract class AbstractRepositoryMojo extends AbstractP2Mojo {

    protected File getAssemblyRepositoryLocation() {
        return getBuildDirectory().getChild("repository");
    }

}
