/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #658 - preserve p2 artifact properties (eg PGP, maven info...)
 *******************************************************************************/
package org.eclipse.tycho.p2.publisher;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

@SuppressWarnings("restriction")
public class MavenPropertiesAdvice implements IPropertyAdvice {

    private final Map<String, String> properties = new LinkedHashMap<>();

    public MavenPropertiesAdvice(IArtifactFacade artifactFacade, MavenContext mavenContext) {
        this(artifactFacade, artifactFacade.getClassifier(), mavenContext);
    }

    public MavenPropertiesAdvice(IArtifactFacade artifactFacade, String classifier, MavenContext mavenContext) {
        this(artifactFacade.getGroupId(), artifactFacade.getArtifactId(), artifactFacade.getVersion(), classifier,
                mavenContext.getExtension(artifactFacade.getPackagingType()), artifactFacade.getPackagingType(),
                artifactFacade.getRepository());
    }

    public MavenPropertiesAdvice(String groupId, String artifactId, String version) {
        properties.put(TychoConstants.PROP_GROUP_ID, groupId);
        properties.put(TychoConstants.PROP_ARTIFACT_ID, artifactId);
        properties.put(TychoConstants.PROP_VERSION, version);
    }

    public MavenPropertiesAdvice(String groupId, String artifactId, String version, String classifier, String extension,
            String type, String repository) {
        this(groupId, artifactId, version);
        if (classifier != null && !classifier.isEmpty()) {
            properties.put(TychoConstants.PROP_CLASSIFIER, classifier);
        }
        if (extension != null && !extension.isEmpty() && !"jar".equalsIgnoreCase(extension)) {
            //This is only for the backward compat of older tycho versions
            properties.put(TychoConstants.PROP_EXTENSION, extension);
        }
        if (repository != null && !repository.isEmpty()) {
            properties.put(TychoConstants.PROP_REPOSITORY, repository);
        }
        if (type != null && !type.isEmpty()) {
            properties.put(TychoConstants.PROP_TYPE, type);
        }
    }

    @Override
    public Map<String, String> getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor) {
        // TODO this is a nasty hack, workaround for Bug 539672 
        setDescriptorProperties(descriptor);
        return null;
    }

    public void setDescriptorProperties(IArtifactDescriptor descriptor) {
        if (descriptor instanceof ArtifactDescriptor artifactDescriptor) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                artifactDescriptor.setProperty(key, value);
            }
        }
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
