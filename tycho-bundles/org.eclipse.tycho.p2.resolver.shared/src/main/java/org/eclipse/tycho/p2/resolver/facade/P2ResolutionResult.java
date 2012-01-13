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
    }

    public Collection<Entry> getArtifacts();

    public Set<?> getNonReactorUnits();
}
