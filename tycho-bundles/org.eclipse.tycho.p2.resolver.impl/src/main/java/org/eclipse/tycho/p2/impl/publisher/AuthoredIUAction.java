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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
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
    public static final String IU_TYPE = "org.eclipse.equinox.p2.type.iu";

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
            Set<InstallableUnitDescription> iuDescriptions = new MetadataIO().readXMLAsDescriptor(is);
            tweakIUs(iuDescriptions);
            Set<IInstallableUnit> ius = toIUs(iuDescriptions);
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
            //If no artifact has been referenced in the metadata, we publish a fake one because the code is not meant to handle this 
            // and fails in many places. I tried to change the code where the failures were occurring but did not succeed.
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

    private Set<IInstallableUnit> toIUs(Set<InstallableUnitDescription> iuDescriptions) {
        HashSet<IInstallableUnit> result = new HashSet<IInstallableUnit>(iuDescriptions.size());
        for (InstallableUnitDescription description : iuDescriptions) {
            result.add(MetadataFactory.createInstallableUnit(description));
        }
        return result;
    }

    //This tweaking is done in order to minimize the number of things the user has to write in the IU
    //Similar logic is performed in the PackageIUMojo. Though the duplication of these tweaks is not necessary
    //for the proper functioning the system, having the p2iu.xml file written in the target containing this
    //tweaks will help understand what is happening.
    private void tweakIUs(Set<InstallableUnitDescription> ius) {
        for (InstallableUnitDescription iu : ius) {
            addMarkerProperty(iu);
            ensureSelfCapability(iu);
        }
    }

    private void addMarkerProperty(InstallableUnitDescription iu) {
        iu.setProperty(IU_TYPE, Boolean.TRUE.toString());
    }

    private void ensureSelfCapability(InstallableUnitDescription iu) {
        Collection<IProvidedCapability> capabilities = iu.getProvidedCapabilities();
        for (IProvidedCapability capability : capabilities) {
            if (IInstallableUnit.NAMESPACE_IU_ID.equals(capability.getNamespace())
                    && iu.getId().equals(capability.getName()) && iu.getVersion().equals(capability.getVersion())) {
                return;
            }
        }
        IProvidedCapability[] newCapabilities = new IProvidedCapability[capabilities.size() + 1];
        capabilities.toArray(newCapabilities);
        newCapabilities[newCapabilities.length - 1] = createSelfCapability(iu.getId(), iu.getVersion());
        iu.setCapabilities(newCapabilities);
    }
}
