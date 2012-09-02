/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.pack200;

import java.io.IOException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * http://wiki.eclipse.org/JarProcessor_Options
 */
public class EclipseInf {

    public static final String PATH_ECLIPSEINF = "META-INF/eclipse.inf";

    public static void assertNotPresent(JarFile jarFile) throws IOException {
        ZipEntry entry = jarFile.getEntry(PATH_ECLIPSEINF);
        if (entry != null) {
            throw new IOException("tycho pack200 does not support " + PATH_ECLIPSEINF + " due to bug 387557");
        }
    }
}
