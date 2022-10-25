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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.osgi.framework.Version;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectModule;

public class TestModule implements ConnectContent, ConnectModule {

	private Map<String, String>	headers;
	private File				location;
	private JarFile				jarFile;
	private String				name;
	private Version version;

	public TestModule(String name, Version version, Map<String, String> headers, File location) {
		this.name = name;
		this.version = version;
		this.headers = headers;
		this.location = location;
	}

	@Override
	public ConnectContent getContent() throws IOException {
		return this;
	}

	@Override
	public Optional<Map<String, String>> getHeaders() {
		return Optional.of(headers);
	}

	@Override
	public Iterable<String> getEntries() throws IOException {
		if (jarFile != null) {
			return jarFile.stream().map(JarEntry::getName).toList();
		}
		if (location != null && location.isDirectory()) {
			try (Stream<String> stream = Files.walk(location.toPath()).map(Path::toString)) {
				return stream.toList();
			}
		}
		return Collections.emptyList();
	}

	@Override
	public Optional<ConnectEntry> getEntry(String path) {
		if (jarFile != null) {
			final ZipEntry entry = jarFile.getEntry(path);
			if (entry == null) {
				return Optional.empty();
			}
			return Optional.of(new ZipConnectEntry(jarFile, entry));
		}
		if (location != null && location.isDirectory()) {
			File file = new File(location, path);
			if (file.isFile()) {
				return Optional.of(new FileConnectEntry(file));
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<ClassLoader> getClassLoader() {
		return Optional.empty();
	}

	@Override
	public void open() throws IOException {
		if (location != null && jarFile == null && location.isFile()) {
			jarFile = new JarFile(location);
		}
	}

	@Override
	public void close() throws IOException {
		if (jarFile != null) {
			jarFile.close();
		}
	}

	public String getName() {
		return name;
	}

	public Version getVersion() {
		return version;
	}

	public File getLocation() {
		return location;
	}

	public String getId() {
		return name + ":" + version;
	}

}
