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
package org.eclipse.tycho.p2.impl.publisher;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

@SuppressWarnings("restriction")
public class MavenPropertiesAdvice implements IPropertyAdvice {

    private final Map<String, String> properties = new LinkedHashMap<>();

    public MavenPropertiesAdvice(String groupId, String artifactId, String version) {
        properties.put(RepositoryLayoutHelper.PROP_GROUP_ID, groupId);
        properties.put(RepositoryLayoutHelper.PROP_ARTIFACT_ID, artifactId);
        properties.put(RepositoryLayoutHelper.PROP_VERSION, version);
    }

    public MavenPropertiesAdvice(String groupId, String artifactId, String version, String classifier) {
        this(groupId, artifactId, version);
        if (classifier != null && !classifier.isEmpty()) {
            properties.put(RepositoryLayoutHelper.PROP_CLASSIFIER, classifier);
        }
    }

    public MavenPropertiesAdvice(String groupId, String artifactId, String version, String classifier,
            String extension) {
        this(groupId, artifactId, version, classifier);
        if (extension != null && !extension.isEmpty()) {
            properties.put(RepositoryLayoutHelper.PROP_EXTENSION, extension);
        }
    }

    @Override
    public Map<String, String> getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor) {
        // workaround Bug 539672
        // TODO this is a nasty hack, and it doesn't even work; see org.eclipse.equinox.p2.publisher.AbstractPublisherAction.processArtifactPropertiesAdvice(IInstallableUnit, IArtifactDescriptor, IPublisherInfo) 
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            ((ArtifactDescriptor) descriptor).setProperty(key, value);
        }
        return null;
    }

    @Override
    public Map<String, String> getInstallableUnitProperties(InstallableUnitDescription iu) {
        return properties;
    }

    @Override
    public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
        return true;
    }

}
