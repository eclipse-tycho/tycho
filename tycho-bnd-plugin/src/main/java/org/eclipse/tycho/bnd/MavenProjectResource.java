/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.bnd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import aQute.bnd.osgi.Resource;

class MavenProjectResource implements Resource {

	private String extra;
	private Path path;

	public MavenProjectResource(Path path) {
		this.path = path;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public InputStream openInputStream() throws Exception {
		return Files.newInputStream(path);
	}

	@Override
	public void write(OutputStream out) throws Exception {
		try (InputStream stream = openInputStream()) {
			stream.transferTo(out);
		}
	}

	@Override
	public long lastModified() {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return -1;
		}
	}

	@Override
	public void setExtra(String extra) {
		this.extra = extra;
	}

	@Override
	public String getExtra() {
		return extra;
	}

	@Override
	public long size() throws Exception {
		return Files.size(path);
	}

	@Override
	public synchronized ByteBuffer buffer() throws Exception {
		try (InputStream stream = openInputStream()) {
			return ByteBuffer.wrap(stream.readAllBytes());
		}
	}

}
