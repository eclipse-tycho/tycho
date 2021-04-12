/*******************************************************************************
 * Copyright (c) 2011, 2014 Sonatype Inc. and others.
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
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
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

    public FeatureXmlTransformer() {
    }

    public FeatureXmlTransformer(Logger log, FileLockService fileLockService) {
        this.log = log;
        this.fileLockService = fileLockService;
    }

    /**
     * Replaces references in the feature model with versions from the target platform.
     * 
     * @param feature
     *            The feature model to have plug-in and feature references completed.
     */
    public Feature expandReferences(Feature feature, TargetPlatform targetPlatform) throws MojoFailureException {

        for (PluginRef pluginRef : feature.getPlugins()) {
            ArtifactKey plugin = resolvePluginReference(targetPlatform, pluginRef);
            pluginRef.setVersion(plugin.getVersion());

            File location = targetPlatform.getArtifactLocation(plugin);
            setDownloadAndInstallSize(pluginRef, location);
        }

        for (FeatureRef featureRef : feature.getIncludedFeatures()) {
            ArtifactKey includedFeature = resolveFeatureReference(targetPlatform, featureRef);
            featureRef.setVersion(includedFeature.getVersion());
        }

        return feature;
    }

    private ArtifactKey resolvePluginReference(TargetPlatform targetPlatform, PluginRef pluginRef)
            throws MojoFailureException {
        try {
            return targetPlatform.resolveArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, pluginRef.getId(),
                    pluginRef.getVersion());
        } catch (IllegalArtifactReferenceException e) {
            throw new MojoFailureException("Invalid plugin reference with id=" + quote(pluginRef.getId())
                    + " and version=" + quote(pluginRef.getVersion()) + ": " + e.getMessage(), e);
        }
    }

    private ArtifactKey resolveFeatureReference(TargetPlatform targetPlatform, FeatureRef featureRef)
            throws MojoFailureException {
        try {
            return targetPlatform.resolveArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE, featureRef.getId(),
                    featureRef.getVersion());
        } catch (IllegalArtifactReferenceException e) {
            throw new MojoFailureException("Invalid feature reference with id=" + quote(featureRef.getId())
                    + " and version " + quote(featureRef.getVersion()) + ": " + e.getMessage(), e);
        }
    }

    private static String quote(String nullableString) {
        if (nullableString == null)
            return null;
        else
            return "\"" + nullableString + "\"";
    }

    private void setDownloadAndInstallSize(PluginRef pluginRefToEdit, File artifact) {
        // TODO 375111 optionally disable this?
        long downloadSize = 0;
        long installSize = 0;
        if (artifact.isFile()) {
            installSize = getInstallSize(artifact);
            downloadSize = artifact.length();
        } else {
            log.info("Download/install size is not calculated for directory based bundle " + pluginRefToEdit.getId());
        }

        pluginRefToEdit.setDownloadSize(downloadSize / KBYTE);
        pluginRefToEdit.setInstallSize(installSize / KBYTE);
    }

    protected long getInstallSize(File location) {
        long installSize = 0;
        FileLocker locker = fileLockService.getFileLocker(location);
        locker.lock();
        try {
            try {
                try (JarFile jar = new JarFile(location)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        long entrySize = entry.getSize();
                        if (entrySize > 0) {
                            installSize += entrySize;
                        }
                    }
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
