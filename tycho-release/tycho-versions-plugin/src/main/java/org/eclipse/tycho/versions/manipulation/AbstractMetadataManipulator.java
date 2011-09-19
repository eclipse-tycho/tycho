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

import java.util.Set;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.pom.MutablePomFile;

public abstract class AbstractMetadataManipulator implements MetadataManipulator {

    @Requirement
    protected Logger logger;

    protected boolean isBundle(ProjectMetadata project) {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);
        return isBundle(pom);
    }

    protected boolean isBundle(MutablePomFile pom) {
        String packaging = pom.getPackaging();
        return ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
    }

    protected boolean isFeature(ProjectMetadata project) {
        String packaging = project.getMetadata(MutablePomFile.class).getPackaging();
        return isFeature(packaging);
    }

    protected boolean isFeature(String packaging) {
        return ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(packaging);
    }

    public boolean addMoreChanges(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        return false;
    }
}
