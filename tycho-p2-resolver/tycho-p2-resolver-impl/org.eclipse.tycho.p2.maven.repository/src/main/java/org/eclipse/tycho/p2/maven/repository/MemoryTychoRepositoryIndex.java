/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.tycho.p2.repository.DefaultTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.GAV;

public class MemoryTychoRepositoryIndex extends DefaultTychoRepositoryIndex {

    public MemoryTychoRepositoryIndex(GAV... intitialContent) {
        this(new HashSet<GAV>(Arrays.asList(intitialContent)));
    }

    public MemoryTychoRepositoryIndex(Set<GAV> intitialContent) {
        super(intitialContent);
    }

    public void save() throws IOException {
        // In memory only
    }
}
