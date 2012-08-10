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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class LocalMetadataRepository extends AbstractMavenMetadataRepository {

    public static enum Consider {
        TRUE, FALSE, DEFAULT;

        public static Consider getConsider(Properties userProperties) {
            String considerLocal = userProperties.getProperty("tycho.considerLocal");
            if (considerLocal == null) {
                return DEFAULT;
            }
            return Boolean.valueOf(considerLocal) ? TRUE : FALSE;
        }

    }

    private Set<GAV> changedGAVs = new LinkedHashSet<GAV>();
    private Consider consider = Consider.DEFAULT;

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
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        if (consider == Consider.FALSE) {
            return new CollectionResult<IInstallableUnit>(Collections.<IInstallableUnit> emptyList());
        } else {
            return super.query(query, monitor);
        }
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

    public void setConsider(Consider consider) {
        this.consider = consider;
    }

    public Consider getConsider() {
        return consider;
    }

}
