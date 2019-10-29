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
package org.eclipse.tycho.extras.buildtimestamp.jgit.test;

import java.io.File;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * Helper class used from prebuild.bsh
 */
public class UnzipFile {
    public static void unzip(File src, File target) throws ArchiverException {
        ZipUnArchiver unzip = new ZipUnArchiver(src);
        unzip.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, "unzip"));
        unzip.setDestDirectory(target);
        unzip.extract();
    }
}
