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
package org.eclipse.tycho.p2.metadata;

import java.util.List;

import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.shared.TargetEnvironment;

public interface DependencyMetadataGenerator {

    /**
     * Generates dependency-only artifact metadata
     */
    public IDependencyMetadata generateMetadata(IArtifactFacade artifact, List<TargetEnvironment> environments,
            OptionalResolutionAction optionalAction, PublisherOptions options);
}
