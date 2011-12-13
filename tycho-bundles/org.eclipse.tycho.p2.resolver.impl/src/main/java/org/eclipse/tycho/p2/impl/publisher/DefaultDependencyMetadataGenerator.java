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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

@SuppressWarnings("restriction")
public class DefaultDependencyMetadataGenerator extends P2GeneratorImpl implements DependencyMetadataGenerator {

    public DefaultDependencyMetadataGenerator() {
        super(true);
    }

    public Set<Object> generateMetadata(IArtifactFacade artifact, List<Map<String, String>> environments,
            OptionalResolutionAction optionalAction) {
        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        LinkedHashSet<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();

        PublisherInfo publisherInfo = new PublisherInfo();
        super.generateMetadata(artifact, environments, units, artifactDescriptors, publisherInfo, optionalAction);

        return new LinkedHashSet<Object>(units);
    }

}
