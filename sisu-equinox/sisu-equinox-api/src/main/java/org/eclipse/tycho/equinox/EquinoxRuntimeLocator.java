/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.equinox;

import java.io.File;
import java.util.List;

public interface EquinoxRuntimeLocator {
    // TODO do we need more specific exception type here?
    public List<File> getRuntimeLocations() throws Exception;

    /**
     * Packages exported by embedding application. This allows embedded runtime import API classes
     * from embedding application with Import-Package.
     * 
     * @return Packages exported by embedding application; never <code>null</code>
     */
    public List<String> getSystemPackagesExtra();
}
