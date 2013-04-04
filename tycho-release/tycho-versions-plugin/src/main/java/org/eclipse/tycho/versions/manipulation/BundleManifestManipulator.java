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
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.versions.bundle.MutableBundleManifest;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.Versions;

@Component(role = MetadataManipulator.class, hint = "bundle-manifest")
public class BundleManifestManipulator extends AbstractMetadataManipulator {

    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        // only update bundle version for now
        if (isBundle(project) && isProjectVersionChange(project, change)) {
            MutableBundleManifest mf = getBundleManifest(project);

            logger.info("  META-INF/MANIFEST.MF//Bundle-Version: " + change.getVersion() + " => "
                    + change.getNewVersion());

            mf.setVersion(change.getNewVersion());
        }
    }

    public Collection<String> validateChange(ProjectMetadata project, VersionChange change) {
        if (isBundle(project) && isProjectVersionChange(project, change)) {
            String error = Versions.validateOsgiVersion(change.getNewVersion(), getManifestFile(project));
            return error != null ? Collections.singleton(error) : null;
        }
        return null;
    }

    private boolean isProjectVersionChange(ProjectMetadata project, VersionChange change) {
        MutableBundleManifest mf = getBundleManifest(project);
        return change.getArtifactId().equals(mf.getSymbolicName()) && change.getVersion().equals(mf.getVersion());
    }

    private MutableBundleManifest getBundleManifest(ProjectMetadata project) {
        MutableBundleManifest mf = project.getMetadata(MutableBundleManifest.class);
        if (mf == null) {
            File file = getManifestFile(project);
            try {
                mf = MutableBundleManifest.read(file);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not parse bundle manifest " + file, e);
            }
            project.putMetadata(mf);
        }
        return mf;
    }

    private File getManifestFile(ProjectMetadata project) {
        return new File(project.getBasedir(), "META-INF/MANIFEST.MF");
    }

    public void writeMetadata(ProjectMetadata project) throws IOException {
        MutableBundleManifest mf = project.getMetadata(MutableBundleManifest.class);
        if (mf != null) {
            MutableBundleManifest.write(mf, getManifestFile(project));
        }
    }
}
