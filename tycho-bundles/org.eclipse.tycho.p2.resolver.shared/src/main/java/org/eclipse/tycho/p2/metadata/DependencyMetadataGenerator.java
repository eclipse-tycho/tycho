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
package org.eclipse.tycho.p2.metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DependencyMetadataGenerator {

    public static enum OptionalResolutionAction {
        /**
         * Treat optional dependencies as required.
         **/
        REQUIRE,

        /**
         * Ignore optional dependencies.
         */
        IGNORE,
    }

    /**
     * Generates dependency-only artifact metadata
     */
    public Set<Object/* IInstallableUnit */> generateMetadata(IArtifactFacade artifact,
            List<Map<String, String>> environments, OptionalResolutionAction optionalAction);
}
