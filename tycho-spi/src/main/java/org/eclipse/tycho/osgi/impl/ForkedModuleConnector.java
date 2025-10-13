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
package org.eclipse.tycho.osgi.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectContent.ConnectEntry;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;

/**
 * This module connector currently do nothing else as replicate a default OSGi-Framework behavior
 */
class ForkedModuleConnector implements ModuleConnector {

    private Map<String, ConnectModule> modules = new ConcurrentHashMap<>();
    private Map<String, Bundle> bundles = new ConcurrentHashMap<>();

    public ForkedModuleConnector() {
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

    public Bundle getBundle(String jarName, Framework framework) throws BundleException {
        String id = "fork-" + jarName;
        Bundle bundle = bundles.get(id);
        if (bundle != null) {
            return bundle;
        }
        Map<String, String> header = new HashMap<>();
        header.put(Constants.BUNDLE_SYMBOLICNAME, id);
        header.put(Constants.BUNDLE_VERSION, "1.0.0");
        header.put(Constants.DYNAMICIMPORT_PACKAGE, "*");
        modules.put(id, new TempBundle(new File(jarName), header));
        Bundle installBundle = framework.getBundleContext().installBundle(id);
        bundles.put(id, installBundle);
        installBundle.start();
        return installBundle;
    }

    private static final class TempBundle extends ZipBundle {

        private Map<String, String> header;

        public TempBundle(File location, Map<String, String> header) {
            super(location);
            this.header = header;
        }

        @Override
        public Optional<Map<String, String>> getHeaders() {
            return Optional.of(header);
        }

        @Override
        public Optional<ClassLoader> getClassLoader() {
            return Optional.empty();
        }

    }

    private static abstract class ZipBundle implements ConnectModule, ConnectContent {

        private JarFile jarFile;
        private final File location;

        public ZipBundle(File location) {
            this.location = location;
        }

        @Override
        public ConnectContent getContent() throws IOException {
            return this;
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
