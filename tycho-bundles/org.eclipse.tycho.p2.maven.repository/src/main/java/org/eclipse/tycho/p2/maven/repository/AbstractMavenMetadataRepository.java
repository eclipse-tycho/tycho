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
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public abstract class AbstractMavenMetadataRepository extends AbstractMetadataRepository {

    protected final TychoRepositoryIndex metadataIndex;

    protected final RepositoryReader contentLocator;

    protected Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

    protected Map<GAV, Set<IInstallableUnit>> unitsMap = new LinkedHashMap<GAV, Set<IInstallableUnit>>();

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
                        RepositoryLayoutHelper.CLASSIFIER_P2_METADATA, RepositoryLayoutHelper.EXTENSION_P2_METADATA);
                if (!localArtifactFileLocation.exists()) {
                    // if files have been manually removed from the repository, simply remove them from the index (bug 351080)
                    metadataIndex.removeGav(gav);
                } else {
                    InputStream is = contentLocator.getContents(gav, RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                            RepositoryLayoutHelper.EXTENSION_P2_METADATA);
                    try {
                        Set<IInstallableUnit> gavUnits = io.readXML(is);

                        unitsMap.put(gav, gavUnits);
                        units.addAll(gavUnits);
                    } finally {
                        is.close();
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

    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        return query.perform(units.iterator());
    }

    /**
     * For testing purposes only
     */
    public Map<GAV, Set<IInstallableUnit>> getGAVs() {
        return unitsMap;
    }

    public Collection<IRepositoryReference> getReferences() {
        return Collections.emptyList();
    }
}
