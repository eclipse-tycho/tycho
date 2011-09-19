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
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Properties;
import java.util.jar.Manifest;

import org.eclipse.osgi.util.ManifestElement;

// TODO cleanup and rework to consistently use equinox implementation
public interface BundleReader {
    Manifest loadManifest(File bundleLocation);

    Properties toProperties(Manifest mf);

    /**
     * returns null if header is not present in the manifest
     */
    ManifestElement[] parseHeader(String header, Manifest mf);

    /**
     * Returns true if Eclipse-BundleShape header is set to dir.
     * 
     * http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/
     * bundle_manifest.html
     * 
     * http://eclipsesource.com/blogs/2009/01/20/tip-eclipse-bundleshape/
     * 
     * TODO this method does not belong here
     */
    boolean isDirectoryShape(Manifest mf);

    /**
     * Returns bundle entry with given path or null if no such entry exists. If bundle is a jar, the
     * entry will be extracted into a cached location.
     */
    File getEntry(File bundleLocation, String path);
}
