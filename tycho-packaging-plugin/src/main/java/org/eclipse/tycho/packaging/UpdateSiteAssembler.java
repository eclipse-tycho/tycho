/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.gzip.GZipCompressor;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.packaging.pack200.Pack200Archiver;

/**
 * Assembles standard eclipse update site directory structure on local filesystem.
 * 
 * @author igor
 */
public class UpdateSiteAssembler extends ArtifactDependencyVisitor {

    public static final String PLUGINS_DIR = "plugins/";

    public static final String FEATURES_DIR = "features/";

    private final MavenSession session;

    private final File target;

    private Map<String, String> archives;

    /**
     * If true, will copy/generate pack200 archives in additional to plugin jar files.
     */
    private boolean pack200;

    /**
     * If true, generated update site will include plugins folders for plugins with
     * PluginRef.unpack. If false, will include plugin jars regardless of PluginRef.unpack.
     */
    private boolean unpackPlugins;

    /**
     * If true, generated update site will include feature directories. If false, generated update
     * site will include feature jars.
     */
    private boolean unpackFeatures;

    public UpdateSiteAssembler(MavenSession session, File target) {
        this.session = session;
        this.target = target;
    }

    public boolean visitFeature(FeatureDescription feature) {
        File location = feature.getLocation();
        String artifactId = feature.getKey().getId();
        String version = feature.getKey().getVersion();

        ReactorProject featureProject = feature.getMavenProject();

        if (featureProject != null) {
            version = featureProject.getExpandedVersion();

            location = featureProject.getArtifact(feature.getClassifier());

            if (location == null) {
                throw new IllegalStateException(featureProject.getId()
                        + " does not provide an artifact with classifier '" + feature.getClassifier() + "'");
            }

            if (location.isDirectory()) {
                throw new IllegalStateException("Should at least run ``package'' phase");
            }
        }

        if (unpackFeatures) {
            File outputJar = getOutputFile(FEATURES_DIR, artifactId, version, null);
            if (location.isDirectory()) {
                copyDir(location, outputJar);
            } else {
                unpackJar(location, outputJar);
            }
        } else {
            File outputJar = getOutputFile(FEATURES_DIR, artifactId, version, ".jar");
            if (location.isDirectory()) {
                packDir(location, outputJar);
            } else {
                copyFile(location, outputJar);
            }
        }

        return true; // keep visiting
    }

    private File getOutputFile(String prefix, String id, String version, String extension) {
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(id);
        sb.append('_');
        sb.append(version);
        if (extension != null) {
            sb.append(extension);
        }

        return new File(target, sb.toString());
    }

    public void visitPlugin(PluginDescription plugin) {
        String bundleId = plugin.getKey().getId();
        String version = plugin.getKey().getVersion();

        String relPath = PLUGINS_DIR + bundleId + "_" + version + ".jar";
        if (archives != null && archives.containsKey(relPath)) {
            copyUrl(archives.get(relPath), new File(target, relPath));
            return;
        }

        if (plugin.getLocation() == null) {
            throw new IllegalStateException("Unresolved bundle reference " + bundleId + "_" + version);
        }

        ReactorProject bundleProject = plugin.getMavenProject();
        File location = null;
        if (bundleProject != null) {
            location = bundleProject.getArtifact(plugin.getClassifier());
            if (location == null) {
                throw new IllegalStateException(bundleProject.getId()
                        + " does not provide an artifact with classifier '" + plugin.getClassifier() + "'");
            }
            if (location.isDirectory()) {
                throw new RuntimeException("Bundle project " + bundleProject.getId()
                        + " artifact is a directory. The build should at least run ``package'' phase.");
            }
            version = bundleProject.getExpandedVersion();
        } else {
            location = plugin.getLocation();
        }

        if (unpackPlugins && isDirectoryShape(plugin, location)) {
            // need a directory
            File outputJar = getOutputFile(PLUGINS_DIR, bundleId, version, null);

            if (location.isDirectory()) {
                copyDir(location, outputJar);
            } else {
                unpackJar(location, outputJar);
            }
        } else {
            // need a jar
            File outputJar = getOutputFile(PLUGINS_DIR, bundleId, version, ".jar");

            if (location.isDirectory()) {
                packDir(location, outputJar);
            } else {
                copyFile(location, outputJar);
            }

            // if ( pack200 )
            // {
            // shipPack200( outputJar );
            // }
        }
    }

    protected boolean isDirectoryShape(PluginDescription plugin, File location) {
        PluginRef pluginRef = plugin.getPluginRef();
        return ((pluginRef != null && pluginRef.isUnpack()) || location.isDirectory());
    }

    private void unpackJar(File location, File outputJar) {
        ZipUnArchiver unzip;
        FileLockService fileLockService;
        try {
            unzip = (ZipUnArchiver) session.lookup(ZipUnArchiver.ROLE, "zip");
            fileLockService = (FileLockService) session.lookup(FileLockService.class.getName());
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Could not lookup required component", e);
        }

        outputJar.mkdirs();

        if (!outputJar.isDirectory()) {
            throw new RuntimeException("Could not create output directory " + outputJar.getAbsolutePath());
        }

        unzip.setSourceFile(location);
        unzip.setDestDirectory(outputJar);
        FileLocker locker = fileLockService.getFileLocker(location);
        locker.lock();
        try {
            unzip.extract();
        } catch (ArchiverException e) {
            throw new RuntimeException("Could not unpack jar", e);
        } finally {
            locker.release();
        }
    }

    private void copyDir(File location, File outputJar) {
        try {
            FileUtils.copyDirectoryStructure(location, outputJar);
        } catch (IOException e) {
            throw new RuntimeException("Could not copy directory", e);
        }
    }

    private void copyUrl(String source, File destination) {
        try {
            URL url = new URL(source);
            InputStream is = url.openStream();
            try {
                OutputStream os = new BufferedOutputStream(new FileOutputStream(destination));
                try {
                    IOUtil.copy(is, os);
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not copy URL contents", e);
        }
    }

    private void copyFile(File source, File destination) {
        try {
            FileUtils.copyFile(source, destination);
        } catch (IOException e) {
            throw new RuntimeException("Could not copy file", e);
        }
    }

    private void shipPack200(File jar) {
        // TODO validate if the jar has been pack200 pre-conditioned

        File outputPack = new File(jar.getParentFile(), jar.getName() + ".pack");

        Pack200Archiver packArchiver = new Pack200Archiver();
        packArchiver.setSourceJar(jar);
        packArchiver.setDestFile(outputPack);
        try {
            packArchiver.createArchive();
        } catch (IOException e) {
            throw new RuntimeException("Could not create pack200 archive", e);
        }

        GZipCompressor gzCompressor = new GZipCompressor();
        gzCompressor.setDestFile(new File(jar.getParentFile(), jar.getName() + ".pack.gz"));
        gzCompressor.setSourceFile(outputPack);
        try {
            gzCompressor.execute();
        } catch (ArchiverException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        outputPack.delete();
    }

    private void packDir(File sourceDir, File targetZip) {
        ZipArchiver archiver;
        try {
            archiver = (ZipArchiver) session.lookup(ZipArchiver.ROLE, "zip");
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Unable to resolve ZipArchiver", e);
        }

        archiver.setDestFile(targetZip);
        try {
            archiver.addDirectory(sourceDir);
            archiver.createArchive();
        } catch (IOException e) {
            throw new RuntimeException("Error packing zip", e);
        } catch (ArchiverException e) {
            throw new RuntimeException("Error packing zip", e);
        }
    }

    public void setArchives(Map<String, String> archives) {
        this.archives = archives;
    }

    public void setPack200(boolean pack200) {
        this.pack200 = pack200;
    }

    public void setUnpackPlugins(boolean unpack) {
        this.unpackPlugins = unpack;
    }

    public void setUnpackFeatures(boolean unpack) {
        this.unpackFeatures = unpack;
    }
}
