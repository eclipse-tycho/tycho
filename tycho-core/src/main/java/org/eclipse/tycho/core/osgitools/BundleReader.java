/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;

/**
 * Cache for OSGi manifest files and bundle classpath entries.
 */
public interface BundleReader {

    /**
     * Load and cache OSGi manifest from path META-INF/MANIFEST.MF under the given location. If no
     * META-INF/MANIFEST.MF is found but plugin.xml or fragment.xml is found, an attempt is made to
     * convert it into an OSGi MANIFEST.
     * 
     * @param bundleLocation
     *            can be either a directory or a jar file
     * @return the OSGi MANIFEST, never <code>null</code>
     * @throws OsgiManifestParserException
     *             if no valid MANIFEST is found in bundleLocation or it cannot be converted from
     *             plugin.xml/fragment.xml.
     * @throws InvalidOSGiManifestException
     *             if valid MANIFEST is found but it does not have valid mandatory OSGi headers
     */
    public OsgiManifest loadManifest(File bundleLocation) throws OsgiManifestParserException,
            InvalidOSGiManifestException;

    /**
     * Returns bundle entry with given path or <code>null</code> if no such entry exists. If bundle
     * is a jar, the entry will be extracted into a cached location.
     * 
     * @param bundleLocation
     *            can be either a directory or a jar file
     * @param path
     *            path relative to the bundle root. Paths starting with "external:" are ignored
     * 
     */
    public File getEntry(File bundleLocation, String path);
}
