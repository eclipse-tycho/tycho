/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;
import java.util.Objects;

/**
 * All values (GAV, project base directory, and target directory) by which a Tycho reactor project
 * can be uniquely identified.
 */
public abstract class ReactorProjectIdentities {

    public abstract String getGroupId();

    public abstract String getArtifactId();

    public abstract String getVersion();

    public abstract File getBasedir();

    public abstract BuildDirectory getBuildDirectory();

    // equals and hashCode could be based on any of the unique keys; GAV is promising the most stable hash values, so use that
    @Override
    public final boolean equals(Object obj) {
        return this == obj || //
                (obj instanceof ReactorProjectIdentities other && //
                        Objects.equals(this.getArtifactId(), other.getArtifactId()) && //
                        Objects.equals(this.getGroupId(), other.getGroupId()) && //
                        Objects.equals(this.getVersion(), other.getVersion()));
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getArtifactId(), getGroupId(), getVersion());
    }

}
