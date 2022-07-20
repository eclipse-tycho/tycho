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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.jar.Attributes;
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
	private ClassLoader classLoader;
	private Optional<Map<String, String>> header;
	private JarFile jarFile;
	private String location;

	public PlexusConnectContent(JarFile jarFile, ClassLoader classLoader) throws IOException {
		this.jarFile = jarFile;
		this.location = jarFile.getName();
		this.classLoader = classLoader;
		Attributes attributes = jarFile.getManifest().getMainAttributes();
		Map<String, String> headers = new LinkedHashMap<String, String>();
		for (Entry<Object, Object> entry : attributes.entrySet()) {
			headers.put(entry.getKey().toString(), entry.getValue().toString());
		}
		this.header = Optional.of(Collections.unmodifiableMap(headers));
	}

	@Override
	public Optional<ClassLoader> getClassLoader() {
		return Optional.of(classLoader);
	}

	@Override
	public Iterable<String> getEntries() throws IOException {
		return jarFile.stream().map(JarEntry::getName).collect(Collectors.toList());
	}

	@Override
	public Optional<ConnectEntry> getEntry(String path) {
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
		if (jarFile == null) {
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