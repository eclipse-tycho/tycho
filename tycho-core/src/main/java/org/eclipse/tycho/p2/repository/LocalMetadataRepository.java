/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.MavenContext;

public class LocalMetadataRepository extends AbstractMavenMetadataRepository {

    private Set<GAV> changedGAVs = new LinkedHashSet<>();

    /**
     * Local existing repository
     */
    public LocalMetadataRepository(IProvisioningAgent agent, URI location, TychoRepositoryIndex projectIndex,
            RepositoryReader contentLocator) {
        super(agent, location, projectIndex, contentLocator);
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
            gavUnits = new LinkedHashSet<>();
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
                MavenContext mavenContext;
                if (contentLocator != null) {
                    mavenContext = contentLocator.getMavenContext();
                } else {
                    mavenContext = metadataIndex.getMavenContext();
                }
                String relpath = RepositoryLayoutHelper.getRelativePath(gav, TychoConstants.CLASSIFIER_P2_METADATA,
                        ArtifactType.TYPE_P2_METADATA, mavenContext);

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
