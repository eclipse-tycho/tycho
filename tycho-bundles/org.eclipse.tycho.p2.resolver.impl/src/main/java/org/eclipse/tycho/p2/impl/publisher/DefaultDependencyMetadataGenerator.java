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

import java.util.List;

import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

@SuppressWarnings("restriction")
public class DefaultDependencyMetadataGenerator extends P2GeneratorImpl implements DependencyMetadataGenerator {

    public DefaultDependencyMetadataGenerator() {
        super(true);
    }

    public DependencyMetadata generateMetadata(IArtifactFacade artifact, List<TargetEnvironment> environments,
            OptionalResolutionAction optionalAction) {
        return super.generateMetadata(artifact, environments, new PublisherInfo(), optionalAction);
    }
}
