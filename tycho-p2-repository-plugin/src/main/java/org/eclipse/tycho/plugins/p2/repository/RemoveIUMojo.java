/*******************************************************************************
 * Copyright (c) 2009, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation in P2 as an ant task
 *     Christoph Läubrich - migration to maven-mojo
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Mojo that provides the <code>p2.remove.iu</code> ant task described <a href=
 * "https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/p2_repositorytasks.htm">here</a>.
 */
@Mojo(name = "remove-iu")
public class RemoveIUMojo extends AbstractRepositoryMojo {

    private static final String CLASSIFIER = "classifier"; //$NON-NLS-1$
    private static final String ID = "id"; //$NON-NLS-1$
    private static final String VERSION = "version"; //$NON-NLS-1$

    @Component
    private P2RepositoryManager repositoryManager;

    @Parameter
    private List<IUDescription> iu;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (iu == null || iu.isEmpty()) {
            return;
        }
        File location = getAssemblyRepositoryLocation();
        try {
            IArtifactRepository artifactRepository = repositoryManager.getArtifactRepository(location,
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE);
            IMetadataRepository metadataRepository = repositoryManager.getMetadataRepository(location,
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE);
            metadataRepository.executeBatch(m -> {
                artifactRepository.executeBatch(m2 -> {
                    removeIUs(metadataRepository, artifactRepository, iu, getLog());
                }, m);
            }, null);
        } catch (ProvisionException e) {
            throw new MojoFailureException("Loading repository failed", e);
        }

    }

    private static void removeIUs(IMetadataRepository repository, IArtifactRepository artifacts,
            List<IUDescription> iuTasks, Log log) {
        final Set<IInstallableUnit> toRemove = new HashSet<>();
        for (IUDescription iu : iuTasks) {
            IQuery<IInstallableUnit> iuQuery = iu.createQuery();

            IQueryResult<IInstallableUnit> queryResult = repository.query(iuQuery, null);

            if (queryResult.isEmpty()) {
                log.warn(String.format("Unable to find %s.", iu.toString()));
            } else {
                for (Iterator<IInstallableUnit> iterator = queryResult.iterator(); iterator.hasNext();) {
                    IInstallableUnit unit = iterator.next();
                    Collection<IArtifactKey> keys = unit.getArtifacts();
                    Filter filter = null;
                    try {
                        filter = iu.getArtifactFilter();
                    } catch (InvalidSyntaxException e) {
                        log.warn(String.format("Invalid filter format, skipping %s.", iu.toString()));
                        continue;
                    }
                    //we will only remove the metadata if all artifacts were removed
                    boolean removeMetadata = (filter != null ? keys.size() > 0 : true);
                    for (IArtifactKey key : keys) {
                        if (filter == null) {
                            artifacts.removeDescriptor(key, new NullProgressMonitor());
                        } else {
                            IArtifactDescriptor[] descriptors = artifacts.getArtifactDescriptors(key);
                            for (IArtifactDescriptor descriptor : descriptors) {
                                if (filter.match(createDictionary(descriptor))) {
                                    artifacts.removeDescriptor(descriptor, new NullProgressMonitor());
                                } else {
                                    removeMetadata = false;
                                }
                            }
                        }
                    }
                    if (removeMetadata) {
                        toRemove.add(unit);
                    }
                }
            }
        }

        if (toRemove.size() > 0) {
            repository.removeInstallableUnits(toRemove);
        }
    }

    private static Dictionary<String, Object> createDictionary(IArtifactDescriptor descriptor) {
        Hashtable<String, Object> result = new Hashtable<>(5);
        result.putAll(descriptor.getProperties());
        IArtifactKey key = descriptor.getArtifactKey();
        result.put(CLASSIFIER, key.getClassifier());
        result.put(ID, key.getId());
        result.put(VERSION, key.getVersion());
        return result;
    }
}
