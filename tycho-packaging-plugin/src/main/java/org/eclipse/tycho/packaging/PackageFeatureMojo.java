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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;

/**
 * @phase package
 * @goal package-feature
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class PackageFeatureMojo extends AbstractTychoPackagingMojo {

    private static final int KBYTE = 1024;

    /**
     * The maven archiver to use.
     * 
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @parameter expression="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * @parameter expression="${project.basedir}"
     */
    private File basedir;

    /**
     * Name of the generated JAR.
     * 
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * If set to <code>true</code> (the default), standard eclipse update site directory with
     * feature content will be created under target folder.
     * 
     * @parameter default-value="false"
     */
    private boolean deployableFeature = false;

    /**
     * @parameter expression="${project.build.directory}/site"
     */
    private File target;

    /**
     * @component
     */
    private FileLockService fileLockService;

    public void execute() throws MojoExecutionException, MojoFailureException {
        expandVersion();

        Properties props = new Properties();
        try {
            FileInputStream is = new FileInputStream(new File(basedir, "build.properties"));
            try {
                props.load(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading build properties", e);
        }

        outputDirectory.mkdirs();

        Feature feature;
        File featureXml = new File(outputDirectory, Feature.FEATURE_XML);
        try {
            feature = getUpdatedFeatureXml();
            Feature.write(feature, featureXml);
        } catch (IOException e) {
            throw new MojoExecutionException("Error updating feature.xml", e);
        }

        File outputJar = new File(outputDirectory, finalName + ".jar");
        outputJar.getParentFile().mkdirs();
        List<String> binIncludes = toFilePattern(props.getProperty("bin.includes"));
        List<String> binExcludes = toFilePattern(props.getProperty("bin.excludes"));
        binExcludes.add(Feature.FEATURE_XML); // we'll include updated feature.xml

        MavenArchiver archiver = new MavenArchiver();
        JarArchiver jarArchiver = getJarArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(outputJar);
        jarArchiver.setDestFile(outputJar);

        try {
            archiver.getArchiver().addFileSet(getFileSet(basedir, binIncludes, binExcludes));
            archiver.getArchiver().addFile(featureXml, Feature.FEATURE_XML);
            archiver.createArchive(project, archive);
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating feature package", e);
        }

        project.getArtifact().setFile(outputJar);

        if (deployableFeature) {
            assembleDeployableFeature(feature);
        }
    }

    private void assembleDeployableFeature(Feature feature) throws MojoExecutionException {
        UpdateSiteAssembler assembler = new UpdateSiteAssembler(session, target);
        getDependencyWalker().walk(assembler);
    }

    private Feature getUpdatedFeatureXml() throws MojoExecutionException, IOException {
        final Feature feature = Feature.loadFeature(basedir);

        getDependencyWalker().traverseFeature(basedir, feature, new ArtifactDependencyVisitor() {
            public void visitPlugin(PluginDescription plugin) {
                PluginRef pluginRef = plugin.getPluginRef();

                if (pluginRef == null) {
                    // can't really happen
                    return;
                }

                File location = plugin.getLocation();

                ReactorProject bundleProject = plugin.getMavenProject();
                if (bundleProject != null) {
                    location = bundleProject.getArtifact();

                    if (location == null || location.isDirectory()) {
                        throw new IllegalStateException("At least ``package'' phase execution is required");
                    }

                    pluginRef.setVersion(bundleProject.getExpandedVersion());
                } else {
                    // use version from target platform
                    pluginRef.setVersion(plugin.getKey().getVersion());
                }

                long downloadSize = 0;
                long installSize = 0;
                if (location.isFile()) {
                    installSize = getInstallSize(location);
                    downloadSize = location.length();
                } else {
                    getLog().info(
                            "Download/install size is not calculated for directory based bundle " + pluginRef.getId());
                }

                pluginRef.setDownloadSide(downloadSize / KBYTE);
                pluginRef.setInstallSize(installSize / KBYTE);
            }

            public boolean visitFeature(FeatureDescription feature) {
                FeatureRef featureRef = feature.getFeatureRef();
                if (featureRef == null) {
                    // this feature
                    ReactorProject reactorProject = DefaultReactorProject.adapt(project);
                    feature.getFeature().setVersion(reactorProject.getExpandedVersion());
                    return true; // keep visiting
                } else {
                    // included feature
                    ReactorProject otherProject = feature.getMavenProject();
                    if (otherProject != null) {
                        featureRef.setVersion(otherProject.getExpandedVersion());
                    } else {
                        featureRef.setVersion(feature.getKey().getVersion());
                    }
                }

                return false; // do not traverse included features
            }
        });

        return feature;
    }

    private JarArchiver getJarArchiver() throws MojoExecutionException {
        try {
            JarArchiver jarArchiver = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
            return jarArchiver;
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Unable to get JarArchiver", e);
        }
    }

    protected long getInstallSize(File location) {
        long installSize = 0;
        FileLocker locker = fileLockService.getFileLocker(location);
        locker.lock();
        try {
            try {
                JarFile jar = new JarFile(location);
                try {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = (JarEntry) entries.nextElement();
                        long entrySize = entry.getSize();
                        if (entrySize > 0) {
                            installSize += entrySize;
                        }
                    }
                } finally {
                    jar.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not determine installation size", e);
            }
        } finally {
            locker.release();
        }
        return installSize;
    }
}
