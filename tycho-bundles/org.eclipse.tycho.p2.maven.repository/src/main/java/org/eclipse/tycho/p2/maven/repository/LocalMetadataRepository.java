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
package org.eclipse.tycho.p2.maven.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class LocalMetadataRepository extends AbstractMavenMetadataRepository {

    private Set<GAV> changedGAVs = new LinkedHashSet<GAV>();

    /**
     * Create new repository
     */
    public LocalMetadataRepository(URI location, TychoRepositoryIndex metadataIndex) {
        super(location, metadataIndex, null);
        if (!location.getScheme().equals("file")) {
            throw new IllegalArgumentException("Invalid local repository location: " + location); //$NON-NLS-1$
        }

        // when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
        save();
    }

    /**
     * Local existing repository
     */
    public LocalMetadataRepository(URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator) {
        super(location, projectIndex, contentLocator);
    }

    @Override
    public void addInstallableUnits(Collection<IInstallableUnit> newUnits) {
        for (IInstallableUnit unit : newUnits) {
            GAV gav = RepositoryLayoutHelper.getGAV(unit.getProperties());

            addInstallableUnit(unit, gav);
        }

        save();
    }

    public void addInstallableUnit(IInstallableUnit unit, GAV gav) {
        this.units.add(unit);

        Set<IInstallableUnit> gavUnits = unitsMap.get(gav);
        if (gavUnits == null) {
            gavUnits = new LinkedHashSet<IInstallableUnit>();
            unitsMap.put(gav, gavUnits);
        }
        gavUnits.add(unit);

        changedGAVs.add(gav);
    }

    public void save() {
        File basedir = new File(getLocation());

        MetadataIO io = new MetadataIO();

        for (GAV gav : changedGAVs) {
            Set<IInstallableUnit> gavUnits = unitsMap.get(gav);

            if (gavUnits != null && !gavUnits.isEmpty()) {
                String relpath = RepositoryLayoutHelper.getRelativePath(gav,
                        RepositoryLayoutHelper.CLASSIFIER_P2_METADATA, RepositoryLayoutHelper.EXTENSION_P2_METADATA);

                File file = new File(basedir, relpath);
                file.getParentFile().mkdirs();

                try {
                    io.writeXML(gavUnits, file);

                    metadataIndex.addGav(gav);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            metadataIndex.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        changedGAVs.clear();
    }

    @Override
    public boolean isModifiable() {
        return true;
    }

}
