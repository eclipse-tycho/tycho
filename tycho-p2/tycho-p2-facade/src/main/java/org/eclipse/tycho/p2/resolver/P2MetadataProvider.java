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
package org.eclipse.tycho.p2.resolver;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.shared.TargetEnvironment;

/**
 * Component interface that allows contribution of additional p2 metadata to reactor projects.
 * Implementations of this interface will be invoked as part of reactor project setup logic and
 * contributed metadata will be used to establish project dependencies and reactor build order.
 */
public interface P2MetadataProvider {

    /**
     * @return Map<String,IDependencyMetadata> classifier to metadata map or <code>null</code>
     */
    //TODO consider allowing MavenExecutionException
    public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction);
}
