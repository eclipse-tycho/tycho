/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.metadata;

import java.io.File;
import java.util.Objects;

import org.eclipse.tycho.IArtifactFacade;

/**
 * Abstract proxy class for implementors who wish to modify some aspects of a facade. implementor
 * should consider the following:
 * <ul>
 * <li>override {@link #toString()} to give a meaningful description of how this is different from
 * the original and also include the proxied toString() value if suitable
 * <li>override {@link #hashCode()} and {@link #equals(Object)} in a way that is consistent with
 * constructing the same facade with equal parameters lead to equal objects
 * <li>prefer final unmodifiable class whenever possible so it is safe to use them across threads or
 * sets
 * </ul>
 */
public abstract class ArtifactFacadeProxy implements IArtifactFacade {

    protected final IArtifactFacade proxy;

    public ArtifactFacadeProxy(IArtifactFacade proxy) {
        this.proxy = proxy;
    }

    public File getLocation() {
        return proxy.getLocation();
    }

    public String getGroupId() {
        return proxy.getGroupId();
    }

    public String getArtifactId() {
        return proxy.getArtifactId();
    }

    public String getClassifier() {
        return proxy.getClassifier();
    }

    public String getVersion() {
        return proxy.getVersion();
    }

    public String getPackagingType() {
        return proxy.getPackagingType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(proxy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArtifactFacadeProxy other = (ArtifactFacadeProxy) obj;
        return Objects.equals(proxy, other.proxy);
    }

}
