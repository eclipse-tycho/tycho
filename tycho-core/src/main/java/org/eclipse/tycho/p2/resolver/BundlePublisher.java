/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.core.resolver.target.FileArtifactRepository;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.osgi.framework.BundleException;

public class BundlePublisher extends BundlesAction {

    private BundlePublisher(BundleDescription bundleDescription) {
        super(new BundleDescription[] { bundleDescription });
    }

    public static Optional<IInstallableUnit> getBundleIU(File bundleLocation, IArtifactRepository repository,
            IPublisherAdvice... advices) throws IOException, BundleException {
        BundleDescription bundleDescription = BundlesAction.createBundleDescription(bundleLocation);
        if (bundleDescription == null) {
            //seems it is not a bundle
            return Optional.empty();
        }
        PublisherInfo publisherInfo = new PublisherInfo();
        for (IPublisherAdvice advice : advices) {
            publisherInfo.addAdvice(advice);
        }
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
        String symbolicName = bundleDescription.getSymbolicName();
        if (symbolicName == null) {
            return Optional.empty();
        }
        IArtifactKey key = BundlesAction.createBundleArtifactKey(symbolicName,
                bundleDescription.getVersion().toString());
        IArtifactDescriptor descriptor = FileArtifactRepository.forFile(bundleLocation, key, repository);
        return Optional.ofNullable(publishBundle(bundleDescription, descriptor, publisherInfo));
    }

    public static IInstallableUnit publishBundle(BundleDescription bundleDescription, IArtifactDescriptor descriptor,
            PublisherInfo publisherInfo) {
        BundlePublisher bundlesAction = new BundlePublisher(bundleDescription);
        bundlesAction.setPublisherInfo(publisherInfo);
        IInstallableUnit iu = bundlesAction.doCreateBundleIU(bundleDescription, descriptor.getArtifactKey(),
                publisherInfo);
        Collection<IPropertyAdvice> advice = publisherInfo.getAdvice(null, false, iu.getId(), iu.getVersion(),
                IPropertyAdvice.class);
        for (IPropertyAdvice entry : advice) {
            Map<String, String> props = entry.getArtifactProperties(iu, descriptor);
            if (props == null)
                continue;
            if (descriptor instanceof SimpleArtifactDescriptor simpleArtifactDescriptor) {
                for (Entry<String, String> pe : props.entrySet()) {
                    simpleArtifactDescriptor.setRepositoryProperty(pe.getKey(), pe.getValue());
                }
            }
        }
        return iu;
    }

    /**
     * Creates a new bundle repository at the given location, if the location already exits it is
     * deleted before performing the operation.
     * 
     * @param repositoryLocation
     *            the location where the repository should be stored
     * @param name
     *            the name of the repository
     * @param files
     *            the files to consider as bundles to be installed in the new repository
     * @param monitor
     *            the monitor to use for the operation
     * @throws ProvisionException
     *             if creation of the repository failed
     */
    public static void createBundleRepository(File repositoryLocation, String name, File[] files,
            IProgressMonitor monitor) throws ProvisionException {
        if (repositoryLocation.exists()) {
            FileUtils.deleteQuietly(repositoryLocation);
        }
        SimpleMetadataRepositoryFactory metadataRepositoryFactory = new SimpleMetadataRepositoryFactory();
        SimpleArtifactRepositoryFactory artifactRepositoryFactory = new SimpleArtifactRepositoryFactory();
        IArtifactRepository artifactRepository = artifactRepositoryFactory.create(repositoryLocation.toURI(), name,
                null, Map.of());
        IMetadataRepository metadataRepository = metadataRepositoryFactory.create(repositoryLocation.toURI(), name,
                null, Map.of());
        metadataRepository.executeBatch(m1 -> {
            artifactRepository.executeBatch(m2 -> {
                PublisherInfo publisherInfo = new PublisherInfo();
                publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);
                publisherInfo.setArtifactRepository(artifactRepository);
                publisherInfo.setMetadataRepository(metadataRepository);
                Publisher publisher = new Publisher(publisherInfo);
                publisher.publish(new IPublisherAction[] { new BundlesAction(files) }, m2);
            }, m1);
        }, monitor);
    }

}
