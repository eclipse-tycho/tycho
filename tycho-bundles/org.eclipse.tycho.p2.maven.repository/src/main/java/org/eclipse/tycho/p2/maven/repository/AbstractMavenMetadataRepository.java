/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.maven.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public abstract class AbstractMavenMetadataRepository extends AbstractMetadataRepository {

    protected final TychoRepositoryIndex metadataIndex;

    protected final RepositoryReader contentLocator;

    protected Set<IInstallableUnit> units = new LinkedHashSet<>();

    protected Map<GAV, Set<IInstallableUnit>> unitsMap = new LinkedHashMap<>();

    public AbstractMavenMetadataRepository(URI location, TychoRepositoryIndex metadataIndex,
            RepositoryReader contentLocator) {
        this(Activator.getProvisioningAgent(), location, metadataIndex, contentLocator);
    }

    public AbstractMavenMetadataRepository(IProvisioningAgent agent, URI location, TychoRepositoryIndex metadataIndex,
            RepositoryReader contentLocator) {
        super(agent);

        setLocation(location);

        this.metadataIndex = metadataIndex;
        this.contentLocator = contentLocator;

        if (metadataIndex != null && contentLocator != null) {
            load();
        }
    }

    protected void load() {
        MetadataIO io = new MetadataIO();

        for (GAV gav : metadataIndex.getProjectGAVs()) {
            try {
                File localArtifactFileLocation = contentLocator.getLocalArtifactLocation(gav,
                        TychoConstants.CLASSIFIER_P2_METADATA, ArtifactType.TYPE_P2_METADATA);
                if (!localArtifactFileLocation.exists()) {
                    // if files have been manually removed from the repository, simply remove them from the index (bug 351080)
                    metadataIndex.removeGav(gav);
                } else {
                    try (InputStream is = new FileInputStream(localArtifactFileLocation)) {
                        Set<IInstallableUnit> gavUnits = io.readXML(is);

                        unitsMap.put(gav, gavUnits);
                        units.addAll(gavUnits);
                    }
                }
            } catch (IOException e) {
                // TODO throw properly typed exception if repository cannot be loaded
                e.printStackTrace();
            }

        }
    }

    @Override
    public void initialize(RepositoryState state) {
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        return query.perform(units.iterator());
    }

    /**
     * For testing purposes only
     */
    public Map<GAV, Set<IInstallableUnit>> getGAVs() {
        return unitsMap;
    }

    @Override
    public Collection<IRepositoryReference> getReferences() {
        return Collections.emptyList();
    }
}
