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
package org.eclipse.tycho.p2.repository;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default implementation of TychoRepositoryIndex defines tycho repository index format and provides
 * generic index read/write methods.
 */
//TODO merge into only sub-class FileBasedTychoRepositoryIndex?
public abstract class DefaultTychoRepositoryIndex implements TychoRepositoryIndex {

    private Set<GAV> gavs;

    protected DefaultTychoRepositoryIndex() {
        this(Collections.<GAV> emptySet());
    }

    /**
     * @param initialContent
     *            must not contain <code>null</code>
     */
    public DefaultTychoRepositoryIndex(Set<GAV> initialContent) {
        gavs = new LinkedHashSet<GAV>(initialContent);
    }

    public Set<GAV> getProjectGAVs() {
        return Collections.unmodifiableSet(new LinkedHashSet<GAV>(gavs));
    }

    public void addGav(GAV gav) {
        if (gav == null)
            throw new NullPointerException();
        gavs.add(gav);
    }

    public void removeGav(GAV gav) {
        gavs.remove(gav);
    }

    protected void setGavs(Set<GAV> content) {
        this.gavs = content;
    }

}
