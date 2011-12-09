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
package org.eclipse.tycho.p2.impl.publisher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.impl.publisher.repo.TransientArtifactRepository;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator.OptionalResolutionAction;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.util.StatusTool;

@SuppressWarnings("restriction")
public abstract class AbstractMetadataGenerator {
    private IProgressMonitor monitor = new NullProgressMonitor();

    protected void generateMetadata(IArtifactFacade artifact, List<Map<String, String>> environments,
            Set<IInstallableUnit> units, Set<IArtifactDescriptor> artifacts, PublisherInfo publisherInfo,
            OptionalResolutionAction optionalAction) {
        for (IPublisherAdvice advice : getPublisherAdvice(artifact)) {
            publisherInfo.addAdvice(advice);
        }
        List<IPublisherAction> actions = getPublisherActions(artifact, environments, optionalAction);

        publish(units, artifacts, publisherInfo, actions);
    }

    protected abstract List<IPublisherAction> getPublisherActions(IArtifactFacade artifact,
            List<Map<String, String>> environments, OptionalResolutionAction optionalAction);

    protected abstract List<IPublisherAdvice> getPublisherAdvice(IArtifactFacade artifact);

    protected ICapabilityAdvice getExtraEntriesAdvice(IArtifactFacade artifact) {
        final IRequirement[] extraRequirements = extractExtraEntriesAsIURequirement(artifact.getLocation());
        return new ICapabilityAdvice() {
            public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
                return true;
            }

            public IRequirement[] getRequiredCapabilities(InstallableUnitDescription iu) {
                return extraRequirements;
            }

            public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu) {
                return null;
            }

            public IRequirement[] getMetaRequiredCapabilities(InstallableUnitDescription iu) {
                return null;
            }
        };
    }

    private IRequirement[] extractExtraEntriesAsIURequirement(File location) {
        Properties buildProperties = Utils.loadBuildProperties(location);
        if (buildProperties == null || buildProperties.size() == 0)
            return null;
        ArrayList<IRequirement> result = new ArrayList<IRequirement>();
        Set<Entry<Object, Object>> pairs = buildProperties.entrySet();
        for (Entry<Object, Object> pair : pairs) {
            if (!(pair.getValue() instanceof String))
                continue;
            String buildPropertyKey = (String) pair.getKey();
            if (buildPropertyKey.startsWith("extra.")) {
                createRequirementFromExtraClasspathProperty(result, ((String) pair.getValue()).split(","));
            }
        }

        String extra = buildProperties.getProperty("jars.extra.classpath");
        if (extra != null) {
            createRequirementFromExtraClasspathProperty(result, extra.split(","));
        }
        if (result.isEmpty())
            return null;
        return result.toArray(new IRequirement[result.size()]);
    }

    private void createRequirementFromExtraClasspathProperty(ArrayList<IRequirement> result, String[] urls) {
        for (int i = 0; i < urls.length; i++) {
            createRequirementFromPlatformURL(result, urls[i].trim());
        }
    }

    private void createRequirementFromPlatformURL(ArrayList<IRequirement> result, String url) {
        Pattern platformURL = Pattern.compile("platform:/(plugin|fragment)/([^/]*)(/)*.*");
        Matcher m = platformURL.matcher(url);
        if (m.matches())
            result.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, m.group(2),
                    VersionRange.emptyRange, null, false, false));
    }

    private void publish(Set<IInstallableUnit> units, Set<IArtifactDescriptor> artifacts, PublisherInfo publisherInfo,
            List<IPublisherAction> actions) {
        PublisherResult result = new PublisherResult();

        Publisher publisher = new Publisher(publisherInfo, result);

        IStatus status = publisher.publish(actions.toArray(new IPublisherAction[actions.size()]), monitor);

        if (!status.isOK()) {
            throw new RuntimeException(StatusTool.collectProblems(status), status.getException());
        }

        if (units != null) {
            units.addAll(result.getIUs(null, null));
        }

        if (artifacts != null) {
            IArtifactRepository artifactRepository = publisherInfo.getArtifactRepository();
            if (artifactRepository instanceof TransientArtifactRepository) {
                artifacts.addAll(((TransientArtifactRepository) artifactRepository).getArtifactDescriptors());
            }
        }
    }

}
