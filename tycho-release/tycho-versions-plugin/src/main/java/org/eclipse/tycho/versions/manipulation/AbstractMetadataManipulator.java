/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - introduce VersionChangesDescriptor
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.pom.PomFile;

public abstract class AbstractMetadataManipulator implements MetadataManipulator {

    @Requirement
    protected Logger logger;

    protected boolean isBundle(ProjectMetadata project) {
        PomFile pom = project.getMetadata(PomFile.class);
        return isBundle(pom);
    }

    protected boolean isBundle(PomFile pom) {
        String packaging = pom.getPackaging();
        return PackagingType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                || PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
    }

    protected boolean isFeature(ProjectMetadata project) {
        String packaging = project.getMetadata(PomFile.class).getPackaging();
        return isFeature(packaging);
    }

    protected boolean isFeature(String packaging) {
        return PackagingType.TYPE_ECLIPSE_FEATURE.equals(packaging);
    }

    @Override
    public boolean addMoreChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        return false;
    }
}
