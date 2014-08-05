/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.p2.impl.Activator;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

@SuppressWarnings("restriction")
public class AuthoredIUAction extends AbstractPublisherAction implements IPublisherAction {
    public File iuProject;

    public AuthoredIUAction(File location) {
        iuProject = location;
    }

    @SuppressWarnings("deprecation")
    public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
        File iuFile = new File(iuProject, "p2iu.xml");
        if (!iuFile.exists())
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not find the p2iu.xml file in folder "
                    + iuProject);
        try {
            FileInputStream is = new FileInputStream(iuFile);
            Set<IInstallableUnit> ius = new MetadataIO().readXML(is);
            results.addIUs(ius, IPublisherResult.ROOT);
            IArtifactRepository repo = info.getArtifactRepository();
            boolean artifactReferenced = false;
            if (repo != null) {
                for (IInstallableUnit iu : ius) {
                    Collection<IArtifactKey> associatedKeys = iu.getArtifacts();
                    for (IArtifactKey key : associatedKeys) {
                        ArtifactDescriptor ad = (ArtifactDescriptor) PublisherHelper.createArtifactDescriptor(info,
                                key, null);
                        processArtifactPropertiesAdvice(iu, ad, info);
                        ad.setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, IArtifactDescriptor.TYPE_ZIP);
                        ad.setProperty(RepositoryLayoutHelper.PROP_EXTENSION, "zip");
                        repo.addDescriptor(ad);
                        artifactReferenced = true;
                    }
                }
            }
            //If no artifact has been referenced in the metadata, we publish a fake one to make sure things are correct.
            if (!artifactReferenced && repo != null) {
                IInstallableUnit iu = ((IInstallableUnit) ius.iterator().next());
                ArtifactDescriptor ad = (ArtifactDescriptor) PublisherHelper.createArtifactDescriptor(info,
                        new ArtifactKey("binary", "generated_" + iu.getId(), iu.getVersion()), null);
                processArtifactPropertiesAdvice(iu, ad, info);
                ad.setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, IArtifactDescriptor.TYPE_ZIP);
                repo.addDescriptor(ad);
                artifactReferenced = true;
            }
            return Status.OK_STATUS;
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error while reading " + iuFile, e);
        }
    }
}
