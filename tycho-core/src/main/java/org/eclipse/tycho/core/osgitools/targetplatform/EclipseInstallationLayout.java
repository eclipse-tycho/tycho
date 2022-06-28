/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * Finds bundles in Eclipse installation.
 * 
 * See https://wiki.eclipse.org/Equinox_p2_Getting_Started See
 * https://mea-bloga.blogspot.com/2008/04/new-target-platform-preference.html
 * 
 * @author igor
 * @deprecated only required for {@link LocalDependencyResolver}
 */

@Component(role = EclipseInstallationLayout.class, instantiationStrategy = "per-lookup")
@Deprecated
public class EclipseInstallationLayout extends AbstractLogEnabled {

    private static final class FEATURE_FILTER implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            if (pathname.getName().startsWith(".")) {
                // filter out all dot files (like .svn or .DS_Store).
                return false;
            }
            return pathname.isDirectory() || pathname.getName().endsWith(".jar");
        }
    }

    public static final String PLUGINS = "plugins";
    public static final String FEATURES = "features";

    private File location;
    private File dropinsLocation;

    public void setLocation(File location) {
        this.location = location;
        this.dropinsLocation = new File(location, "dropins");
    }

    public File getLocation() {
        return location;
    }

    public Set<File> getFeatures(File site) {
        Set<File> result = new LinkedHashSet<>();
        File[] features = new File(site, FEATURES).listFiles(new FEATURE_FILTER());
        if (features != null) {
            result.addAll(Arrays.asList(features));
        }

        return result;
    }

    public Set<File> getInstalledPlugins() {
        Set<File> result = new LinkedHashSet<>();
        try {
            result.addAll(readBundlesTxt(location));
        } catch (IOException e) {
            getLogger().warn("Exception reading P2 bundles list", e);
        }
        return result;
    }

    public Set<File> getPlugins(File site) {
        Set<File> result = new LinkedHashSet<>();

        addPlugins(result, new File(site, PLUGINS).listFiles());

        // check for bundles in the root of dropins directory
        if (dropinsLocation.equals(site)) {
            addPlugins(result, site.listFiles());
        }

        return result;
    }

    private void addPlugins(Set<File> result, File[] plugins) {
        if (plugins != null) {
            for (File plugin : plugins) {
                if (plugin.isDirectory() && isDirectoryPlugin(plugin)) {
                    result.add(plugin);
                } else if (plugin.isFile() && plugin.getName().endsWith(".jar")) {
                    result.add(plugin);
                }
            }
        }
    }

    private boolean isDirectoryPlugin(File plugin) {
        return new File(plugin, "META-INF/MANIFEST.MF").canRead();
    }

    public Set<File> getSites() {
        Set<File> result = new LinkedHashSet<>();

        if (location == null) {
            return result;
        }

        if (new File(location, PLUGINS).isDirectory()) {
            result.add(location);
        }

        File platform = new File(location, "configuration/org.eclipse.update/platform.xml");
        if (platform.canRead()) {
            try {
                try (FileInputStream is = new FileInputStream(platform)) {
                    XmlStreamReader reader = new XmlStreamReader(is);
                    Xpp3Dom dom = Xpp3DomBuilder.build(reader);
                    Xpp3Dom[] sites = dom.getChildren("site");
                    for (Xpp3Dom site : sites) {
                        String enabled = site.getAttribute("enabled");
                        if (enabled == null || Boolean.parseBoolean(enabled)) {
                            File dir = parsePlatformURL(location, site.getAttribute("url"));
                            if (dir != null) {
                                result.add(dir);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                getLogger().warn("Exception parsing " + toString(platform), e);
            }
        }

        addLinks(result, location, new File(location, "links"));

        // deal with dropins folder
        result.add(dropinsLocation);

        File[] dropinsFiles = dropinsLocation.listFiles();
        if (dropinsFiles != null) {
            for (File dropinsFile : dropinsFiles) {
                File plugins = new File(dropinsFile, PLUGINS);
                if (plugins.isDirectory()) {
                    result.add(plugins.getParentFile());
                    continue;
                }
                plugins = new File(dropinsFile, "eclipse/plugins");
                if (plugins.isDirectory()) {
                    result.add(plugins.getParentFile());
                }
            }
        }

        addLinks(result, location, dropinsLocation);

        return result;
    }

    private String toString(File file) {
        return file.getAbsolutePath();
    }

    private void addLinks(Set<File> result, File targetPlatform, File linksFolder) {
        File[] links = linksFolder.listFiles();
        if (links != null) {
            for (File link : links) {
                if (link.isFile() && link.canRead() && link.getName().endsWith(".link")) {
                    Properties props = new Properties();
                    try {
                        try (InputStream is = new FileInputStream(link)) {
                            props.load(is);
                        }
                        String path = props.getProperty("path");
                        if (path != null) {
                            File dir = new File(path);
                            if (!dir.isAbsolute() && targetPlatform.getParentFile() != null) {
                                dir = new File(targetPlatform.getParentFile(), path);
                            }
                            dir = dir.getAbsoluteFile();
                            if (dir.isDirectory()) {
                                result.add(dir);
                            }
                        }
                    } catch (Exception e) {
                        getLogger().warn("Exception parsing " + toString(link), e);
                        continue;
                    }
                }
            }
        }
    }

    private static final String PLATFORM_BASE_PREFIX = "platform:/base/";
    private static final String FILE_PREFIX = "file:";

    private File parsePlatformURL(File platformBase, String url) {
        if (url == null) {
            return null;
        }

        url = url.replace('\\', '/');

        String relPath = null;
        if (url.startsWith(PLATFORM_BASE_PREFIX)) {
            relPath = url.substring(PLATFORM_BASE_PREFIX.length());
        } else if (url.startsWith(FILE_PREFIX)) {
            relPath = url.substring(FILE_PREFIX.length());
        }

        if (relPath == null) {
            return null;
        }

        if (!relPath.isEmpty() && relPath.charAt(0) == '/') {
            return new File(relPath);
        }

        return new File(platformBase, relPath);
    }

    private List<File> readBundlesTxt(File platformBase) throws IOException {
        getLogger().debug("Reading P2 bundles list");

        // there is no way to find location of bundle pool without access to P2 profile
        // so lets assume equinox.launcher comes from the pool
        File eclipseIni = new File(platformBase, "eclipse.ini");
        File pool = platformBase;
        if (eclipseIni.isFile() && eclipseIni.canRead()) {
            try (BufferedReader in = new BufferedReader(new FileReader(eclipseIni))) {
                String str = null;
                while ((str = in.readLine()) != null) {
                    if ("-startup".equals(str.trim())) {
                        str = in.readLine();
                        if (str != null) {
                            File file = new File(str);
                            if (!file.isAbsolute()) {
                                file = new File(platformBase, str).getAbsoluteFile();
                            }
                            pool = file.getParentFile().getParentFile().getAbsoluteFile();
                        }
                        break;
                    }
                }
            }
        }

        getLogger().debug("Bundle pool location " + toString(pool));

        File bundlesInfo = new File(platformBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
        if (!bundlesInfo.isFile() || !bundlesInfo.canRead()) {
            getLogger().info("Could not read P2 bundles list " + toString(bundlesInfo));
            return null;
        }

        ArrayList<File> plugins = new ArrayList<>();

        String line;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(bundlesInfo)))) {
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#")) //$NON-NLS-1$
                    continue;
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // (expectedState is an integer).
                if (line.startsWith("org.eclipse.equinox.simpleconfigurator.baseUrl" + "=")) { //$NON-NLS-1$ //$NON-NLS-2$
                    continue;
                }

                StringTokenizer tok = new StringTokenizer(line, ",");
                /* String symbolicName = */tok.nextToken();
                /* String version = */tok.nextToken();
                String location = tok.nextToken();

                plugins.add(parsePlatformURL(pool, location));
            }
        }

        return plugins;
    }

}
