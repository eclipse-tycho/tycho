/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP AG and others.
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

/**
 * All values (GAV, project base directory, and target directory) by which a Tycho reactor project
 * can be uniquely identified.
 */
public abstract class ReactorProjectIdentities {

    public abstract String getGroupId();

    public abstract String getArtifactId();

    public abstract String getVersion();

    public abstract File getBasedir();

    public abstract BuildOutputDirectory getBuildDirectory();

    // equals and hashCode could be based on any of the unique keys; GAV is promising the most stable hash values, so use that
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ReactorProjectIdentities)) {
            return false;
        }

        ReactorProjectIdentities other = (ReactorProjectIdentities) obj;
        return eq(this.getArtifactId(), other.getArtifactId()) && eq(this.getGroupId(), other.getGroupId())
                && eq(this.getVersion(), other.getVersion());
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hash(getArtifactId());
        result = prime * result + hash(getGroupId());
        result = prime * result + hash(getVersion());
        return result;
    }

    private static boolean eq(String left, String right) {
        if (left == right)
            return true;
        else if (left == null)
            return false;
        else
            return left.equals(right);
    }

    private static int hash(String string) {
        return (string == null) ? 0 : string.hashCode();
    }

}
