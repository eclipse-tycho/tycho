/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.repository;

import java.io.File;
import java.util.Objects;

import org.eclipse.tycho.core.shared.MavenContext;

public class LocalRepositoryReader implements RepositoryReader {

    private MavenContext mavenContext;

    public LocalRepositoryReader(MavenContext mavenContext) {
        Objects.requireNonNull(mavenContext);
        this.mavenContext = mavenContext;
    }

    @Override
    public File getLocalArtifactLocation(GAV gav, String classifier, String type) {
        return new File(mavenContext.getLocalRepositoryRoot(),
                RepositoryLayoutHelper.getRelativePath(gav, classifier, type, mavenContext));
    }

    @Override
    public MavenContext getMavenContext() {
        return mavenContext;
    }

}
