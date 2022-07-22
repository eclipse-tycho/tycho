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
package org.eclipse.sisu.osgi.connect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectModule;

/**
 * Implements the {@link ConnectContent} on top of a jar file and a classloader
 *
 */
class PlexusConnectContent implements ConnectContent, ConnectModule {
	private final ClassLoader classLoader;
	private final Optional<Map<String, String>> header;
	private final String location;
	private JarFile jarFile;

	public PlexusConnectContent(JarFile jarFile, Map<String, String> header, ClassLoader classLoader) {
		this.jarFile = jarFile;
		this.location = jarFile == null ? null : jarFile.getName();
		this.classLoader = classLoader;
		this.header = Optional.of(header);
	}

	@Override
	public Optional<ClassLoader> getClassLoader() {
		return Optional.of(classLoader);
	}

	@Override
	public Iterable<String> getEntries() throws IOException {
		if (jarFile == null) {
			return Collections.emptyList();
		}
		return jarFile.stream().map(JarEntry::getName).collect(Collectors.toList());
	}

	@Override
	public Optional<ConnectEntry> getEntry(String path) {
		if (jarFile == null) {
			return Optional.empty();
		}
		final ZipEntry entry = jarFile.getEntry(path);
		if (entry == null) {
			return Optional.empty();
		}
		return Optional.of(new ZipConnectEntry(jarFile, entry));
	}

	@Override
	public Optional<Map<String, String>> getHeaders() {
		return header;
	}

	@Override
	public void open() throws IOException {
		if (jarFile == null && location != null) {
			jarFile = new JarFile(location);
		}
	}

	@Override
	public void close() throws IOException {
		if (jarFile != null) {
			try {
				jarFile.close();
			} finally {
				jarFile = null;
			}
		}
	}

	private static final class ZipConnectEntry implements ConnectEntry {

		private ZipEntry entry;
		private JarFile jarFile;

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

	@Override
	public ConnectContent getContent() throws IOException {
		return this;
	}

}