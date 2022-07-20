/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.embedder;

import java.io.File;

public interface FrameworkRuntimeDescription {
    public void addInstallation(File location);

    public void addBundle(File location);

    /**
     * Packages exported by embedding application. This allows embedded runtime import API classes
     * from embedding application with Import-Package.
     * 
     * @return Packages exported by embedding application; never <code>null</code>
     */
    public void addExtraSystemPackage(String systemPackages);

    public void addPlatformProperty(String property, String value);

    public void addBundleStartLevel(String id, int level, boolean autostart);
}
