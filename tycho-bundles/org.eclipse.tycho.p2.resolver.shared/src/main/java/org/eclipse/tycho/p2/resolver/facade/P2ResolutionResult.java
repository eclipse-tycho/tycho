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

public interface P2ResolutionResult {

    public static interface Entry {

        public String getType();

        public String getId();

        public String getVersion();

        public File getLocation();

        public Set<Object> getInstallableUnits();

        public String getClassifier();

        public boolean isFileAlreadyAvailableLocally();
    }

    public Collection<Entry> getArtifacts();

    public Set<?> getNonReactorUnits();
}
