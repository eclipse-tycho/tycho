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
package org.eclipse.tycho.p2resolver;

import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;

@Component(role = DependencyMetadataGenerator.class, hint = DependencyMetadataGenerator.DEPENDENCY_ONLY)
public class DefaultDependencyMetadataGenerator extends P2GeneratorImpl implements DependencyMetadataGenerator {

    public DefaultDependencyMetadataGenerator() {
        super(true);
    }

    @Override
    public DependencyMetadata generateMetadata(IArtifactFacade artifact, List<TargetEnvironment> environments,
            OptionalResolutionAction optionalAction, PublisherOptions options) {
        return super.generateMetadata(artifact, environments, new PublisherInfo(), optionalAction, options);
    }

}
