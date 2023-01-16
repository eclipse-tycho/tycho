/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Maps an {@link IInstallableUnit} into
 * <a href="https://docs.osgi.org/specification/osgi.core/8.0.0/framework.resource.html">Resource
 * API Specification</a> {@link Resource}
 */
public class InstallableUnitResource implements Resource {

    IInstallableUnit installableUnit;
    private final Map<String, List<Requirement>> requirementsMap;
    private final Map<String, List<Capability>> capabilitiesMap;

    public InstallableUnitResource(IInstallableUnit installableUnit, IArtifactRepository artifactRepository) {
        this.installableUnit = installableUnit;
        Collection<IRequirement> requirements = installableUnit.getRequirements();
        requirementsMap = new HashMap<>(requirements.size());
        for (IRequirement requirement : requirements) {
            if (RequiredCapability.isVersionRangeRequirement(requirement.getMatches())) {
                InstallableUnitRequirement req = new InstallableUnitRequirement(
                        this, requirement);
                requirementsMap.computeIfAbsent(req.getNamespace(), nil -> new ArrayList<>()).add(req);
            }
        }
        //TODO <directive name='effective' value='meta'/> for meta requirements?!
        List<IArtifactDescriptor> artifacts = installableUnit.getArtifacts().stream().flatMap(key -> {
            IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
            return Arrays.stream(descriptors);
        }).toList();
        Collection<IProvidedCapability> capabilities = installableUnit.getProvidedCapabilities();
        capabilitiesMap = new HashMap<>(capabilities.size() + artifacts.size() + 1);
        for (IProvidedCapability capability : capabilities) {
            InstallableUnitCapability cap = new InstallableUnitCapability(InstallableUnitResource.this,
                    capability);
            capabilitiesMap.computeIfAbsent(cap.getNamespace(), nil -> new ArrayList<>()).add(cap);
        }
        for (IArtifactDescriptor descriptor : artifacts) {
            ArtifactDescriptorRepositoryContentCapability cap = new ArtifactDescriptorRepositoryContentCapability(this, descriptor, artifactRepository);
            capabilitiesMap.computeIfAbsent(cap.getNamespace(), nil -> new ArrayList<>()).add(cap);
        }
    }

    @Override
    public List<Capability> getCapabilities(String namespace) {
        if (namespace != null) {
            return Collections.unmodifiableList(capabilitiesMap.getOrDefault(namespace, List.of()));
        }
        return capabilitiesMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public List<Requirement> getRequirements(String namespace) {
        if (namespace != null) {
            return Collections.unmodifiableList(requirementsMap.getOrDefault(namespace, List.of()));
        }
        return requirementsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return installableUnit.toString();
    }

}
