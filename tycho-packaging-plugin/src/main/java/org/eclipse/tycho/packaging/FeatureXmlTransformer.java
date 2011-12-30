/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;

@Component(role = FeatureXmlTransformer.class)
public class FeatureXmlTransformer {
    private static final int KBYTE = 1024;

    @Requirement
    private Logger log;

    @Requirement
    private FileLockService fileLockService;

    public Feature transform(final ReactorProject reactorProject, Feature source, ArtifactDependencyWalker dependencies) {

        Feature feature = new Feature(source);

        dependencies.traverseFeature(reactorProject.getBasedir(), feature, new ArtifactDependencyVisitor() {
            public void visitPlugin(PluginDescription plugin) {
                PluginRef pluginRef = plugin.getPluginRef();

                if (pluginRef == null) {
                    // can't really happen
                    return;
                }

                File location = plugin.getLocation();

                ReactorProject bundleProject = plugin.getMavenProject();
                if (bundleProject != null) {
                    location = bundleProject.getArtifact(plugin.getClassifier());
                    if (location == null) {
                        throw new IllegalStateException(bundleProject.getId()
                                + " does not provide an artifact with classifier '" + plugin.getClassifier() + "'");
                    }
                    if (location.isDirectory()) {
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
                    log.info("Download/install size is not calculated for directory based bundle " + pluginRef.getId());
                }

                pluginRef.setDownloadSide(downloadSize / KBYTE);
                pluginRef.setInstallSize(installSize / KBYTE);
            }

            public boolean visitFeature(FeatureDescription feature) {
                FeatureRef featureRef = feature.getFeatureRef();
                if (featureRef == null) {
                    // this feature
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
