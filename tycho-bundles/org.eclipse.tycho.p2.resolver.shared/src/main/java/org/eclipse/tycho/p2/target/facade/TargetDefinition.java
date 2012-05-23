/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.net.URI;
import java.util.List;

// TODO javadoc
public interface TargetDefinition {

    public List<? extends Location> getLocations();

    /**
     * Returns <code>true</code> if the target definition specifies an explicit list of bundles to
     * include (i.e. an <tt>&lt;includeBundles&gt;</tt> in target definition files).
     */
    boolean hasIncludedBundles();

    @Override
    public boolean equals(Object obj);

    @Override
    public int hashCode();

    public interface Location {

        /**
         * Returns a description of the underlying location implementation.
         */
        String getTypeDescription();

    }

    public interface InstallableUnitLocation extends Location {
        public List<? extends Repository> getRepositories();

        public List<? extends Unit> getUnits();

        public IncludeMode getIncludeMode();

        public boolean includeAllEnvironments();
    }

    public enum IncludeMode {
        SLICER, PLANNER
    }

    public interface Repository {
        URI getLocation();

        String getId();
    }

    public interface Unit {

        public String getId();

        public String getVersion();
    }
}
