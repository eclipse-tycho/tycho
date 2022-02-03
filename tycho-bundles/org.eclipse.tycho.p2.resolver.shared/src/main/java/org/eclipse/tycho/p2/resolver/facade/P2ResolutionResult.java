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
package org.eclipse.tycho.p2.resolver.facade;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.eclipse.tycho.ArtifactKey;

public interface P2ResolutionResult {

    public static interface Entry extends ArtifactKey {

        /**
         * 
         * @param fetch
         *            whether to force fetching the artifact from the repository if file isn't
         *            already available locally.
         * @return the artifact file location on local filesystem. If file is not already available
         *         locally and <code>fetch=false</code>, this returns <code>null</code>.
         */
        public File getLocation(boolean fetch);

        public Set<Object> getInstallableUnits();

        public String getClassifier();
    }

    public Collection<Entry> getArtifacts();

    public Set<?> getNonReactorUnits();

    /**
     * 
     * @return a list of fragments that belong to the resolved state of this result
     */
    Collection<Entry> getDependencyFragments();
}
