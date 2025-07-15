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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
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

    private static final SwtClassLoader SWT_CLASS_LOADER = new SwtClassLoader();

    private Map<String, ConnectModule> modules = new ConcurrentHashMap<>();

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

    public String newBundle(Class<?> clazz, String[] requireBundles) {
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
        if (requireBundles != null && requireBundles.length > 0) {
            header.put(Constants.REQUIRE_BUNDLE, Arrays.stream(requireBundles).collect(Collectors.joining(",")));
        }
        modules.put(id, new TempBundle(new File(location), header));
        return id;
    }

    public String newFragment(Class<?> clazz, Bundle bundle) {
        URI location = getLocationFromClass(clazz);
        if (location == null) {
            throw new RuntimeException("can't get location of class " + clazz);
        }
        String id = "eclipse-fragment" + UUID.randomUUID().toString();
        Map<String, String> header = new HashMap<>();
        header.put(Constants.BUNDLE_NAME, clazz.getName());
        header.put(Constants.BUNDLE_SYMBOLICNAME, id);
        header.put(Constants.BUNDLE_VERSION, "1.0.0");
        header.put(Constants.FRAGMENT_HOST, bundle.getSymbolicName());
        modules.put(id, new TempBundle(new File(location), header));
        return id;
    }

    public void release(String id) {
        modules.remove(id);
    }

    public String loadSWT(Path bundleFile) {
        if (bundleFile.getFileName().toString().contains("org.eclipse.swt")) {
            return loadGlobalSWT(bundleFile);
        }
        return null;
    }

    private String loadGlobalSWT(Path bundleFile) {
        try (JarFile jarFile = new JarFile(bundleFile.toFile())) {
            Attributes attributes = jarFile.getManifest().getMainAttributes();
            String bsn = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME).split(";")[0];
            if ("org.eclipse.swt".equals(bsn)) {
                return createSingleSwtBundleLoader(bundleFile, bsn);
            } else {
                String value = attributes.getValue(Constants.FRAGMENT_HOST);
                if (value != null) {
                    String host = value.split(";")[0];
                    if ("org.eclipse.swt".equals(host)) {
                        return createSingleSwtBundleLoader(bundleFile, bsn);
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String createSingleSwtBundleLoader(Path bundleFile, String bsn) {
        //TODO actually we would need one bundle per SWT version that has an own binary... but for this we would need the fragment names now...
        //even better we would simply replace them by a headless fragment see
        // https://github.com/eclipse-platform/eclipse.platform.swt/issues/1750
        modules.put(bsn, new SwtBundle(bundleFile));
        return bsn;
    }

    static URI getLocationFromClass(Class<?> clazz) {
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

    private static final class SwtBundle extends ZipBundle {

        public SwtBundle(Path location) {
            super(location.toFile());
            SWT_CLASS_LOADER.addURL(location);
        }

        @Override
        public Optional<Map<String, String>> getHeaders() {
            return Optional.empty();
        }

        @Override
        public Optional<ClassLoader> getClassLoader() {
            return Optional.of(SWT_CLASS_LOADER);
        }

    }

    private static final class SwtClassLoader extends URLClassLoader {

        private Set<String> added = new HashSet<>();

        public SwtClassLoader() {
            super(new URL[] {}, null);
        }

        public synchronized void addURL(Path path) {
            if (added.add(path.toAbsolutePath().toString())) {
                try {
                    super.addURL(path.toFile().toURI().toURL());
                } catch (MalformedURLException e) {
                    System.err.println("Can't add path " + path + ": " + e);
                }
            }
        }

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
