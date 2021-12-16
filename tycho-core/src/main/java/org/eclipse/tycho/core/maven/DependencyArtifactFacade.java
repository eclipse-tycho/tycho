/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;

import org.apache.maven.model.Dependency;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class DependencyArtifactFacade implements IArtifactFacade {

    private Dependency dependency;

    public DependencyArtifactFacade(Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public File getLocation() {
        //Dependencies are not resolved and thus can't have a location
        return null;
    }

    @Override
    public String getGroupId() {
        return dependency.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return dependency.getArtifactId();
    }

    @Override
    public String getClassifier() {
        return dependency.getClassifier();
    }

    @Override
    public String getVersion() {
        return dependency.getVersion();
    }

    @Override
    public String getPackagingType() {
        return dependency.getType();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DependencyArtifactFacade [dependency=");
        builder.append(dependency);
        builder.append("]");
        return builder.toString();
    }

}
