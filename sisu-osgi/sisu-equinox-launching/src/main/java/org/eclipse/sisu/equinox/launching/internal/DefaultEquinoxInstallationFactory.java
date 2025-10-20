/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
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
package org.eclipse.sisu.equinox.launching.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.sisu.equinox.launching.BundleReference;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationFactory;
import org.eclipse.tycho.ReproducibleUtils;
import org.eclipse.tycho.TychoConstants;
import org.osgi.framework.Constants;

@Named
@Singleton
public class DefaultEquinoxInstallationFactory implements EquinoxInstallationFactory {
    @Inject
    private PlexusContainer plexus;

    private final Map<String, Manifest> manifestCache = new HashMap<>();

    @Inject
    private Logger log;

    public DefaultEquinoxInstallationFactory() {
        // for plexus
    }

    DefaultEquinoxInstallationFactory(Logger log) {
        this.log = log;
    }

    @Override
    public EquinoxInstallation createInstallation(EquinoxInstallationDescription description, File location) {
        Set<String> bundlesToExplode = description.getBundlesToExplode();
        Collection<File> frameworkExtensions = description.getFrameworkExtensions();
        Map<String, BundleStartLevel> startLevel = description.getBundleStartLevel();
        BundleStartLevel defaultBundleStartLevel = description.getDefaultBundleStartLevel();
        if (defaultBundleStartLevel == null) {
            defaultBundleStartLevel = new BundleStartLevel(null, 4, false);
        }

        try {

            Map<BundleReference, File> effective = new LinkedHashMap<>();

            for (BundleReference artifact : description.getBundles()) {
                File file = artifact.getLocation();
                if (needsUnpack(artifact, bundlesToExplode)) {
                    String filename = artifact.getId() + "_" + artifact.getVersion();
                    File unpacked = new File(location, "plugins/" + filename);

                    unpacked.mkdirs();

                    unpack(file, unpacked);

                    effective.put(artifact, unpacked);
                } else {
                    effective.put(artifact, file);
                }
            }
            location.mkdirs();

            Properties p = new Properties();

            p.putAll(description.getPlatformProperties());

            String newOsgiBundles = toOsgiBundles(effective, startLevel, defaultBundleStartLevel);

            p.setProperty("osgi.bundles", newOsgiBundles);

            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=234069
            p.setProperty("osgi.bundlefile.limit", "100");

            // @see SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION
            // p.setProperty("org.eclipse.equinox.simpleconfigurator.exclusiveInstallation", "false");

            p.setProperty("osgi.install.area", "file:" + location.getAbsolutePath().replace('\\', '/'));
            p.setProperty("osgi.configuration.cascaded", "false");
            p.setProperty("osgi.framework", "org.eclipse.osgi");
            p.setProperty("osgi.bundles.defaultStartLevel", String.valueOf(defaultBundleStartLevel.getLevel()));

            // fix osgi.framework
            String url = p.getProperty("osgi.framework");
            if (url != null) {
                File file;
                BundleReference desc = description.getBundle(url, null);
                if (desc != null) {
                    url = "file:" + desc.getLocation().getAbsolutePath().replace('\\', '/');
                } else if (url.startsWith("file:")) {
                    String path = url.substring("file:".length());
                    file = new File(path);
                    if (!file.isAbsolute()) {
                        file = new File(location, path);
                    }
                    url = "file:" + file.getAbsolutePath().replace('\\', '/');
                }
            }
            if (url != null) {
                p.setProperty("osgi.framework", url);
            }

            if (!frameworkExtensions.isEmpty()) {
                // see osgi.framework.extensions at https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Fruntime-options.html
                Collection<String> bundleNames = unpackFrameworkExtensions(location, frameworkExtensions);
                p.setProperty("osgi.framework", copySystemBundle(description, location));
                p.setProperty("osgi.framework.extensions", StringUtils.join(bundleNames.iterator(), ","));
            }

            if (!description.getDevEntries().isEmpty()) {
                p.put("osgi.dev", createDevProperties(location, description.getDevEntries()));
            }

            File configIni = new File(location, TychoConstants.CONFIG_INI_PATH);
            ReproducibleUtils.storeProperties(p, configIni.toPath());
            File configurationLocation = configIni.getParentFile();
            return new DefaultEquinoxInstallation(description, location, configurationLocation);
        } catch (IOException e) {
            throw new RuntimeException("Exception creating test eclipse runtime", e);
        }
    }

    private boolean needsUnpack(BundleReference artifact, Set<String> bundlesToExplode) throws IOException {
        File file = artifact.getLocation();
        if (file.isDirectory()) {
            //directory items needs never being unpacked...
            return false;
        }
        if (bundlesToExplode.contains(artifact.getId())) {
            return true;
        }
        Manifest manifest = getManifest(file);
        return isDirectoryShape(manifest);
    }

    private boolean isDirectoryShape(Manifest mf) {

        String value = mf.getMainAttributes().getValue("Eclipse-BundleShape");
        return "dir".equals(value);
    }

    private Manifest getManifest(File file) throws IOException {
        String key = file.getAbsolutePath();
        Manifest manifest = manifestCache.get(key);
        if (manifest != null) {
            return manifest;
        }
        if (file.isDirectory()) {
            File manifestFile = new File(file, JarFile.MANIFEST_NAME);
            try (FileInputStream stream = new FileInputStream(manifestFile)) {
                manifest = new Manifest(stream);
            }
        } else {
            try (JarFile jarFile = new JarFile(file)) {
                manifest = jarFile.getManifest();
            }
        }
        manifestCache.put(key, manifest);
        return manifest;
    }

    /**
     * See
     *
     * <pre>
     * https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html#osgidev
     * </pre>
     */
    private String createDevProperties(File location, Map<String, String> devEntries) throws IOException {
        File file = new File(location, "dev.properties");
        Properties properties = new Properties();
        properties.putAll(devEntries);
        ReproducibleUtils.storeProperties(properties, file.toPath());
        return file.toURI().toURL().toExternalForm();
    }

    protected void unpack(File source, File destination) {
        UnArchiver unzip;
        try {
            unzip = plexus.lookup(UnArchiver.class, "zip");
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Could not lookup required component", e);
        }
        unzip.setIgnorePermissions(true);
        destination.mkdirs();
        unzip.setSourceFile(source);
        unzip.setDestDirectory(destination);
        try {
            unzip.extract();
        } catch (ArchiverException e) {
            throw new RuntimeException("Unable to unpack jar " + source, e);
        }
    }

    private List<String> unpackFrameworkExtensions(File location, Collection<File> frameworkExtensions)
            throws IOException {
        List<String> bundleNames = new ArrayList<>();

        for (File bundleFile : frameworkExtensions) {
            Manifest mf = getManifest(bundleFile);
            String symbolicName = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
            String version = mf.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            bundleNames.add(symbolicName);
            File bundleDir = new File(location, "plugins/" + symbolicName + "_" + version);
            if (bundleFile.isFile()) {
                unpack(bundleFile, bundleDir);
            } else {
                FileUtils.copyDirectoryStructure(bundleFile, bundleDir);
            }
        }
        return bundleNames;
    }

    private String copySystemBundle(EquinoxInstallationDescription description, File location) throws IOException {
        BundleReference bundle = description.getSystemBundle();
        File srcFile = bundle.getLocation();
        File dstFile = new File(location, "plugins/" + srcFile.getName());
        FileUtils.copyFileIfModified(srcFile, dstFile);

        return "file:" + dstFile.getAbsolutePath().replace('\\', '/');
    }

    protected String toOsgiBundles(Map<BundleReference, File> effective, Map<String, BundleStartLevel> startLevel,
            BundleStartLevel defaultStartLevel) throws IOException {
        log.debug("Installation OSGI bundles:");
        StringJoiner result = new StringJoiner(",");
        for (Entry<BundleReference, File> entry : effective.entrySet()) {
            BundleStartLevel level = startLevel.get(entry.getKey().getId());
            if (level != null && level.getLevel() == -1) {
                continue; // system bundle
            }

            StringBuilder line = new StringBuilder();
            line.append(appendAbsolutePath(entry.getValue()));
            if (level != null) {
                line.append('@');
                if (level.getLevel() > 0) {
                    line.append(level.getLevel());
                }
                if (level.isAutoStart()) {
                    if (line.charAt(line.length() - 1) == '@') {
                        line.append("start");
                    } else {
                        line.append(":start");
                    }
                }
            } else {
                if (defaultStartLevel.isAutoStart()) {
                    line.append("@start");
                }
            }
            log.debug("\t" + line);
            result.add(line.toString());
        }
        return result.toString();
    }

    private String appendAbsolutePath(File file) throws IOException {
        String url = file.getAbsolutePath().replace('\\', '/');
        return "reference:file:" + url;
    }

}
