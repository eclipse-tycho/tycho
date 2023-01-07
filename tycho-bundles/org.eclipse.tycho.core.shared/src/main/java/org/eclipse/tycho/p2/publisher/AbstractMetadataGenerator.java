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
package org.eclipse.tycho.p2.publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.repository.util.StatusTool;

public abstract class AbstractMetadataGenerator {

    private IProgressMonitor monitor = new NullProgressMonitor();

    protected DependencyMetadata generateMetadata(IArtifactFacade artifact, List<TargetEnvironment> environments,
            IPublisherInfo publisherInfo, OptionalResolutionAction optionalAction, PublisherOptions options) {
        for (IPublisherAdvice advice : getPublisherAdvice(artifact, options)) {
            publisherInfo.addAdvice(advice);
        }
        List<IPublisherAction> actions = getPublisherActions(artifact, environments, optionalAction);

        return publish(publisherInfo, actions);
    }

    protected abstract List<IPublisherAction> getPublisherActions(IArtifactFacade artifact,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction);

    protected abstract List<IPublisherAdvice> getPublisherAdvice(IArtifactFacade artifact, PublisherOptions options);

    protected ICapabilityAdvice getExtraEntriesAdvice(IArtifactFacade artifact, BuildProperties buildProps) {
        final IRequirement[] extraRequirements = extractExtraEntriesAsIURequirement(buildProps);
        return new ICapabilityAdvice() {
            @Override
            public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
                return true;
            }

            @Override
            public IRequirement[] getRequiredCapabilities(InstallableUnitDescription iu) {
                return extraRequirements;
            }

            @Override
            public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu) {
                return null;
            }

            @Override
            public IRequirement[] getMetaRequiredCapabilities(InstallableUnitDescription iu) {
                return null;
            }
        };
    }

    private IRequirement[] extractExtraEntriesAsIURequirement(BuildProperties buildProps) {

        ArrayList<IRequirement> result = new ArrayList<>();
        for (Entry<String, List<String>> entry : buildProps.getJarToExtraClasspathMap().entrySet()) {
            createRequirementFromExtraClasspathProperty(result, entry.getValue());
        }
        createRequirementFromExtraClasspathProperty(result, buildProps.getJarsExtraClasspath());
        if (result.isEmpty())
            return null;
        return result.toArray(new IRequirement[result.size()]);
    }

    private void createRequirementFromExtraClasspathProperty(ArrayList<IRequirement> result, List<String> urls) {
        for (String url : urls) {
            createRequirementFromPlatformURL(result, url);
        }
    }

    private void createRequirementFromPlatformURL(ArrayList<IRequirement> result, String url) {
        Matcher m = TychoConstants.PLATFORM_URL_PATTERN.matcher(url);
        if (m.matches())
            result.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, m.group(2),
                    VersionRange.emptyRange, null, false, false));
    }

    private DependencyMetadata publish(IPublisherInfo publisherInfo, List<IPublisherAction> actions) {
        PublisherResult result = new PublisherResult();

        Publisher publisher = new Publisher(publisherInfo, result);

        IStatus status = publisher.publish(actions.toArray(new IPublisherAction[actions.size()]), monitor);

        if (!status.isOK()) {
            throw new RuntimeException(StatusTool.collectProblems(status), status.getException());
        }

        DependencyMetadata metadata = new DependencyMetadata();

        metadata.setDependencyMetadata(DependencyMetadataType.SEED, result.getIUs(null, PublisherResult.ROOT));
        metadata.setDependencyMetadata(DependencyMetadataType.RESOLVE, result.getIUs(null, PublisherResult.NON_ROOT));

        IArtifactRepository artifactRepository = publisherInfo.getArtifactRepository();
        if (artifactRepository instanceof TransientArtifactRepository transientArtifactRepo) {
            metadata.setArtifacts(transientArtifactRepo.getArtifactDescriptors());
        }

        return metadata;
    }

    protected abstract BuildPropertiesParser getBuildPropertiesParser();

}
