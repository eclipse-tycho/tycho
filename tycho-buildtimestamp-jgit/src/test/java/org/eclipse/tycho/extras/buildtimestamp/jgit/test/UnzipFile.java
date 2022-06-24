/*******************************************************************************
 * Copyright (c) 2012, 2022 Sonatype Inc. and others.
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
package org.eclipse.tycho.extras.buildtimestamp.jgit.test;

import java.io.File;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

/**
 * Helper class used from prebuild.bsh
 */
public class UnzipFile {
	public static void unzip(File src, File target) throws ArchiverException {
		ZipUnArchiver unzip = new ZipUnArchiver(src);
		unzip.setDestDirectory(target);
		unzip.extract();
	}
}
