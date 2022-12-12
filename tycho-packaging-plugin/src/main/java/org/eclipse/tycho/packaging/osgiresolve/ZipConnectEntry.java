/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging.osgiresolve;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.connect.ConnectContent.ConnectEntry;

final class ZipConnectEntry implements ConnectEntry {

	private ZipEntry	entry;
	private JarFile		jarFile;

	public ZipConnectEntry(JarFile jarFile, ZipEntry entry) {
		this.jarFile = jarFile;
		this.entry = entry;
	}

	@Override
	public String getName() {
		return entry.getName();
	}

	@Override
	public long getContentLength() {
		return entry.getSize();
	}

	@Override
	public long getLastModified() {
		return entry.getTime();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return jarFile.getInputStream(entry);
	}

}
