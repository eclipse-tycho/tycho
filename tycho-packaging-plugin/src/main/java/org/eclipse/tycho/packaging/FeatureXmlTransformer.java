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
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.TargetPlatform;
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

    @Requirement
    private EquinoxServiceFactory osgiServices;

    // TODO unit test
    // TODO extract methods
    public Feature transform(final ReactorProject reactorProject, Feature source, TargetPlatform targetPlatform) {
        Feature feature = new Feature(source); // TODO why is this needed?
        feature.setVersion(reactorProject.getExpandedVersion());

        for (PluginRef pluginRef : feature.getPlugins()) {
            // TODO the key contains an expanded version - this contradicts the ArtifactKey's JavaDoc
            // TODO catch exceptions and enrich message
            ArtifactKey plugin = targetPlatform.resolveReference(ArtifactType.TYPE_ECLIPSE_PLUGIN, pluginRef.getId(),
                    pluginRef.getVersion());
            File location = targetPlatform.getArtifactLocation(plugin);
            pluginRef.setVersion(plugin.getVersion());
            setDownloadAndInstallSize(pluginRef, location);
        }

        List<FeatureRef> includedFeatures = feature.getIncludedFeatures();
        for (FeatureRef featureRef : includedFeatures) {
            ArtifactKey includedFeature = targetPlatform.resolveReference(ArtifactType.TYPE_ECLIPSE_FEATURE,
                    featureRef.getId(), featureRef.getVersion());
            featureRef.setVersion(includedFeature.getVersion());
        }

        return feature;
    }

    private void setDownloadAndInstallSize(PluginRef pluginRefToEdit, File artifact) {
        long downloadSize = 0;
        long installSize = 0;
        if (artifact.isFile()) {
            installSize = getInstallSize(artifact);
            downloadSize = artifact.length();
        } else {
            log.info("Download/install size is not calculated for directory based bundle " + pluginRefToEdit.getId());
        }

        pluginRefToEdit.setDownloadSide(downloadSize / KBYTE);
        pluginRefToEdit.setInstallSize(installSize / KBYTE);
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
                throw new RuntimeException("Could not determine installation size of file " + location, e);
            }
        } finally {
            locker.release();
        }
        return installSize;
    }
}
