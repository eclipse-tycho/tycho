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

import java.util.List;

import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class MemoryTychoRepositoryIndex implements TychoRepositoryIndex {
    private List<GAV> projectGAVs;

    public MemoryTychoRepositoryIndex(List<GAV> projectGAVs) {
        this.projectGAVs = projectGAVs;
    }

    public List<GAV> getProjectGAVs() {
        return projectGAVs;
    }
}
