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
package org.eclipse.tycho.osgi.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectContent.ConnectEntry;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;

import aQute.bnd.osgi.Constants;

/**
 * This module connector currently do nothing else as replicate a default OSGi-Framework behavior
 */
class EclipseModuleConnector implements ModuleConnector {

    private final Map<String, ConnectModule> modules = new ConcurrentHashMap<>();

    public EclipseModuleConnector() {
    }

    @Override
    public void initialize(File storage, Map<String, String> configuration) {

    }

    @Override
    public Optional<ConnectModule> connect(String location) throws BundleException {
        return Optional.ofNullable(modules.get(location));
    }

    @Override
    public Optional<BundleActivator> newBundleActivator() {
        return Optional.empty();
    }

    public String newBundle(Class<?> clazz) {
        URI location = getLocationFromClass(clazz);
        if (location == null) {
            throw new RuntimeException("can't get location of class " + clazz);
        }
        String id = "eclipse-" + UUID.randomUUID().toString();
        Map<String, String> header = new HashMap<>();
        header.put(Constants.BUNDLE_NAME, clazz.getName());
        header.put(Constants.BUNDLE_SYMBOLICNAME, id);
        header.put(Constants.BUNDLE_VERSION, "1.0.0");
        header.put(Constants.DYNAMICIMPORT_PACKAGE, "*");
        modules.put(id, new TempBundle(new File(location), header));
        return id;
    }

    public void release(String id) {
        modules.remove(id);
    }

    private static URI getLocationFromClass(Class<?> clazz) {
        ProtectionDomain domain = clazz.getProtectionDomain();
        if (domain == null) {
            return null;
        }
        CodeSource codeSource = domain.getCodeSource();
        if (codeSource == null) {
            return null;
        }
        URL url = codeSource.getLocation();
        if (url == null) {
            return null;
        }
        try {
            return url.toURI().normalize();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static final class TempBundle implements ConnectModule, ConnectContent {

        private JarFile jarFile;
        private final File location;
        private Map<String, String> header;

        public TempBundle(File location, Map<String, String> header) {
            this.location = location;
            this.header = header;
        }

        @Override
        public ConnectContent getContent() throws IOException {
            return this;
        }

        @Override
        public Optional<Map<String, String>> getHeaders() {
            return Optional.of(header);
        }

        @Override
        public Optional<ClassLoader> getClassLoader() {
            return Optional.empty();
        }

        @Override
        public Iterable<String> getEntries() throws IOException {
            if (jarFile == null) {
                return Collections.emptyList();
            }
            return jarFile.stream().map(JarEntry::getName).toList();
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

}
