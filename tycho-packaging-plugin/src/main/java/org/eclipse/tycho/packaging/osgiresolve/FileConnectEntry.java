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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.osgi.framework.connect.ConnectContent.ConnectEntry;

class FileConnectEntry implements ConnectEntry {

	private File file;

	FileConnectEntry(File file) {
		this.file = file;
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public long getContentLength() {
		return file.length();
	}

	@Override
	public long getLastModified() {
		return file.lastModified();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(file);
	}

}
